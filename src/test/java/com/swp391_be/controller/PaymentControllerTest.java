package com.swp391_be.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.swp391_be.dto.CashCheckoutRequest;
import com.swp391_be.dto.PaymentResponse;
import com.swp391_be.entity.PaymentMethod;
import com.swp391_be.entity.PaymentStatus;
import com.swp391_be.service.PaymentService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class PaymentControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private PaymentService paymentService;

    @Test
    void managerCanPerformCashCheckout() throws Exception {
        PaymentResponse response = new PaymentResponse(
                1L, "PAY-001", 10L, "INV-001", 2L, "Hoi Vien",
                3L, "Goi 1 thang", 500000.0, PaymentMethod.CASH,
                PaymentStatus.COMPLETED, "CASH-INV-001", Instant.now(), Instant.now(), "Quay thu tien");

        when(paymentService.directCashCheckout(any(CashCheckoutRequest.class))).thenReturn(response);

        mvc.perform(post("/api/payments/cash-checkout")
                        .with(user("manager@example.com").roles("MANAGER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "memberId": 2,
                                    "packageId": 3,
                                    "note": "Quay thu tien"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.paymentId").value(1))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void receptionistCanPerformCashCheckout() throws Exception {
        PaymentResponse response = new PaymentResponse(
                1L, "PAY-001", 10L, "INV-001", 2L, "Hoi Vien",
                3L, "Goi 1 thang", 500000.0, PaymentMethod.CASH,
                PaymentStatus.COMPLETED, "CASH-INV-001", Instant.now(), Instant.now(), "Quay thu tien");

        when(paymentService.directCashCheckout(any(CashCheckoutRequest.class))).thenReturn(response);

        mvc.perform(post("/api/payments/cash-checkout")
                        .with(user("receptionist@example.com").roles("RECEPTIONIST"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "memberId": 2,
                                    "packageId": 3,
                                    "note": "Quay thu tien"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.paymentId").value(1));
    }

    @Test
    void receptionistCanConfirmPayment() throws Exception {
        PaymentResponse response = new PaymentResponse(
                1L, "PAY-001", 10L, "INV-001", 2L, "Hoi Vien",
                3L, "Goi 1 thang", 500000.0, PaymentMethod.BANK_TRANSFER,
                PaymentStatus.COMPLETED, "TXN-999", Instant.now(), Instant.now(), "Da nhan chuyen khoan");

        when(paymentService.confirmPayment(eq(1L), any())).thenReturn(response);

        mvc.perform(post("/api/payments/1/confirm")
                        .with(user("receptionist@example.com").roles("RECEPTIONIST"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "transactionReference": "TXN-999",
                                    "note": "Da nhan chuyen khoan"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void receptionistCanCancelPayment() throws Exception {
        PaymentResponse response = new PaymentResponse(
                1L, "PAY-001", 10L, "INV-001", 2L, "Hoi Vien",
                3L, "Goi 1 thang", 500000.0, PaymentMethod.CASH,
                PaymentStatus.CANCELLED, null, Instant.now(), null, "Khach doi y");

        when(paymentService.cancelPayment(eq(1L), eq("Khach doi y"))).thenReturn(response);

        mvc.perform(post("/api/payments/1/cancel")
                        .with(user("receptionist@example.com").roles("RECEPTIONIST"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "reason": "Khach doi y"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void memberCannotAccessStaffPaymentEndpoints() throws Exception {
        mvc.perform(post("/api/payments/cash-checkout")
                        .with(user("member@example.com").roles("MEMBER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "memberId": 2,
                                    "packageId": 3
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void coachCannotAccessStaffPaymentEndpoints() throws Exception {
        mvc.perform(post("/api/payments/cash-checkout")
                        .with(user("coach@example.com").roles("COACH"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "memberId": 2,
                                    "packageId": 3
                                }
                                """))
                .andExpect(status().isForbidden());
    }
}
