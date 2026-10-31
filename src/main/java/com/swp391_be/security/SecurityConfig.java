package com.swp391_be.security;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import com.swp391_be.service.CustomUserDetailsService;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    private final JwtAuthenticationFilter jwtFilter;
    private final CustomUserDetailsService users;

    public SecurityConfig(JwtAuthenticationFilter jwtFilter, CustomUserDetailsService users) {
        this.jwtFilter = jwtFilter;
        this.users = users;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider(PasswordEncoder encoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(users);
        provider.setPasswordEncoder(encoder);
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, DaoAuthenticationProvider provider)
            throws Exception {
        http.cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/auth/register", "/auth/login", "/auth/verify-email",
                                "/auth/resend-verification", "/api/test")
                        .permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        // Ai cũng xem được gói tập (trang chủ, trang đăng ký)
                        .requestMatchers(HttpMethod.GET, "/api/membership-packages/**").permitAll()
                        // Cho khách chưa đăng nhập xem danh sách lớp, lịch tập, phòng (B3)
                        .requestMatchers(HttpMethod.GET, "/api/classes/**", "/api/schedules/**", "/api/rooms/**")
                        .permitAll()
                        .requestMatchers("/api/member/**").hasRole("MEMBER")
                        // Cổng thanh toán PayOS
                        .requestMatchers("/api/payos/create-payment-link")
                        .hasAnyRole("MEMBER", "MANAGER", "RECEPTIONIST")
                        .requestMatchers("/api/payos/webhook", "/api/payos/callback", "/api/payos/cancel").permitAll()
                        .requestMatchers("/api/payos/mock-success/**").authenticated()
                        // Quản lý hóa đơn & thanh toán cho Receptionist và Manager
                        .requestMatchers("/api/payments/**").hasAnyRole("MANAGER", "RECEPTIONIST")
                        .requestMatchers("/api/invoices/**").hasAnyRole("MANAGER", "RECEPTIONIST")
                        // Đã đăng nhập thì xem được phòng, lớp, lịch, danh sách Coach
                        .requestMatchers(HttpMethod.GET, "/api/**").authenticated()
                        // Thêm / sửa / xóa gói, phòng, lớp, lịch: chỉ Center Manager
                        .requestMatchers("/api/**").hasRole("MANAGER")
                        .requestMatchers("/manager/**").hasRole("MANAGER")
                        .requestMatchers("/coach/**").hasRole("COACH")
                        .requestMatchers("/member/**").hasRole("MEMBER")
                        .requestMatchers("/receptionist/**").hasRole("RECEPTIONIST")
                        .anyRequest().authenticated())
                .authenticationProvider(provider)
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint(
                                (request, response, exception) -> writeError(response, 401, "Authentication required"))
                        .accessDeniedHandler(
                                (request, response, exception) -> writeError(response, 403, "Access denied")))
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    // Cho phép Front-End (Vite chạy ở localhost:5173) gọi API
    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins:http://localhost:5173}") List<String> allowedOrigins) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    private void writeError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"message\":\"" + message + "\"}");
    }
}
