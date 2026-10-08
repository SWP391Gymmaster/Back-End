package com.swp391_be.controller;

import com.swp391_be.dto.CashCheckoutRequest;
import com.swp391_be.dto.ConfirmPaymentRequest;
import com.swp391_be.dto.PaymentResponse;
import com.swp391_be.service.PaymentService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping
    public List<PaymentResponse> getAllPayments() {
        return paymentService.getAllPayments();
    }

    @GetMapping("/{id}")
    public PaymentResponse getPaymentById(@PathVariable Long id) {
        return paymentService.getPaymentById(id);
    }

    @PostMapping("/cash-checkout")
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentResponse directCashCheckout(@Valid @RequestBody CashCheckoutRequest request) {
        return paymentService.directCashCheckout(request);
    }

    @PostMapping("/{id}/confirm")
    public PaymentResponse confirmPayment(
            @PathVariable Long id,
            @RequestBody(required = false) ConfirmPaymentRequest request) {
        return paymentService.confirmPayment(id, request);
    }

    @PostMapping("/{id}/cancel")
    public PaymentResponse cancelPayment(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body) {
        String reason = body != null ? body.get("reason") : null;
        return paymentService.cancelPayment(id, reason);
    }
}
