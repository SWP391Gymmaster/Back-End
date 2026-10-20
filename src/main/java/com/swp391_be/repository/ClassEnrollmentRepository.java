package com.swp391_be.repository;

import com.swp391_be.entity.ClassEnrollment;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClassEnrollmentRepository extends JpaRepository<ClassEnrollment, Long> {
    boolean existsByMemberId(Long memberId);

    long countByScheduleId(Long scheduleId);

    boolean existsByMemberIdAndScheduleId(Long memberId, Long scheduleId);

    Optional<ClassEnrollment> findByMemberIdAndScheduleId(Long memberId, Long scheduleId);

    List<ClassEnrollment> findByMemberIdOrderByEnrolledAtDesc(Long memberId);

    boolean existsByScheduleId(Long scheduleId);
}
