package com.swp391_be.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

/**
 * Hội viên đăng ký LỚP. Phải đăng ký lớp thì mới đăng ký được các buổi học
 * (ClassEnrollment) của lớp đó.
 */
@Entity
@Table(name = "class_registrations", uniqueConstraints = @UniqueConstraint(name = "uk_class_registration_member_class", columnNames = {
        "member_id", "class_id" }), indexes = @Index(name = "idx_class_registration_class", columnList = "class_id"))
public class ClassRegistration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "class_id", nullable = false)
    private Long classId;

    @Column(name = "registered_at", nullable = false, updatable = false)
    private Instant registeredAt;

    protected ClassRegistration() {
    }

    public ClassRegistration(Long memberId, Long classId, Instant registeredAt) {
        this.memberId = memberId;
        this.classId = classId;
        this.registeredAt = registeredAt;
    }

    public Long getId() {
        return id;
    }

    public Long getMemberId() {
        return memberId;
    }

    public Long getClassId() {
        return classId;
    }

    public Instant getRegisteredAt() {
        return registeredAt;
    }
}