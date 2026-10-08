package com.swp391_be.controller;

import com.swp391_be.dto.InvoiceResponse;
import com.swp391_be.service.PaymentService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/invoices")
public class InvoiceController {

    private final PaymentService paymentService;

    public InvoiceController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping
    public List<InvoiceResponse> getAllInvoices() {
        return paymentService.getAllInvoices();
    }

    @GetMapping("/{id}")
    public InvoiceResponse getInvoiceById(@PathVariable Long id) {
        return paymentService.getInvoiceById(id);
    }
}
