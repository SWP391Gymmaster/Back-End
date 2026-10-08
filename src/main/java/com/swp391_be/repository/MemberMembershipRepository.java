package com.swp391_be.repository;

import com.swp391_be.entity.MemberMembership;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MemberMembershipRepository extends JpaRepository<MemberMembership, Long> {

    @Query("""
            select membership from MemberMembership membership
            where membership.memberId = :memberId
              and membership.startDate <= :today
              and membership.endDate >= :sessionDate
            """)
    List<MemberMembership> findCoveringMemberships(
            @Param("memberId") Long memberId,
            @Param("today") LocalDate today,
            @Param("sessionDate") LocalDate sessionDate);

    List<MemberMembership> findByMemberIdOrderByEndDateDesc(Long memberId);

    List<MemberMembership> findByMemberId(Long memberId);
}
