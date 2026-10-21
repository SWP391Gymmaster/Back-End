package com.swp391_be.service;

import com.swp391_be.dto.CashCheckoutRequest;
import com.swp391_be.dto.ConfirmPaymentRequest;
import com.swp391_be.dto.CreatePaymentRequest;
import com.swp391_be.dto.InvoiceResponse;
import com.swp391_be.dto.MemberMembershipResponse;
import com.swp391_be.dto.PaymentResponse;
import com.swp391_be.entity.Invoice;
import com.swp391_be.entity.InvoiceStatus;
import com.swp391_be.entity.MemberMembership;
import com.swp391_be.entity.MembershipPackage;
import com.swp391_be.entity.PackageStatus;
import com.swp391_be.entity.Payment;
import com.swp391_be.entity.PaymentMethod;
import com.swp391_be.entity.PaymentStatus;
import com.swp391_be.entity.Role;
import com.swp391_be.entity.User;
import com.swp391_be.exception.BusinessException;
import com.swp391_be.exception.ResourceNotFoundException;
import com.swp391_be.repository.InvoiceRepository;
import com.swp391_be.repository.MembershipPackageRepository;
import com.swp391_be.repository.PaymentRepository;
import com.swp391_be.repository.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {

    private static final DateTimeFormatter CODE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final PaymentRepository payments;
    private final InvoiceRepository invoices;
    private final MembershipPackageRepository packages;
    private final UserRepository users;
    private final MemberMembershipService memberMemberships;
    private final Clock clock;

    public PaymentService(
            PaymentRepository payments,
            InvoiceRepository invoices,
            MembershipPackageRepository packages,
            UserRepository users,
            MemberMembershipService memberMemberships,
            Clock clock) {
        this.payments = payments;
        this.invoices = invoices;
        this.packages = packages;
        this.users = users;
        this.memberMemberships = memberMemberships;
        this.clock = clock;
    }

    @Transactional
    public PaymentResponse createPayment(Long memberId, CreatePaymentRequest request) {
        User member = validateAndGetMember(memberId);
        MembershipPackage pkg = validateAndGetActivePackage(request.getPackageId());

        Instant now = Instant.now(clock);
        String invoiceCode = generateCode("INV");
        String paymentCode = generateCode("PAY");

        Invoice invoice = new Invoice(
                invoiceCode,
                member.getId(),
                pkg.getId(),
                pkg.getPrice(),
                now,
                request.getNote() != null ? request.getNote() : "Thanh toán gói: " + pkg.getName());
        invoice = invoices.save(invoice);

        Payment payment = new Payment(
                paymentCode,
                invoice.getId(),
                member.getId(),
                invoice.getAmount(),
                request.getPaymentMethod(),
                now,
                request.getNote());
        payment = payments.save(payment);

        return toPaymentResponse(payment, invoice, member, pkg);
    }

    @Transactional
    public PaymentResponse confirmPayment(Long paymentId, ConfirmPaymentRequest request) {
        Payment payment = payments.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));

        if (payment.getStatus() == PaymentStatus.COMPLETED) {
            throw new BusinessException("Payment is already completed");
        }
        if (payment.getStatus() == PaymentStatus.CANCELLED) {
            throw new BusinessException("Cannot complete a cancelled payment");
        }

        Invoice invoice = invoices.findById(payment.getInvoiceId())
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));

        Long memberId = payment.getMemberId();
        User member = users.findByIdForUpdate(memberId).or(() -> users.findById(memberId))
                .orElseThrow(() -> new ResourceNotFoundException("Member not found"));

        MembershipPackage pkg = packages.findById(invoice.getPackageId())
                .orElseThrow(() -> new ResourceNotFoundException("Membership package not found"));

        Instant now = Instant.now(clock);

        // Cập nhật trạng thái Payment
        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setCompletedAt(now);
        if (request != null && request.getTransactionReference() != null && !request.getTransactionReference().isBlank()) {
            payment.setTransactionReference(request.getTransactionReference());
        }
        if (request != null && request.getNote() != null && !request.getNote().isBlank()) {
            payment.setNote(request.getNote());
        }
        payment = payments.save(payment);

        // Cập nhật trạng thái Invoice
        invoice.setStatus(InvoiceStatus.PAID);
        invoice.setPaidAt(now);
        invoices.save(invoice);

        // Tự động kích hoạt hoặc gia hạn gói tập cho Member
        memberMemberships.activateMembership(member.getId(), pkg.getId(), pkg.getDurationDays());

        return toPaymentResponse(payment, invoice, member, pkg);
    }

    @Transactional
    public PaymentResponse directCashCheckout(CashCheckoutRequest request) {
        User member = validateAndGetMember(request.getMemberId());
        MembershipPackage pkg = validateAndGetActivePackage(request.getPackageId());

        Instant now = Instant.now(clock);
        String invoiceCode = generateCode("INV");
        String paymentCode = generateCode("PAY");

        Invoice invoice = new Invoice(
                invoiceCode,
                member.getId(),
                pkg.getId(),
                pkg.getPrice(),
                now,
                request.getNote() != null ? request.getNote() : "Thanh toán trực tiếp tại quầy: " + pkg.getName());
        invoice.setStatus(InvoiceStatus.PAID);
        invoice.setPaidAt(now);
        invoice = invoices.save(invoice);

        Payment payment = new Payment(
                paymentCode,
                invoice.getId(),
                member.getId(),
                invoice.getAmount(),
                PaymentMethod.CASH,
                now,
                request.getNote());
        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setCompletedAt(now);
        payment.setTransactionReference("CASH-" + invoiceCode);
        payment = payments.save(payment);

        // Kích hoạt gói tập
        memberMemberships.activateMembership(member.getId(), pkg.getId(), pkg.getDurationDays());

        return toPaymentResponse(payment, invoice, member, pkg);
    }

    @Transactional
    public PaymentResponse cancelPayment(Long paymentId, String reason) {
        Payment payment = payments.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));

        if (payment.getStatus() == PaymentStatus.COMPLETED) {
            throw new BusinessException("Cannot cancel an already completed payment");
        }

        Invoice invoice = invoices.findById(payment.getInvoiceId())
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));

        payment.setStatus(PaymentStatus.CANCELLED);
        if (reason != null && !reason.isBlank()) {
            payment.setNote(payment.getNote() != null ? payment.getNote() + " [Lý do hủy: " + reason + "]" : "Lý do hủy: " + reason);
        }
        payments.save(payment);

        invoice.setStatus(InvoiceStatus.CANCELLED);
        invoices.save(invoice);

        User member = users.findById(payment.getMemberId()).orElse(null);
        MembershipPackage pkg = packages.findById(invoice.getPackageId()).orElse(null);

        return toPaymentResponse(payment, invoice, member, pkg);
    }

    public PaymentResponse getPaymentById(Long id) {
        Payment payment = payments.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
        Invoice invoice = invoices.findById(payment.getInvoiceId()).orElse(null);
        User member = users.findById(payment.getMemberId()).orElse(null);
        MembershipPackage pkg = invoice != null ? packages.findById(invoice.getPackageId()).orElse(null) : null;
        return toPaymentResponse(payment, invoice, member, pkg);
    }

    public InvoiceResponse getInvoiceById(Long id) {
        Invoice invoice = invoices.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));
        User member = users.findById(invoice.getMemberId()).orElse(null);
        MembershipPackage pkg = packages.findById(invoice.getPackageId()).orElse(null);
        return toInvoiceResponse(invoice, member, pkg);
    }

    public List<PaymentResponse> getMyPayments(Long memberId) {
        return payments.findByMemberIdOrderByCreatedAtDesc(memberId).stream()
                .map(this::enrichPaymentResponse)
                .toList();
    }

    public List<InvoiceResponse> getMyInvoices(Long memberId) {
        return invoices.findByMemberIdOrderByIssuedAtDesc(memberId).stream()
                .map(this::enrichInvoiceResponse)
                .toList();
    }

    public List<PaymentResponse> getAllPayments() {
        return payments.findAllByOrderByCreatedAtDesc().stream()
                .map(this::enrichPaymentResponse)
                .toList();
    }

    public List<InvoiceResponse> getAllInvoices() {
        return invoices.findAllByOrderByIssuedAtDesc().stream()
                .map(this::enrichInvoiceResponse)
                .toList();
    }

    public List<MemberMembershipResponse> getMemberMembershipDetails(Long memberId) {
        LocalDate today = LocalDate.now(clock);
        List<MemberMembership> list = memberMemberships.getMembershipsByMemberId(memberId);
        return list.stream().map(m -> {
            MembershipPackage pkg = packages.findById(m.getPackageId()).orElse(null);
            String pkgName = pkg != null ? pkg.getName() : "Gói tập #" + m.getPackageId();
            boolean active = !today.isBefore(m.getStartDate()) && !today.isAfter(m.getEndDate());
            long remaining = today.isAfter(m.getEndDate()) ? 0 : ChronoUnit.DAYS.between(today, m.getEndDate()) + 1;
            return new MemberMembershipResponse(
                    m.getId(),
                    m.getMemberId(),
                    m.getPackageId(),
                    pkgName,
                    m.getStartDate(),
                    m.getEndDate(),
                    active,
                    remaining);
        }).toList();
    }

    private PaymentResponse enrichPaymentResponse(Payment payment) {
        Invoice invoice = invoices.findById(payment.getInvoiceId()).orElse(null);
        User member = users.findById(payment.getMemberId()).orElse(null);
        MembershipPackage pkg = invoice != null ? packages.findById(invoice.getPackageId()).orElse(null) : null;
        return toPaymentResponse(payment, invoice, member, pkg);
    }

    private InvoiceResponse enrichInvoiceResponse(Invoice invoice) {
        User member = users.findById(invoice.getMemberId()).orElse(null);
        MembershipPackage pkg = packages.findById(invoice.getPackageId()).orElse(null);
        return toInvoiceResponse(invoice, member, pkg);
    }

    private User validateAndGetMember(Long memberId) {
        User user = users.findByIdForUpdate(memberId).or(() -> users.findById(memberId))
                .orElseThrow(() -> new ResourceNotFoundException("Member not found"));
        if (user.getRole() != Role.MEMBER) {
            throw new BusinessException("User is not a member");
        }
        return user;
    }

    private MembershipPackage validateAndGetActivePackage(Long packageId) {
        MembershipPackage pkg = packages.findById(packageId)
                .orElseThrow(() -> new ResourceNotFoundException("Membership package not found"));
        if (pkg.getStatus() != PackageStatus.ACTIVE) {
            throw new BusinessException("Membership package is currently inactive");
        }
        return pkg;
    }

    private String generateCode(String prefix) {
        String timestamp = LocalDateTime.now(clock).format(CODE_DATE_FORMAT);
        int random = ThreadLocalRandom.current().nextInt(1000, 9999);
        return prefix + "-" + timestamp + "-" + random;
    }

    private PaymentResponse toPaymentResponse(Payment payment, Invoice invoice, User member, MembershipPackage pkg) {
        return new PaymentResponse(
                payment.getId(),
                payment.getPaymentCode(),
                invoice != null ? invoice.getId() : payment.getInvoiceId(),
                invoice != null ? invoice.getInvoiceCode() : null,
                payment.getMemberId(),
                member != null ? member.getFullName() : null,
                pkg != null ? pkg.getId() : (invoice != null ? invoice.getPackageId() : null),
                pkg != null ? pkg.getName() : null,
                payment.getAmount(),
                payment.getPaymentMethod(),
                payment.getStatus(),
                payment.getTransactionReference(),
                payment.getCreatedAt(),
                payment.getCompletedAt(),
                payment.getNote());
    }

    private InvoiceResponse toInvoiceResponse(Invoice invoice, User member, MembershipPackage pkg) {
        return new InvoiceResponse(
                invoice.getId(),
                invoice.getInvoiceCode(),
                invoice.getMemberId(),
                member != null ? member.getFullName() : null,
                invoice.getPackageId(),
                pkg != null ? pkg.getName() : null,
                invoice.getAmount(),
                invoice.getStatus(),
                invoice.getIssuedAt(),
                invoice.getPaidAt(),
                invoice.getNote());
    }
}
