package com.swp391_be.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Lớp tập (GymClass).
 * Mỗi lớp được nhận dạng bằng mã (classCode) duy nhất, ví dụ: CLS-BOXING-BASIC.
 * Mã lớp dùng thay tên để tránh sai sót khi tra cứu và liên kết.
 *
 * Đổi tên từ "Class" để không trùng với java.lang.Class. Tên bảng vẫn là "classes".
 */
@Entity
@Table(name = "classes",
        uniqueConstraints = @UniqueConstraint(name = "uk_class_code", columnNames = "class_code"))
public class GymClass {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Mã lớp — định danh duy nhất, ví dụ: CLS-BOXING-001, CLS-YOGA-MORNING.
     * Dùng mã thay vì tên để tra cứu chính xác.
     */
    @NotBlank
    @Size(max = 32)
    @Column(name = "class_code", nullable = false, unique = true, length = 32)
    private String classCode;

    /** Tên hiển thị thân thiện (không dùng làm khoá). */
    @NotBlank
    @Column(nullable = false)
    private String name;

    /** Bộ môn: Gym, Yoga, Boxing... */
    private String subject;

    private String description;

    @NotNull
    @Positive
    @Column(nullable = false)
    private Integer capacity;

    /** Coach phụ trách lớp (id trong bảng users, role = COACH). */
    private Long coachId;

    public GymClass() {
    }

    public GymClass(
            String classCode,
            String name,
            String subject,
            String description,
            Integer capacity,
            Long coachId) {
        this.classCode = classCode;
        this.name = name;
        this.subject = subject;
        this.description = description;
        this.capacity = capacity;
        this.coachId = coachId;
    }

    // ── Backward-compat constructor (không có classCode, dùng trong test cũ) ──
    public GymClass(
            String name,
            String subject,
            String description,
            Integer capacity,
            Long coachId) {
        String base = (name != null && !name.isBlank()) ? name.replaceAll("\\s+", "-").toUpperCase() : "CLS";
        if (base.length() > 20) {
            base = base.substring(0, 20);
        }
        this.classCode = (base + "-" + java.util.UUID.randomUUID().toString().substring(0, 8)).toUpperCase();
        this.name = name;
        this.subject = subject;
        this.description = description;
        this.capacity = capacity;
        this.coachId = coachId;
    }

    // ── Getters / Setters ──────────────────────────────────────────────────

    public Long getId() {
        return id;
    }

    public String getClassCode() {
        return classCode;
    }

    public void setClassCode(String classCode) {
        this.classCode = classCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public Long getCoachId() {
        return coachId;
    }

    public void setCoachId(Long coachId) {
        this.coachId = coachId;
    }
}
