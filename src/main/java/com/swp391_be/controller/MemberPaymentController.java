package com.swp391_be.controller;

import com.swp391_be.dto.ConfirmPaymentRequest;
import com.swp391_be.dto.CreatePaymentRequest;
import com.swp391_be.dto.InvoiceResponse;
import com.swp391_be.dto.MemberMembershipResponse;
import com.swp391_be.dto.PaymentResponse;
import com.swp391_be.entity.User;
import com.swp391_be.exception.BusinessException;
import com.swp391_be.exception.ResourceNotFoundException;
import com.swp391_be.repository.UserRepository;
import com.swp391_be.service.PaymentService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/member")
public class MemberPaymentController {

    private final PaymentService paymentService;
    private final UserRepository users;

    public MemberPaymentController(PaymentService paymentService, UserRepository users) {
        this.paymentService = paymentService;
        this.users = users;
    }

    @PostMapping("/payments/checkout")
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentResponse checkout(
            @Valid @RequestBody CreatePaymentRequest request,
            Authentication authentication) {
        User member = resolveMember(authentication);
        return paymentService.createPayment(member.getId(), request);
    }

    @PostMapping("/payments/{paymentId}/simulate-pay")
    public PaymentResponse simulatePay(
            @PathVariable Long paymentId,
            Authentication authentication) {
        User member = resolveMember(authentication);
        PaymentResponse payment = paymentService.getPaymentById(paymentId);
        if (!payment.getMemberId().equals(member.getId())) {
            throw new BusinessException("You are not authorized to pay for this invoice");
        }
        ConfirmPaymentRequest confirmRequest = new ConfirmPaymentRequest(
                "SIMULATED-ONLINE-TXN",
                "Thanh toán trực tuyến giả lập thành công");
        return paymentService.confirmPayment(paymentId, confirmRequest);
    }

    @GetMapping("/payments")
    public List<PaymentResponse> getMyPayments(Authentication authentication) {
        User member = resolveMember(authentication);
        return paymentService.getMyPayments(member.getId());
    }

    @GetMapping("/payments/{paymentId}")
    public PaymentResponse getPaymentById(
            @PathVariable Long paymentId,
            Authentication authentication) {
        User member = resolveMember(authentication);
        PaymentResponse response = paymentService.getPaymentById(paymentId);
        if (!response.getMemberId().equals(member.getId())) {
            throw new BusinessException("Access denied to this payment");
        }
        return response;
    }

    @GetMapping("/invoices")
    public List<InvoiceResponse> getMyInvoices(Authentication authentication) {
        User member = resolveMember(authentication);
        return paymentService.getMyInvoices(member.getId());
    }

    @GetMapping("/invoices/{invoiceId}")
    public InvoiceResponse getInvoiceById(
            @PathVariable Long invoiceId,
            Authentication authentication) {
        User member = resolveMember(authentication);
        InvoiceResponse response = paymentService.getInvoiceById(invoiceId);
        if (!response.getMemberId().equals(member.getId())) {
            throw new BusinessException("Access denied to this invoice");
        }
        return response;
    }

    @GetMapping("/memberships")
    public List<MemberMembershipResponse> getMyMemberships(Authentication authentication) {
        User member = resolveMember(authentication);
        return paymentService.getMemberMembershipDetails(member.getId());
    }

    private User resolveMember(Authentication authentication) {
        return users.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Member not found"));
    }
}
