package com.swp391_be.controller;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.swp391_be.dto.InvoiceResponse;
import com.swp391_be.entity.InvoiceStatus;
import com.swp391_be.service.PaymentService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class InvoiceControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private PaymentService paymentService;

    @Test
    void receptionistCanGetAllInvoices() throws Exception {
        when(paymentService.getAllInvoices()).thenReturn(List.of(new InvoiceResponse(
                1L, "INV-001", 2L, "Nguyen Van A", 3L, "Goi 1 thang",
                500000.0, InvoiceStatus.PAID, Instant.now(), Instant.now(), "Da thanh toan")));

        mvc.perform(get("/api/invoices")
                        .with(user("receptionist@example.com").roles("RECEPTIONIST")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].invoiceId").value(1))
                .andExpect(jsonPath("$[0].invoiceCode").value("INV-001"));
    }

    @Test
    void managerCanGetInvoiceById() throws Exception {
        when(paymentService.getInvoiceById(1L)).thenReturn(new InvoiceResponse(
                1L, "INV-001", 2L, "Nguyen Van A", 3L, "Goi 1 thang",
                500000.0, InvoiceStatus.PAID, Instant.now(), Instant.now(), "Da thanh toan"));

        mvc.perform(get("/api/invoices/1")
                        .with(user("manager@example.com").roles("MANAGER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.invoiceId").value(1));
    }

    @Test
    void unauthenticatedAccessReturns401() throws Exception {
        mvc.perform(get("/api/invoices")).andExpect(status().isUnauthorized());
    }
}
