package com.swp391_be.service;

import com.swp391_be.dto.AuthResponse;
import com.swp391_be.dto.LoginRequest;
import com.swp391_be.dto.RegisterRequest;
import com.swp391_be.entity.Role;
import com.swp391_be.entity.User;
import com.swp391_be.exception.EmailAlreadyExistsException;
import com.swp391_be.exception.UserNameAlreadyExistsException;
import com.swp391_be.repository.UserRepository;
import com.swp391_be.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final AuthenticationManager authenticationManager;
    public AuthService(UserRepository users, PasswordEncoder encoder, JwtService jwt, AuthenticationManager authenticationManager) {
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
        this.authenticationManager = authenticationManager;
    }
    public AuthResponse register(RegisterRequest request) {
        if (users.existsByEmail(request.email())) throw new EmailAlreadyExistsException(request.email());
        if (users.existsByUserName(request.userName())) throw new UserNameAlreadyExistsException(request.userName());
        User user = users.save(new User(request.userName(), request.fullName(), request.email(),
                encoder.encode(request.password()), request.phone(), Role.MEMBER));
        return response(user);
    }
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        User user = users.findByEmail(request.email())
                .orElseThrow(() -> new IllegalStateException("Authenticated user missing from database"));
        return response(user);
    }
    private AuthResponse response(User user) {
        return new AuthResponse(jwt.generateToken(user.getEmail(), user.getRole().name()),
                user.getEmail(), user.getFullName(), user.getRole().name(), user.getId());
    }
}
