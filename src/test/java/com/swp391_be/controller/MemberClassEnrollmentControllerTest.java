package com.swp391_be.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.swp391_be.dto.AvailableClassSessionResponse;
import com.swp391_be.dto.ClassEnrollmentResponse;
import com.swp391_be.entity.Role;
import com.swp391_be.entity.User;
import com.swp391_be.exception.BusinessException;
import com.swp391_be.repository.UserRepository;
import com.swp391_be.service.ClassEnrollmentService;
import java.time.Instant;
import java.time.LocalDateTime;
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
class MemberClassEnrollmentControllerTest {

    private static final String MEMBER_EMAIL = "member-controller@example.com";
    private static final String MANAGER_EMAIL = "manager-controller@example.com";
    private static final String COACH_EMAIL = "coach-controller@example.com";
    private static final String RECEPTIONIST_EMAIL = "receptionist-controller@example.com";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserRepository users;

    @MockitoBean
    private ClassEnrollmentService service;

    private Long memberId;

    @BeforeEach
    void setUp() {
        memberId = ensureUser(MEMBER_EMAIL, Role.MEMBER).getId();
        ensureUser(MANAGER_EMAIL, Role.MANAGER);
        ensureUser(COACH_EMAIL, Role.COACH);
        ensureUser(RECEPTIONIST_EMAIL, Role.RECEPTIONIST);
    }

    @Test
    void memberCanListAvailableSessions() throws Exception {
        when(service.getAvailableSessions(memberId)).thenReturn(List.of(new AvailableClassSessionResponse(
                10L, 20L, "Yoga", "Yoga", 30L, 40L,
                LocalDateTime.of(2026, 10, 1, 9, 0),
                LocalDateTime.of(2026, 10, 1, 10, 0),
                5, 2, 3, false)));

        mvc.perform(get("/api/member/class-sessions/available")
                        .with(user(MEMBER_EMAIL).roles("MEMBER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].scheduleId").value(10))
                .andExpect(jsonPath("$[0].remainingSlots").value(3));
    }

    @Test
    void memberCanEnroll() throws Exception {
        when(service.enroll(memberId, 10L)).thenReturn(new ClassEnrollmentResponse(
                99L, 10L, 20L, "Yoga",
                LocalDateTime.of(2026, 10, 1, 9, 0),
                LocalDateTime.of(2026, 10, 1, 10, 0),
                Instant.parse("2026-09-29T00:00:00Z")));

        mvc.perform(post("/api/member/class-sessions/10/enrollment")
                        .with(user(MEMBER_EMAIL).roles("MEMBER")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.enrollmentId").value(99))
                .andExpect(jsonPath("$.scheduleId").value(10));
    }

    @Test
    void memberCanCancel() throws Exception {
        mvc.perform(delete("/api/member/class-sessions/10/enrollment")
                        .with(user(MEMBER_EMAIL).roles("MEMBER")))
                .andExpect(status().isNoContent());

        verify(service).cancel(memberId, 10L);
    }

    @Test
    void expiredPackageReturns400WithStableMessage() throws Exception {
        when(service.enroll(memberId, 10L))
                .thenThrow(new BusinessException("Membership package is expired or does not cover this class session"));

        mvc.perform(post("/api/member/class-sessions/10/enrollment")
                        .with(user(MEMBER_EMAIL).roles("MEMBER")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Membership package is expired or does not cover this class session"));
    }

    @Test
    void fullClassReturns400WithStableMessage() throws Exception {
        when(service.enroll(memberId, 10L)).thenThrow(new BusinessException("Class session is full"));

        mvc.perform(post("/api/member/class-sessions/10/enrollment")
                        .with(user(MEMBER_EMAIL).roles("MEMBER")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Class session is full"));
    }

    @Test
    void unauthenticatedRequestReturns401() throws Exception {
        mvc.perform(get("/api/member/class-sessions/available")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/member/class-enrollments")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/member/class-sessions/10/enrollment")).andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/member/class-sessions/10/enrollment")).andExpect(status().isUnauthorized());
    }

    @Test
    void managerCoachAndReceptionistReceive403() throws Exception {
        for (String role : List.of("MANAGER", "COACH", "RECEPTIONIST")) {
            mvc.perform(get("/api/member/class-sessions/available")
                            .with(user(role.toLowerCase() + "-controller@example.com").roles(role)))
                    .andExpect(status().isForbidden());
            mvc.perform(post("/api/member/class-sessions/10/enrollment")
                            .with(user(role.toLowerCase() + "-controller@example.com").roles(role)))
                    .andExpect(status().isForbidden());
            mvc.perform(delete("/api/member/class-sessions/10/enrollment")
                            .with(user(role.toLowerCase() + "-controller@example.com").roles(role)))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void requestCannotChooseAnotherMemberId() throws Exception {
        mvc.perform(post("/api/member/class-sessions/10/enrollment")
                        .with(user(MEMBER_EMAIL).roles("MEMBER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"memberId\":999}"))
                .andExpect(status().isCreated());

        verify(service).enroll(memberId, 10L);
        verify(service, org.mockito.Mockito.never()).enroll(999L, 10L);
    }

    @Test
    void memberCanListOwnEnrollments() throws Exception {
        when(service.getMyEnrollments(memberId)).thenReturn(List.of(new ClassEnrollmentResponse(
                99L, 10L, 20L, "Yoga",
                LocalDateTime.of(2026, 10, 1, 9, 0),
                LocalDateTime.of(2026, 10, 1, 10, 0),
                Instant.parse("2026-09-29T00:00:00Z"))));

        mvc.perform(get("/api/member/class-enrollments")
                        .with(user(MEMBER_EMAIL).roles("MEMBER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].scheduleId").value(10));
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
