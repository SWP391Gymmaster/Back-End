package com.swp391_be.repository;

import com.swp391_be.entity.GymClass;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GymClassRepository extends JpaRepository<GymClass, Long> {
    boolean existsByCoachId(Long coachId);
}
