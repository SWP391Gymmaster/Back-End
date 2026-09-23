package com.swp391_be.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_username", columnNames = "user_name"),
        @UniqueConstraint(name = "uk_user_email", columnNames = "email")
})
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_name", nullable = false)
    private String userName;
    @Column(name = "full_name", nullable = false)
    private String fullName;
    @Column(nullable = false)
    private String email;
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;
    private String phone;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private Role role;

    protected User() {}
    public User(String userName, String fullName, String email, String passwordHash, String phone, Role role) {
        this.userName = userName;
        this.fullName = fullName;
        this.email = email;
        this.passwordHash = passwordHash;
        this.phone = phone;
        this.role = role;
    }
    public Long getId() { return id; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
}
