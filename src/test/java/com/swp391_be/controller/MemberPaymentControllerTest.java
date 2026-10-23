package com.swp391_be.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.swp391_be.dto.InvoiceResponse;
import com.swp391_be.dto.MemberMembershipResponse;
import com.swp391_be.dto.PaymentResponse;
import com.swp391_be.entity.InvoiceStatus;
import com.swp391_be.entity.PaymentMethod;
import com.swp391_be.entity.PaymentStatus;
import com.swp391_be.entity.Role;
import com.swp391_be.entity.User;
import com.swp391_be.repository.UserRepository;
import com.swp391_be.service.PaymentService;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
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
class MemberPaymentControllerTest {

    private static final String MEMBER_EMAIL = "member-payment@example.com";
    private static final String COACH_EMAIL = "coach-payment@example.com";
    private static final String MANAGER_EMAIL = "manager-payment@example.com";
    private static final String RECEPTIONIST_EMAIL = "receptionist-payment@example.com";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserRepository users;

    @MockitoBean
    private PaymentService paymentService;

    private Long memberId;

    @BeforeEach
    void setUp() {
        memberId = ensureUser(MEMBER_EMAIL, Role.MEMBER).getId();
        ensureUser(COACH_EMAIL, Role.COACH);
        ensureUser(MANAGER_EMAIL, Role.MANAGER);
        ensureUser(RECEPTIONIST_EMAIL, Role.RECEPTIONIST);
    }

    @Test
    void memberCanCheckout() throws Exception {
        PaymentResponse response = new PaymentResponse(
                10L, "PAY-001", 20L, "INV-001", memberId, "Demo Member",
                1L, "Gói 1 tháng", 500000.0, PaymentMethod.PAYOS,
                PaymentStatus.PENDING, null, Instant.now(), null, "Thanh toán");

        when(paymentService.createPayment(eq(memberId), any())).thenReturn(response);

        mvc.perform(post("/api/member/payments/checkout")
                        .with(user(MEMBER_EMAIL).roles("MEMBER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "packageId": 1,
                                    "paymentMethod": "PAYOS",
                                    "note": "Thanh toán"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.paymentId").value(10))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.amount").value(500000.0));
    }

    @Test
    void memberCanSimulatePaymentSuccess() throws Exception {
        PaymentResponse existing = new PaymentResponse(
                10L, "PAY-001", 20L, "INV-001", memberId, "Demo Member",
                1L, "Gói 1 tháng", 500000.0, PaymentMethod.PAYOS,
                PaymentStatus.PENDING, null, Instant.now(), null, "Thanh toán");

        PaymentResponse completed = new PaymentResponse(
                10L, "PAY-001", 20L, "INV-001", memberId, "Demo Member",
                1L, "Gói 1 tháng", 500000.0, PaymentMethod.PAYOS,
                PaymentStatus.COMPLETED, "SIMULATED-ONLINE-TXN", Instant.now(), Instant.now(), "Thanh toán");

        when(paymentService.getPaymentById(10L)).thenReturn(existing);
        when(paymentService.confirmPayment(eq(10L), any())).thenReturn(completed);

        mvc.perform(post("/api/member/payments/10/simulate-pay")
                        .with(user(MEMBER_EMAIL).roles("MEMBER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void memberCanListOwnPayments() throws Exception {
        when(paymentService.getMyPayments(memberId)).thenReturn(List.of(new PaymentResponse(
                10L, "PAY-001", 20L, "INV-001", memberId, "Demo Member",
                1L, "Gói 1 tháng", 500000.0, PaymentMethod.PAYOS,
                PaymentStatus.COMPLETED, null, Instant.now(), Instant.now(), null)));

        mvc.perform(get("/api/member/payments")
                        .with(user(MEMBER_EMAIL).roles("MEMBER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].paymentId").value(10));
    }

    @Test
    void memberCanListOwnInvoices() throws Exception {
        when(paymentService.getMyInvoices(memberId)).thenReturn(List.of(new InvoiceResponse(
                20L, "INV-001", memberId, "Demo Member", 1L, "Gói 1 tháng",
                500000.0, InvoiceStatus.PAID, Instant.now(), Instant.now(), null)));

        mvc.perform(get("/api/member/invoices")
                        .with(user(MEMBER_EMAIL).roles("MEMBER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].invoiceId").value(20));
    }

    @Test
    void memberCanListOwnMemberships() throws Exception {
        when(paymentService.getMemberMembershipDetails(memberId)).thenReturn(List.of(new MemberMembershipResponse(
                99L, memberId, 1L, "Gói 1 tháng",
                LocalDate.of(2026, 10, 8), LocalDate.of(2026, 11, 6), true, 30)));

        mvc.perform(get("/api/member/memberships")
                        .with(user(MEMBER_EMAIL).roles("MEMBER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(99))
                .andExpect(jsonPath("$[0].active").value(true))
                .andExpect(jsonPath("$[0].daysRemaining").value(30));
    }

    @Test
    void unauthenticatedAccessReturns401() throws Exception {
        mvc.perform(get("/api/member/payments")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/member/payments/checkout")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/member/invoices")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/member/memberships")).andExpect(status().isUnauthorized());
    }

    @Test
    void otherRolesReceive403OnMemberEndpoints() throws Exception {
        for (String role : List.of("COACH", "MANAGER", "RECEPTIONIST")) {
            mvc.perform(get("/api/member/payments")
                            .with(user(role.toLowerCase() + "-payment@example.com").roles(role)))
                    .andExpect(status().isForbidden());
        }
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
