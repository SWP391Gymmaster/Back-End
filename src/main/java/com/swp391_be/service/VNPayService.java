package com.swp391_be.service;

import com.swp391_be.config.VNPayConfig;
import com.swp391_be.dto.PaymentResponse;
import com.swp391_be.dto.VNPayCreateUrlRequest;
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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ThreadLocalRandom;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * VNPay Sandbox Integration (T5.5).
 *
 * Luồng:
 *   Member chọn gói → POST /api/vnpay/create-payment-url
 *     → tạo Invoice + Payment(PENDING)
 *     → sinh URL redirect VNPay sandbox
 *   VNPay gọi IPN (server→server): POST /api/vnpay/ipn
 *     → verify hash
 *     → set Payment=COMPLETED, Invoice=PAID
 *     → kích hoạt MemberMembership
 *   VNPay redirect browser về: GET /api/vnpay/callback
 *     → verify hash, trả kết quả cho FE
 *
 * Nếu muốn dùng mock (không có tài khoản sandbox thật),
 * dùng endpoint POST /api/vnpay/mock-success/{paymentCode}.
 */
@Service
public class VNPayService {

    private static final Logger log = LoggerFactory.getLogger(VNPayService.class);
    private static final DateTimeFormatter CODE_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final VNPayConfig vnPayConfig;
    private final PaymentRepository payments;
    private final InvoiceRepository invoices;
    private final MembershipPackageRepository packages;
    private final UserRepository users;
    private final MemberMembershipService memberMemberships;
    private final Clock clock;

    public VNPayService(
            VNPayConfig vnPayConfig,
            PaymentRepository payments,
            InvoiceRepository invoices,
            MembershipPackageRepository packages,
            UserRepository users,
            MemberMembershipService memberMemberships,
            Clock clock) {
        this.vnPayConfig = vnPayConfig;
        this.payments = payments;
        this.invoices = invoices;
        this.packages = packages;
        this.users = users;
        this.memberMemberships = memberMemberships;
        this.clock = clock;
    }

    // ── Bước 1: Tạo URL redirect VNPay ────────────────────────────────────

    /**
     * Tạo Invoice + Payment(PENDING) rồi sinh URL thanh toán VNPay sandbox.
     *
     * @param memberId id của member (từ JWT)
     * @param request  thông tin gói muốn mua
     * @param clientIp IP của client (truyền vào header X-Forwarded-For hoặc RemoteAddr)
     * @return URL redirect đến trang thanh toán VNPay
     */
    @Transactional
    public String createPaymentUrl(Long memberId, VNPayCreateUrlRequest request, String clientIp) {
        User member = validateAndGetMember(memberId);
        MembershipPackage pkg = validateAndGetActivePackage(request.getPackageId());

        Instant now = Instant.now(clock);
        String invoiceCode = generateCode("INV");
        String paymentCode = generateCode("PAY");

        // Tạo Invoice
        String orderInfo = request.getOrderInfo() != null && !request.getOrderInfo().isBlank()
                ? request.getOrderInfo()
                : "Thanh toan goi: " + pkg.getPackageCode();

        Invoice invoice = new Invoice(
                invoiceCode,
                member.getId(),
                pkg.getId(),
                pkg.getPrice(),
                now,
                orderInfo);
        invoice = invoices.save(invoice);

        // Tạo Payment (PENDING, method = VNPAY)
        Payment payment = new Payment(
                paymentCode,
                invoice.getId(),
                member.getId(),
                invoice.getAmount(),
                PaymentMethod.VNPAY,
                now,
                orderInfo);
        payments.save(payment);

        // Sinh URL VNPay
        return buildVNPayUrl(paymentCode, pkg.getPrice(), orderInfo, clientIp);
    }

    // ── Bước 2: IPN — VNPay gọi server khi thanh toán xong ───────────────

    /**
     * Xử lý IPN (Instant Payment Notification) từ VNPay.
     * VNPay POST trực tiếp vào server, không qua browser.
     *
     * @param params tất cả query params từ VNPay
     * @return "00" nếu xử lý OK, "97" nếu chữ ký sai, "01" nếu không tìm thấy đơn
     */
    @Transactional
    public String handleIpn(Map<String, String> params) {
        if (!verifySignature(params)) {
            log.warn("[VNPay IPN] Invalid signature: {}", params);
            return "97"; // Invalid signature
        }

        String paymentCode = params.get("vnp_TxnRef");
        String responseCode = params.get("vnp_ResponseCode");
        String transactionNo = params.get("vnp_TransactionNo");

        Payment payment = payments.findByPaymentCode(paymentCode).orElse(null);
        if (payment == null) {
            log.warn("[VNPay IPN] Payment not found: {}", paymentCode);
            return "01"; // Order not found
        }

        if (payment.getStatus() == PaymentStatus.COMPLETED) {
            return "00"; // Already processed (idempotent)
        }

        Instant now = Instant.now(clock);

        if ("00".equals(responseCode)) {
            // Thanh toán thành công
            payment.setStatus(PaymentStatus.COMPLETED);
            payment.setCompletedAt(now);
            payment.setTransactionReference(transactionNo);
            payments.save(payment);

            Invoice invoice = invoices.findById(payment.getInvoiceId())
                    .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));
            invoice.setStatus(InvoiceStatus.PAID);
            invoice.setPaidAt(now);
            invoices.save(invoice);

            MembershipPackage pkg = packages.findById(invoice.getPackageId())
                    .orElseThrow(() -> new ResourceNotFoundException("Package not found"));
            memberMemberships.activateMembership(payment.getMemberId(), pkg.getId(), pkg.getDurationDays());

            log.info("[VNPay IPN] Payment COMPLETED: code={}, txn={}", paymentCode, transactionNo);
        } else {
            // Thanh toán thất bại / hủy
            payment.setStatus(PaymentStatus.CANCELLED);
            payment.setNote("VNPay response code: " + responseCode);
            payments.save(payment);

            Invoice invoice = invoices.findById(payment.getInvoiceId()).orElse(null);
            if (invoice != null) {
                invoice.setStatus(InvoiceStatus.CANCELLED);
                invoices.save(invoice);
            }
            log.info("[VNPay IPN] Payment FAILED: code={}, responseCode={}", paymentCode, responseCode);
        }

        return "00";
    }

    // ── Bước 3: Callback — Browser redirect về sau khi thanh toán ─────────

    /**
     * Verify chữ ký VNPay khi browser redirect về (return URL).
     * Trả true nếu chữ ký hợp lệ.
     */
    public boolean verifyCallback(Map<String, String> params) {
        return verifySignature(params);
    }

    /**
     * Lấy trạng thái thanh toán theo paymentCode.
     */
    public Payment getPaymentByCode(String paymentCode) {
        return payments.findByPaymentCode(paymentCode)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found: " + paymentCode));
    }

    // ── Mock (không cần sandbox thật) ──────────────────────────────────────

    /**
     * Mock thành công thanh toán VNPay — dùng để demo / test không có tài khoản sandbox.
     * Gọi trực tiếp giả lập IPN thành công.
     */
    @Transactional
    public PaymentResponse mockSuccess(String paymentCode) {
        Payment payment = payments.findByPaymentCode(paymentCode)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found: " + paymentCode));

        if (payment.getStatus() == PaymentStatus.COMPLETED) {
            throw new BusinessException("Payment already completed");
        }
        if (payment.getPaymentMethod() != PaymentMethod.VNPAY) {
            throw new BusinessException("Only VNPAY payments can be mock-completed");
        }

        Instant now = Instant.now(clock);
        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setCompletedAt(now);
        payment.setTransactionReference("MOCK-VNPAY-" + paymentCode);
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

    // ── Internal helpers ───────────────────────────────────────────────────

    private String buildVNPayUrl(String paymentCode, Double amount, String orderInfo, String clientIp) {
        String createDate = new SimpleDateFormat("yyyyMMddHHmmss")
                .format(new Date(Instant.now(clock).toEpochMilli()));

        // VNPay yêu cầu amount = số tiền * 100 (đơn vị đồng, không thập phân)
        long vnpAmount = Math.round(amount * 100);

        Map<String, String> vnpParams = new HashMap<>();
        vnpParams.put("vnp_Version", vnPayConfig.getApiVersion());
        vnpParams.put("vnp_Command", vnPayConfig.getCommand());
        vnpParams.put("vnp_TmnCode", vnPayConfig.getTmnCode());
        vnpParams.put("vnp_Amount", String.valueOf(vnpAmount));
        vnpParams.put("vnp_CurrCode", vnPayConfig.getCurrencyCode());
        vnpParams.put("vnp_TxnRef", paymentCode);
        vnpParams.put("vnp_OrderInfo", orderInfo);
        vnpParams.put("vnp_OrderType", "other");
        vnpParams.put("vnp_Locale", vnPayConfig.getLocale());
        vnpParams.put("vnp_ReturnUrl", vnPayConfig.getReturnUrl());
        vnpParams.put("vnp_IpAddr", clientIp != null ? clientIp : "127.0.0.1");
        vnpParams.put("vnp_CreateDate", createDate);

        // Expire after 15 minutes
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        cal.add(Calendar.MINUTE, 15);
        vnpParams.put("vnp_ExpireDate", new SimpleDateFormat("yyyyMMddHHmmss").format(cal.getTime()));

        List<String> fieldNames = new ArrayList<>(vnpParams.keySet());
        Collections.sort(fieldNames);

        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        for (String fieldName : fieldNames) {
            String fieldValue = vnpParams.get(fieldName);
            if (fieldValue != null && !fieldValue.isEmpty()) {
                hashData.append(fieldName).append('=')
                        .append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
                query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII))
                        .append('=')
                        .append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
                hashData.append('&');
                query.append('&');
            }
        }
        // Remove trailing &
        if (!hashData.isEmpty()) hashData.deleteCharAt(hashData.length() - 1);
        if (!query.isEmpty()) query.deleteCharAt(query.length() - 1);

        String secureHash = hmacSHA512(vnPayConfig.getHashSecret(), hashData.toString());
        return vnPayConfig.getPaymentUrl() + "?" + query + "&vnp_SecureHash=" + secureHash;
    }

    private boolean verifySignature(Map<String, String> params) {
        String secureHash = params.get("vnp_SecureHash");
        if (secureHash == null) return false;

        Map<String, String> copy = new HashMap<>(params);
        copy.remove("vnp_SecureHash");
        copy.remove("vnp_SecureHashType");

        List<String> fieldNames = new ArrayList<>(copy.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        for (String fieldName : fieldNames) {
            String fieldValue = copy.get(fieldName);
            if (fieldValue != null && !fieldValue.isEmpty()) {
                hashData.append(fieldName).append('=')
                        .append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII))
                        .append('&');
            }
        }
        if (!hashData.isEmpty()) hashData.deleteCharAt(hashData.length() - 1);

        String computedHash = hmacSHA512(vnPayConfig.getHashSecret(), hashData.toString());
        return computedHash.equalsIgnoreCase(secureHash);
    }

    private static String hmacSHA512(String key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            mac.init(secretKey);
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(2 * hash.length);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception ex) {
            throw new RuntimeException("Failed to compute HMAC-SHA512", ex);
        }
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
