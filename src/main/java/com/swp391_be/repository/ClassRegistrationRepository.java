package com.swp391_be.repository;

import com.swp391_be.entity.ClassRegistration;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ClassRegistrationRepository extends JpaRepository<ClassRegistration, Long> {

    boolean existsByMemberIdAndClassId(Long memberId, Long classId);

    Optional<ClassRegistration> findByMemberIdAndClassId(Long memberId, Long classId);

    List<ClassRegistration> findByMemberId(Long memberId);

    long countByClassId(Long classId);

    boolean existsByClassId(Long classId);

    // [classId, số hội viên đã đăng ký lớp]
    @Query("select r.classId, count(r) from ClassRegistration r group by r.classId")
    List<Object[]> countRegistrationsPerClass();
}