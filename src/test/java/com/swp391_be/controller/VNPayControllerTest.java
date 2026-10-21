package com.swp391_be.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import com.swp391_be.service.VNPayService;
import java.time.Instant;
import java.util.Map;
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
class VNPayControllerTest {

    private static final String MEMBER_EMAIL = "member-vnpay-test@example.com";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserRepository users;

    @MockitoBean
    private VNPayService vnPayService;

    private Long memberId;

    @BeforeEach
    void setUp() {
        memberId = ensureUser(MEMBER_EMAIL, Role.MEMBER).getId();
    }

    @Test
    void memberCanCreatePaymentUrl() throws Exception {
        when(vnPayService.createPaymentUrl(eq(memberId), any(), any()))
                .thenReturn("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?test=1");

        mvc.perform(post("/api/vnpay/create-payment-url")
                        .with(user(MEMBER_EMAIL).roles("MEMBER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "packageId": 1,
                                    "orderInfo": "Thanh toan goi tap"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentUrl").value("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?test=1"));
    }

    @Test
    void unauthenticatedCannotCreatePaymentUrl() throws Exception {
        mvc.perform(post("/api/vnpay/create-payment-url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "packageId": 1
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void ipnWebhookIsPubliclyAccessible() throws Exception {
        when(vnPayService.handleIpn(any())).thenReturn("00");

        mvc.perform(post("/api/vnpay/ipn")
                        .param("vnp_ResponseCode", "00")
                        .param("vnp_TxnRef", "PAY-TEST-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.RspCode").value("00"))
                .andExpect(jsonPath("$.Message").value("Confirm Success"));
    }

    @Test
    void callbackIsPubliclyAccessible() throws Exception {
        when(vnPayService.verifyCallback(any())).thenReturn(true);

        mvc.perform(get("/api/vnpay/callback")
                        .param("vnp_ResponseCode", "00")
                        .param("vnp_TxnRef", "PAY-TEST-001")
                        .param("vnp_SecureHash", "mockhash"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.responseCode").value("00"));
    }

    @Test
    void mockSuccessAccessibleByAuthenticatedUser() throws Exception {
        PaymentResponse response = new PaymentResponse(
                10L, "PAY-MOCK-001", 20L, "INV-001", memberId, "Demo Member",
                1L, "Gói 1 tháng", 500000.0, PaymentMethod.VNPAY,
                PaymentStatus.COMPLETED, "MOCK-VNPAY-001", Instant.now(), Instant.now(), null);

        when(vnPayService.mockSuccess("PAY-MOCK-001")).thenReturn(response);

        mvc.perform(post("/api/vnpay/mock-success/PAY-MOCK-001")
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
