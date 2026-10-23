package com.swp391_be.controller;

import com.swp391_be.dto.PayOSCreateRequest;
import com.swp391_be.dto.PaymentResponse;
import com.swp391_be.entity.User;
import com.swp391_be.exception.ResourceNotFoundException;
import com.swp391_be.repository.UserRepository;
import com.swp391_be.service.PayOSService;
import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * PayOS payment controller — thay thế VNPayController.
 *
 * Endpoints:
 *   POST   /api/payos/create-payment-link  — member tạo link thanh toán PayOS
 *   POST   /api/payos/webhook              — PayOS gọi webhook (server-to-server)
 *   GET    /api/payos/callback             — PayOS redirect browser về (return URL)
 *   GET    /api/payos/cancel               — PayOS redirect khi hủy thanh toán
 *   POST   /api/payos/mock-success/{code}  — Mock thành công (demo/test)
 */
@RestController
@RequestMapping("/api/payos")
public class PayOSController {

    private final PayOSService payOSService;
    private final UserRepository users;

    public PayOSController(PayOSService payOSService, UserRepository users) {
        this.payOSService = payOSService;
        this.users = users;
    }

    /**
     * Bước 1: Member tạo link thanh toán PayOS.
     * Trả về: { "paymentUrl": "https://pay.payos.vn/web/..." }
     */
    @PostMapping("/create-payment-link")
    public ResponseEntity<Map<String, String>> createPaymentLink(
            @Valid @RequestBody PayOSCreateRequest request,
            Authentication authentication) {
        User member = resolveMember(authentication);
        String url = payOSService.createPaymentLink(member.getId(), request);
        return ResponseEntity.ok(Map.of("paymentUrl", url));
    }

    /**
     * Bước 2: PayOS gọi webhook (server-to-server, không cần auth).
     * Trả về: { "success": true }
     */
    @PostMapping("/webhook")
    public ResponseEntity<Map<String, Object>> webhook(@RequestBody String body) {
        payOSService.handleWebhook(body);
        return ResponseEntity.ok(Map.of("success", true));
    }

    /**
     * Bước 3: Browser redirect về sau thanh toán (return URL).
     * FE sẽ đọc orderCode từ query param để hiển thị kết quả.
     */
    @GetMapping("/callback")
    public ResponseEntity<Map<String, Object>> callback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String id,
            @RequestParam(required = false) String cancel,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long orderCode) {

        Map<String, Object> result = new HashMap<>();
        result.put("code", code);
        result.put("id", id);
        result.put("cancel", cancel);
        result.put("status", status);
        result.put("orderCode", orderCode);
        result.put("success", "00".equals(code) && !"true".equals(cancel));
        result.put("message", "true".equals(cancel) ? "Payment cancelled" :
                "00".equals(code) ? "Payment successful" : "Payment failed");

        return ResponseEntity.ok(result);
    }

    /**
     * PayOS redirect khi user hủy thanh toán.
     */
    @GetMapping("/cancel")
    public ResponseEntity<Map<String, Object>> cancel(
            @RequestParam(required = false) Long orderCode) {
        Map<String, Object> result = new HashMap<>();
        result.put("orderCode", orderCode);
        result.put("success", false);
        result.put("message", "Payment cancelled by user");
        return ResponseEntity.ok(result);
    }

    /**
     * Mock PayOS thành công — chỉ dùng để demo khi không có tài khoản PayOS thật.
     */
    @PostMapping("/mock-success/{paymentCode}")
    public PaymentResponse mockSuccess(@PathVariable String paymentCode) {
        return payOSService.mockSuccess(paymentCode);
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private User resolveMember(Authentication authentication) {
        return users.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Member not found"));
    }
}
