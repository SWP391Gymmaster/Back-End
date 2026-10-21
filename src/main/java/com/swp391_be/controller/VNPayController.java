package com.swp391_be.controller;

import com.swp391_be.dto.PaymentResponse;
import com.swp391_be.dto.VNPayCreateUrlRequest;
import com.swp391_be.entity.User;
import com.swp391_be.exception.ResourceNotFoundException;
import com.swp391_be.repository.UserRepository;
import com.swp391_be.service.VNPayService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * VNPay payment controller (T5.5).
 *
 * Endpoints:
 *   POST   /api/vnpay/create-payment-url  — member tạo URL thanh toán
 *   POST   /api/vnpay/ipn                 — VNPay gọi IPN (server-to-server)
 *   GET    /api/vnpay/callback            — VNPay redirect browser về (return URL)
 *   POST   /api/vnpay/mock-success/{code} — Mock thành công (demo/test)
 */
@RestController
@RequestMapping("/api/vnpay")
public class VNPayController {

    private final VNPayService vnPayService;
    private final UserRepository users;

    public VNPayController(VNPayService vnPayService, UserRepository users) {
        this.vnPayService = vnPayService;
        this.users = users;
    }

    /**
     * Bước 1: Member tạo URL thanh toán VNPay.
     * Trả về: { "paymentUrl": "https://sandbox.vnpayment.vn/..." }
     */
    @PostMapping("/create-payment-url")
    public ResponseEntity<Map<String, String>> createPaymentUrl(
            @Valid @RequestBody VNPayCreateUrlRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        User member = resolveMember(authentication);
        String clientIp = resolveClientIp(httpRequest);
        String url = vnPayService.createPaymentUrl(member.getId(), request, clientIp);
        return ResponseEntity.ok(Map.of("paymentUrl", url));
    }

    /**
     * Bước 2: VNPay gọi IPN (server-to-server, không cần auth).
     * Trả về: { "RspCode": "00", "Message": "Confirm Success" }
     */
    @PostMapping("/ipn")
    public ResponseEntity<Map<String, String>> ipn(@RequestParam Map<String, String> params) {
        String rspCode = vnPayService.handleIpn(params);
        String message = "00".equals(rspCode) ? "Confirm Success" : "Error";
        return ResponseEntity.ok(Map.of("RspCode", rspCode, "Message", message));
    }

    /**
     * Bước 3: Browser redirect về sau thanh toán.
     * Verify chữ ký VNPay, trả kết quả.
     */
    @GetMapping("/callback")
    public ResponseEntity<Map<String, Object>> callback(@RequestParam Map<String, String> params) {
        boolean valid = vnPayService.verifyCallback(params);
        String responseCode = params.get("vnp_ResponseCode");
        String paymentCode = params.get("vnp_TxnRef");

        Map<String, Object> result = new HashMap<>();
        result.put("signatureValid", valid);
        result.put("responseCode", responseCode);
        result.put("paymentCode", paymentCode);
        result.put("success", valid && "00".equals(responseCode));
        result.put("message", "00".equals(responseCode) ? "Payment successful" : "Payment failed or cancelled");

        return ResponseEntity.ok(result);
    }

    /**
     * Mock VNPay thành công — chỉ dùng để demo khi không có tài khoản sandbox thật.
     * Gọi với paymentCode từ bước 1.
     */
    @PostMapping("/mock-success/{paymentCode}")
    public PaymentResponse mockSuccess(@PathVariable String paymentCode) {
        return vnPayService.mockSuccess(paymentCode);
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private User resolveMember(Authentication authentication) {
        return users.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Member not found"));
    }

    private String resolveClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
