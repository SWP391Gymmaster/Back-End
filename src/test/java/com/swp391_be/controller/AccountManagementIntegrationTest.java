package com.swp391_be.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.swp391_be.entity.Role;
import com.swp391_be.entity.User;
import com.swp391_be.repository.UserRepository;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.reset;
import org.mockito.ArgumentCaptor;

@SpringBootTest
@AutoConfigureMockMvc
class AccountManagementIntegrationTest {
    @Autowired private MockMvc mvc;
    @Autowired private UserRepository users;
    @Autowired private PasswordEncoder encoder;
    @MockitoBean private JavaMailSender mail;

    private String suffix;
    private User admin;

    @BeforeEach
    void setUp() {
        reset(mail);
        suffix = UUID.randomUUID().toString().substring(0, 8);
        admin = new User("admin_" + suffix, "Test Admin", "admin_" + suffix + "@example.com",
                encoder.encode("password123"), null, Role.ADMIN);
        admin.setEmailVerified(true);
        admin = users.saveAndFlush(admin);
    }

    @Test
    void registrationRequiresEmailVerificationBeforeLogin() throws Exception {
        String email = "new_" + suffix + "@example.com";
        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userName":"new_%s","fullName":"New Member","email":"%s","password":"password123"}
                                """.formatted(suffix, email)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").doesNotExist())
                .andExpect(jsonPath("$.verificationRequired").value(true));

        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{" + "\"email\":\"" + email + "\",\"password\":\"password123\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("EMAIL_NOT_VERIFIED"));

        ArgumentCaptor<SimpleMailMessage> message = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mail).send(message.capture());
        Matcher token = Pattern.compile("token=([A-Za-z0-9_-]+)").matcher(message.getValue().getText());
        if (!token.find()) throw new AssertionError("verification link did not contain a token");

        mvc.perform(post("/auth/verify-email").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + token.group(1) + "\"}"))
                .andExpect(status().isOk());

        mvc.perform(post("/auth/verify-email").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + token.group(1) + "\"}"))
                .andExpect(status().isBadRequest());

        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{" + "\"email\":\"" + email + "\",\"password\":\"password123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void adminCanCreateAndListManagedAccountsButCannotReadAdmins() throws Exception {
        mvc.perform(post("/api/admin/users").with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userName":"coach_%s","fullName":"Test Coach","email":"coach_%s@example.com","password":"password123","role":"COACH"}
                                """.formatted(suffix, suffix)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.role").value("COACH"))
                .andExpect(jsonPath("$.user.emailVerified").value(false))
                .andExpect(jsonPath("$.verificationEmailStatus").value("SENT"));

        mvc.perform(get("/api/admin/users").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.role == 'ADMIN')]").isEmpty());

        mvc.perform(get("/api/admin/users/" + admin.getId()).with(user("admin").roles("ADMIN")))
                .andExpect(status().isNotFound());

        mvc.perform(get("/api/admin/users").with(user("manager").roles("MANAGER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCannotAssignAdminRole() throws Exception {
        String email = "manager_" + suffix + "@example.com";
        mvc.perform(post("/api/admin/users").with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userName":"manager_%s","fullName":"Test Manager","email":"%s","password":"password123","role":"MANAGER"}
                                """.formatted(suffix, email)))
                .andExpect(status().isCreated());
        Long managedUserId = users.findByEmail(email).orElseThrow().getId();

        mvc.perform(patch("/api/admin/users/" + managedUserId).with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"ADMIN\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ADMIN_ROLE_NOT_ALLOWED"));
    }
}
