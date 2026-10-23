package com.swp391_be.service;

import com.swp391_be.config.PayOSConfig;
import com.swp391_be.dto.PayOSCreateRequest;
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
import com.swp391_be.exception.ResourceNotFoundException;
import com.swp391_be.repository.InvoiceRepository;
import com.swp391_be.repository.MembershipPackageRepository;
import com.swp391_be.repository.PaymentRepository;
import com.swp391_be.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.payos.PayOS;
import vn.payos.type.CheckoutResponseData;
import vn.payos.type.ItemData;
import vn.payos.type.PaymentData;
import vn.payos.type.PaymentLinkData;
import vn.payos.type.Webhook;
import vn.payos.type.WebhookData;

/**
 * PayOS Integration — thay thế VNPay.
 *
 * Luồng:
 *   Member chọn gói → POST /api/payos/create-payment-link
 *     → tạo Invoice + Payment(PENDING)
 *     → gọi PayOS SDK tạo link thanh toán
 *   PayOS gọi webhook: POST /api/payos/webhook
 *     → verify signature
 *     → set Payment=COMPLETED, Invoice=PAID
 *     → kích hoạt MemberMembership
 *   PayOS redirect browser về: GET /api/payos/callback
 *     → trả kết quả cho FE
 *
 * Nếu muốn dùng mock (không có tài khoản PayOS thật),
 * dùng endpoint POST /api/payos/mock-success/{paymentCode}.
 */
@Service
public class PayOSService {

    private static final Logger log = LoggerFactory.getLogger(PayOSService.class);
    private static final DateTimeFormatter CODE_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final PayOS payOS;
    private final PayOSConfig payOSConfig;
    private final PaymentRepository payments;
    private final InvoiceRepository invoices;
    private final MembershipPackageRepository packages;
    private final UserRepository users;
    private final MemberMembershipService memberMemberships;
    private final Clock clock;

    public PayOSService(
            PayOS payOS,
            PayOSConfig payOSConfig,
            PaymentRepository payments,
            InvoiceRepository invoices,
            MembershipPackageRepository packages,
            UserRepository users,
            MemberMembershipService memberMemberships,
            Clock clock) {
        this.payOS = payOS;
        this.payOSConfig = payOSConfig;
        this.payments = payments;
        this.invoices = invoices;
        this.packages = packages;
        this.users = users;
        this.memberMemberships = memberMemberships;
        this.clock = clock;
    }

    // ── Bước 1: Tạo link thanh toán PayOS ─────────────────────────────────

    /**
     * Tạo Invoice + Payment(PENDING) rồi gọi PayOS SDK tạo link thanh toán.
     *
     * @param memberId id của member (từ JWT)
     * @param request  thông tin gói muốn mua
     * @return URL redirect đến trang thanh toán PayOS
     */
    @Transactional
    public String createPaymentLink(Long memberId, PayOSCreateRequest request) {
        User member = validateAndGetMember(memberId);
        MembershipPackage pkg = validateAndGetActivePackage(request.getPackageId());

        Instant now = Instant.now(clock);
        String invoiceCode = generateCode("INV");
        String paymentCode = generateCode("PAY");

        // Tạo description
        String description = request.getDescription() != null && !request.getDescription().isBlank()
                ? request.getDescription()
                : "Thanh toan goi: " + pkg.getPackageCode();

        // Tạo Invoice
        Invoice invoice = new Invoice(
                invoiceCode,
                member.getId(),
                pkg.getId(),
                pkg.getPrice(),
                now,
                description);
        invoice = invoices.save(invoice);

        // Tạo Payment (PENDING, method = PAYOS)
        Payment payment = new Payment(
                paymentCode,
                invoice.getId(),
                member.getId(),
                invoice.getAmount(),
                PaymentMethod.PAYOS,
                now,
                description);
        payments.save(payment);

        // Gọi PayOS SDK tạo link thanh toán
        try {
            // PayOS yêu cầu orderCode là số long, dùng timestamp + random
            long orderCode = generateOrderCode();

            // Lưu orderCode vào payment note để tra cứu sau
            payment.setNote("orderCode=" + orderCode);
            payments.save(payment);

            ItemData item = ItemData.builder()
                    .name(pkg.getName())
                    .quantity(1)
                    .price(pkg.getPrice().intValue())
                    .build();

            PaymentData paymentData = PaymentData.builder()
                    .orderCode(orderCode)
                    .amount(pkg.getPrice().intValue())
                    .description(truncate(description, 25))
                    .returnUrl(payOSConfig.getReturnUrl())
                    .cancelUrl(payOSConfig.getCancelUrl())
                    .item(item)
                    .build();

            CheckoutResponseData result = payOS.createPaymentLink(paymentData);
            return result.getCheckoutUrl();
        } catch (Exception e) {
            log.error("[PayOS] Failed to create payment link", e);
            throw new BusinessException("Failed to create PayOS payment link: " + e.getMessage());
        }
    }

    // ── Bước 2: Webhook — PayOS gọi server khi thanh toán xong ────────────

    /**
     * Xử lý webhook từ PayOS (server-to-server).
     * PayOS POST trực tiếp vào server, không qua browser.
     *
     * @param webhookBody raw JSON body từ PayOS
     */
    @Transactional
    public void handleWebhook(String webhookBody) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            Webhook webhook = objectMapper.readValue(webhookBody, Webhook.class);
            WebhookData webhookData = payOS.verifyPaymentWebhookData(webhook);

            long orderCode = webhookData.getOrderCode();
            String transactionReference = webhookData.getReference();

            // Tìm payment theo orderCode (lưu trong note)
            Payment payment = findPaymentByOrderCode(orderCode);
            if (payment == null) {
                log.warn("[PayOS Webhook] Payment not found for orderCode: {}", orderCode);
                return;
            }

            if (payment.getStatus() == PaymentStatus.COMPLETED) {
                log.info("[PayOS Webhook] Payment already completed: orderCode={}", orderCode);
                return; // Idempotent
            }

            Instant now = Instant.now(clock);

            if ("PAID".equals(webhookData.getCode())) {
                // Thanh toán thành công
                payment.setStatus(PaymentStatus.COMPLETED);
                payment.setCompletedAt(now);
                payment.setTransactionReference(transactionReference);
                payments.save(payment);

                Invoice invoice = invoices.findById(payment.getInvoiceId())
                        .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));
                invoice.setStatus(InvoiceStatus.PAID);
                invoice.setPaidAt(now);
                invoices.save(invoice);

                MembershipPackage pkg = packages.findById(invoice.getPackageId())
                        .orElseThrow(() -> new ResourceNotFoundException("Package not found"));
                memberMemberships.activateMembership(payment.getMemberId(), pkg.getId(), pkg.getDurationDays());

                log.info("[PayOS Webhook] Payment COMPLETED: orderCode={}, ref={}", orderCode, transactionReference);
            } else {
                // Thanh toán thất bại / hủy
                payment.setStatus(PaymentStatus.CANCELLED);
                payment.setNote(payment.getNote() + " | PayOS code: " + webhookData.getCode());
                payments.save(payment);

                Invoice invoice = invoices.findById(payment.getInvoiceId()).orElse(null);
                if (invoice != null) {
                    invoice.setStatus(InvoiceStatus.CANCELLED);
                    invoices.save(invoice);
                }
                log.info("[PayOS Webhook] Payment FAILED: orderCode={}, code={}", orderCode, webhookData.getCode());
            }
        } catch (Exception e) {
            log.error("[PayOS Webhook] Error processing webhook", e);
            throw new BusinessException("Failed to process PayOS webhook: " + e.getMessage());
        }
    }

    // ── Bước 3: Callback — Browser redirect về sau khi thanh toán ──────────

    /**
     * Lấy thông tin thanh toán từ PayOS theo orderCode.
     * Dùng khi browser redirect về return URL.
     */
    public PaymentLinkData getPaymentInfo(long orderCode) {
        try {
            return payOS.getPaymentLinkInformation(orderCode);
        } catch (Exception e) {
            log.error("[PayOS] Failed to get payment info for orderCode: {}", orderCode, e);
            throw new BusinessException("Failed to get payment info from PayOS");
        }
    }

    /**
     * Lấy trạng thái thanh toán theo paymentCode.
     */
    public Payment getPaymentByCode(String paymentCode) {
        return payments.findByPaymentCode(paymentCode)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found: " + paymentCode));
    }

    // ── Mock (không cần PayOS thật) ───────────────────────────────────────

    /**
     * Mock thành công thanh toán PayOS — dùng để demo / test không có tài khoản PayOS.
     */
    @Transactional
    public PaymentResponse mockSuccess(String paymentCode) {
        Payment payment = payments.findByPaymentCode(paymentCode)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found: " + paymentCode));

        if (payment.getStatus() == PaymentStatus.COMPLETED) {
            throw new BusinessException("Payment already completed");
        }
        if (payment.getPaymentMethod() != PaymentMethod.PAYOS) {
            throw new BusinessException("Only PAYOS payments can be mock-completed");
        }

        Instant now = Instant.now(clock);
        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setCompletedAt(now);
        payment.setTransactionReference("MOCK-PAYOS-" + paymentCode);
        payments.save(payment);

        Invoice invoice = invoices.findById(payment.getInvoiceId())
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));
        invoice.setStatus(InvoiceStatus.PAID);
        invoice.setPaidAt(now);
        invoices.save(invoice);

        MembershipPackage pkg = packages.findById(invoice.getPackageId())
                .orElseThrow(() -> new ResourceNotFoundException("Package not found"));
        memberMemberships.activateMembership(payment.getMemberId(), pkg.getId(), pkg.getDurationDays());

        User member = users.findById(payment.getMemberId()).orElse(null);
        return toPaymentResponse(payment, invoice, member, pkg);
    }

    // ── Internal helpers ──────────────────────────────────────────────────

    private Payment findPaymentByOrderCode(long orderCode) {
        String orderCodeStr = "orderCode=" + orderCode;
        List<Payment> allPending = payments.findAll();
        return allPending.stream()
                .filter(p -> p.getNote() != null && p.getNote().contains(orderCodeStr))
                .findFirst()
                .orElse(null);
    }

    private long generateOrderCode() {
        // PayOS yêu cầu orderCode là số long duy nhất
        // Dùng timestamp (ms) + random 3 chữ số cuối
        return System.currentTimeMillis() * 1000 + ThreadLocalRandom.current().nextInt(0, 999);
    }

    private String truncate(String s, int maxLen) {
        if (s == null) return "";
        return s.length() <= maxLen ? s : s.substring(0, maxLen);
    }

    private String generateCode(String prefix) {
        String timestamp = LocalDateTime.now(clock).format(CODE_FMT);
        int random = ThreadLocalRandom.current().nextInt(1000, 9999);
        return prefix + "-" + timestamp + "-" + random;
    }

    private User validateAndGetMember(Long memberId) {
        User user = users.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found"));
        if (user.getRole() != Role.MEMBER) {
            throw new BusinessException("User is not a member");
        }
        return user;
    }

    private MembershipPackage validateAndGetActivePackage(Long packageId) {
        MembershipPackage pkg = packages.findById(packageId)
                .orElseThrow(() -> new ResourceNotFoundException("Membership package not found"));
        if (pkg.getStatus() == com.swp391_be.entity.PackageStatus.SUSPENDED) {
            throw new BusinessException("Membership package is currently inactive: " + pkg.getPackageCode());
        }
        return pkg;
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
}
