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

@Entity
@Table(name = "class_enrollments",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_enrollment_member_schedule",
                columnNames = {"member_id", "schedule_id"}),
        indexes = @Index(name = "idx_enrollment_schedule", columnList = "schedule_id"))
public class ClassEnrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "schedule_id", nullable = false)
    private Long scheduleId;

    @Column(name = "enrolled_at", nullable = false, updatable = false)
    private Instant enrolledAt;

    protected ClassEnrollment() {
    }

    public ClassEnrollment(Long memberId, Long scheduleId, Instant enrolledAt) {
        this.memberId = memberId;
        this.scheduleId = scheduleId;
        this.enrolledAt = enrolledAt;
    }

    public Long getId() {
        return id;
    }

    public Long getMemberId() {
        return memberId;
    }

    public Long getScheduleId() {
        return scheduleId;
    }

    public Instant getEnrolledAt() {
        return enrolledAt;
    }
}
