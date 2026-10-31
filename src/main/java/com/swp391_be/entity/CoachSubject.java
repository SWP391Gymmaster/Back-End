package com.swp391_be.entity;

import jakarta.persistence.*;

/** Bộ môn mà một Coach được phép dạy (mỗi dòng là một cặp coach - bộ môn). */
@Entity
@Table(name = "coach_subjects", uniqueConstraints = @UniqueConstraint(
        name = "uk_coach_subject", columnNames = {"coach_id", "subject_key"}))
public class CoachSubject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "coach_id", nullable = false)
    private Long coachId;

    /** Tên hiển thị, ví dụ "Group X". */
    @Column(nullable = false, length = 40)
    private String subject;

    /** Dạng chuẩn hóa dùng để so sánh, ví dụ "GROUP_X". */
    @Column(name = "subject_key", nullable = false, length = 40)
    private String subjectKey;

    protected CoachSubject() {
    }

    public CoachSubject(Long coachId, String subject, String subjectKey) {
        this.coachId = coachId;
        this.subject = subject;
        this.subjectKey = subjectKey;
    }

    public Long getId() {
        return id;
    }

    public Long getCoachId() {
        return coachId;
    }

    public String getSubject() {
        return subject;
    }

    public String getSubjectKey() {
        return subjectKey;
    }
}
