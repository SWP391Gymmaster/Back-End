package com.swp391_be.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

// Đổi tên từ "Class" để không trùng với java.lang.Class. Tên bảng vẫn là "classes".
@Entity
@Table(name = "classes")
public class GymClass {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String name;

    // Bộ môn: Gym, Yoga, Boxing...
    private String subject;

    private String description;

    @Positive
    private Integer capacity;

    // Coach phụ trách lớp (id trong bảng users, role = COACH)
    private Long coachId;

    public GymClass() {
    }

    public GymClass(
            String name,
            String subject,
            String description,
            Integer capacity,
            Long coachId) {
        this.name = name;
        this.subject = subject;
        this.description = description;
        this.capacity = capacity;
        this.coachId = coachId;
    }

    public Long getId() {
        return id;
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
