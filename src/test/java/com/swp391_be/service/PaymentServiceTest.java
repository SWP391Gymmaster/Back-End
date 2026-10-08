package com.swp391_be.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.swp391_be.dto.CashCheckoutRequest;
import com.swp391_be.dto.ConfirmPaymentRequest;
import com.swp391_be.dto.CreatePaymentRequest;
import com.swp391_be.dto.PaymentResponse;
import com.swp391_be.entity.Invoice;
import com.swp391_be.entity.InvoiceStatus;
import com.swp391_be.entity.MembershipPackage;
import com.swp391_be.entity.Payment;
import com.swp391_be.entity.PaymentMethod;
import com.swp391_be.entity.PaymentStatus;
import com.swp391_be.entity.Role;
import com.swp391_be.entity.User;
import com.swp391_be.exception.BusinessException;
import com.swp391_be.repository.InvoiceRepository;
import com.swp391_be.repository.MembershipPackageRepository;
import com.swp391_be.repository.PaymentRepository;
import com.swp391_be.repository.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final Instant NOW = Instant.parse("2026-10-08T10:00:00Z");

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private MembershipPackageRepository packageRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MemberMembershipService memberMembershipService;

    private final Clock clock = Clock.fixed(NOW, ZONE);

    private PaymentService paymentService;

    private User member;
    private MembershipPackage gymPackage;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(
                paymentRepository,
                invoiceRepository,
                packageRepository,
                userRepository,
                memberMembershipService,
                clock);

        member = new User("member1", "Nguyen Van A", "member@example.com", "hash", "0901234567", Role.MEMBER);
        setId(member, 1L);
        gymPackage = new MembershipPackage("Gói 1 tháng", "Tập không giới hạn", 500000.0, 30);
        setId(gymPackage, 10L);
    }

    @Test
    void createPaymentSuccessfully() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(member));
        when(packageRepository.findById(10L)).thenReturn(Optional.of(gymPackage));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> {
            Invoice inv = invocation.getArgument(0);
            return inv;
        });
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment p = invocation.getArgument(0);
            return p;
        });

        CreatePaymentRequest request = new CreatePaymentRequest(10L, null, PaymentMethod.VNPAY, "Mua gói online");
        PaymentResponse response = paymentService.createPayment(1L, request);

        assertThat(response).isNotNull();
        assertThat(response.getAmount()).isEqualTo(500000.0);
        assertThat(response.getPaymentMethod()).isEqualTo(PaymentMethod.VNPAY);
        assertThat(response.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(response.getPaymentCode()).startsWith("PAY-");
        assertThat(response.getInvoiceCode()).startsWith("INV-");
    }

    @Test
    void createPaymentFailsWhenPackageInactive() {
        gymPackage.setActive(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(member));
        when(packageRepository.findById(10L)).thenReturn(Optional.of(gymPackage));

        CreatePaymentRequest request = new CreatePaymentRequest(10L, null, PaymentMethod.VNPAY, null);

        assertThatThrownBy(() -> paymentService.createPayment(1L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Membership package is currently inactive");
    }

    @Test
    void createPaymentFailsWhenUserNotMember() {
        User coach = new User("coach1", "Coach B", "coach@example.com", "hash", null, Role.COACH);
        when(userRepository.findById(2L)).thenReturn(Optional.of(coach));

        CreatePaymentRequest request = new CreatePaymentRequest(10L, null, PaymentMethod.VNPAY, null);

        assertThatThrownBy(() -> paymentService.createPayment(2L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("User is not a member");
    }

    @Test
    void confirmPaymentSuccessfullyAndActivatesMembership() {
        Invoice invoice = new Invoice("INV-001", 1L, 10L, 500000.0, NOW, "Ghi chú");
        Payment payment = new Payment("PAY-001", 100L, 1L, 500000.0, PaymentMethod.BANK_TRANSFER, NOW, "Ghi chú");

        when(paymentRepository.findById(50L)).thenReturn(Optional.of(payment));
        when(invoiceRepository.findById(payment.getInvoiceId())).thenReturn(Optional.of(invoice));
        when(userRepository.findById(1L)).thenReturn(Optional.of(member));
        when(packageRepository.findById(10L)).thenReturn(Optional.of(gymPackage));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ConfirmPaymentRequest confirmRequest = new ConfirmPaymentRequest("TXN123456", "Xác nhận chuyển khoản");
        PaymentResponse response = paymentService.confirmPayment(50L, confirmRequest);

        assertThat(response.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.PAID);
        assertThat(payment.getTransactionReference()).isEqualTo("TXN123456");

        // Verify that member membership was activated with package durationDays (30)
        verify(memberMembershipService).activateMembership(eq(1L), eq(gymPackage.getId()), eq(30));
    }

    @Test
    void confirmPaymentRejectsAlreadyCompletedPayment() {
        Payment payment = new Payment("PAY-001", 100L, 1L, 500000.0, PaymentMethod.CASH, NOW, null);
        payment.setStatus(PaymentStatus.COMPLETED);

        when(paymentRepository.findById(50L)).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentService.confirmPayment(50L, new ConfirmPaymentRequest()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Payment is already completed");
    }

    @Test
    void directCashCheckoutSuccessfullyActivatesMembership() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(member));
        when(packageRepository.findById(10L)).thenReturn(Optional.of(gymPackage));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CashCheckoutRequest request = new CashCheckoutRequest(1L, 10L, "Thu tiền tại quầy");
        PaymentResponse response = paymentService.directCashCheckout(request);

        assertThat(response.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(response.getPaymentMethod()).isEqualTo(PaymentMethod.CASH);
        assertThat(response.getAmount()).isEqualTo(500000.0);

        verify(memberMembershipService).activateMembership(eq(1L), eq(gymPackage.getId()), eq(30));
    }

    @Test
    void cancelPaymentSuccessfully() {
        Invoice invoice = new Invoice("INV-001", 1L, 10L, 500000.0, NOW, null);
        Payment payment = new Payment("PAY-001", 100L, 1L, 500000.0, PaymentMethod.VNPAY, NOW, null);

        when(paymentRepository.findById(50L)).thenReturn(Optional.of(payment));
        when(invoiceRepository.findById(payment.getInvoiceId())).thenReturn(Optional.of(invoice));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentResponse response = paymentService.cancelPayment(50L, "Khách đổi ý");

        assertThat(response.getStatus()).isEqualTo(PaymentStatus.CANCELLED);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CANCELLED);
        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.CANCELLED);
        assertThat(payment.getNote()).contains("Lý do hủy: Khách đổi ý");
    }

    @Test
    void cancelPaymentFailsIfAlreadyCompleted() {
        Payment payment = new Payment("PAY-001", 100L, 1L, 500000.0, PaymentMethod.VNPAY, NOW, null);
        payment.setStatus(PaymentStatus.COMPLETED);

        when(paymentRepository.findById(50L)).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentService.cancelPayment(50L, "Khách đòi hủy"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Cannot cancel an already completed payment");
    }

    private void setId(Object entity, Long id) {
        try {
            java.lang.reflect.Field field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (ReflectiveOperationException ex) {
            throw new AssertionError(ex);
        }
    }
}
