package com.swp391_be.repository;

import com.swp391_be.entity.GymClass;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GymClassRepository extends JpaRepository<GymClass, Long> {

    boolean existsByCoachId(Long coachId);

    /** T5.4 — Tra cứu theo mã lớp (thay vì tên). */
    Optional<GymClass> findByClassCode(String classCode);

    /** T5.4 — Kiểm tra trùng mã lớp khi tạo mới. */
    boolean existsByClassCode(String classCode);

    /** T5.4 — Kiểm tra trùng mã lớp ngoại trừ chính lớp đang sửa. */
    boolean existsByClassCodeIgnoreCaseAndIdNot(String classCode, Long id);
}
