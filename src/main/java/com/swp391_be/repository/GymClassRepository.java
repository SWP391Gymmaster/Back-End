package com.swp391_be.repository;

import com.swp391_be.entity.GymClass;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface GymClassRepository extends JpaRepository<GymClass, Long> {

    boolean existsByCoachId(Long coachId);

    /** T5.4 — Tra cứu theo mã lớp (thay vì tên). */
    Optional<GymClass> findByClassCode(String classCode);

    /** T5.4 — Kiểm tra trùng mã lớp khi tạo mới. */
    boolean existsByClassCode(String classCode);

    /** T5.4 — Kiểm tra trùng mã lớp ngoại trừ chính lớp đang sửa. */
    boolean existsByClassCodeIgnoreCaseAndIdNot(String classCode, Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from GymClass c where c.id = :id")
    Optional<GymClass> findByIdForUpdate(@Param("id") Long id);

    List<GymClass> findByCoachId(Long coachId);
}
