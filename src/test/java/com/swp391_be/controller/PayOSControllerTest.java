package com.swp391_be.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.swp391_be.dto.PaymentResponse;
import com.swp391_be.entity.PaymentMethod;
import com.swp391_be.entity.PaymentStatus;
import com.swp391_be.entity.Role;
import com.swp391_be.entity.User;
import com.swp391_be.repository.UserRepository;
import com.swp391_be.service.PayOSService;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class PayOSControllerTest {

    private static final String MEMBER_EMAIL = "member-payos-test@example.com";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserRepository users;

    @MockitoBean
    private PayOSService payOSService;

    private Long memberId;

    @BeforeEach
    void setUp() {
        memberId = ensureUser(MEMBER_EMAIL, Role.MEMBER).getId();
    }

    @Test
    void memberCanCreatePaymentLink() throws Exception {
        when(payOSService.createPaymentLink(eq(memberId), any()))
                .thenReturn("https://pay.payos.vn/web/checkout-link-test");

        mvc.perform(post("/api/payos/create-payment-link")
                        .with(user(MEMBER_EMAIL).roles("MEMBER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "packageId": 1,
                                    "description": "Thanh toan goi tap"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentUrl").value("https://pay.payos.vn/web/checkout-link-test"));
    }

    @Test
    void unauthenticatedCannotCreatePaymentLink() throws Exception {
        mvc.perform(post("/api/payos/create-payment-link")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "packageId": 1
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void webhookIsPubliclyAccessible() throws Exception {
        // doNothing for void webhook handler
        doNothing().when(payOSService).handleWebhook(any());

        mvc.perform(post("/api/payos/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"00\",\"data\":{\"orderCode\":123456}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void callbackIsPubliclyAccessible() throws Exception {
        mvc.perform(get("/api/payos/callback")
                        .param("code", "00")
                        .param("id", "test-id")
                        .param("cancel", "false")
                        .param("status", "PAID")
                        .param("orderCode", "123456"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("00"));
    }

    @Test
    void cancelIsPubliclyAccessible() throws Exception {
        mvc.perform(get("/api/payos/cancel")
                        .param("orderCode", "123456"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Payment cancelled by user"));
    }

    @Test
    void mockSuccessAccessibleByAuthenticatedUser() throws Exception {
        PaymentResponse response = new PaymentResponse(
                10L, "PAY-MOCK-001", 20L, "INV-001", memberId, "Demo Member",
                1L, "Gói 1 tháng", 500000.0, PaymentMethod.PAYOS,
                PaymentStatus.COMPLETED, "MOCK-PAYOS-001", Instant.now(), Instant.now(), null);

        when(payOSService.mockSuccess("PAY-MOCK-001")).thenReturn(response);

        mvc.perform(post("/api/payos/mock-success/PAY-MOCK-001")
                        .with(user(MEMBER_EMAIL).roles("MEMBER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.paymentCode").value("PAY-MOCK-001"));
    }

    private User ensureUser(String email, Role role) {
        return users.findByEmail(email).orElseGet(() -> users.save(new User(
                email.substring(0, email.indexOf('@')),
                email,
                email,
                "hash",
                null,
                role)));
    }
}
