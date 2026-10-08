package com.swp391_be.service;

import com.swp391_be.entity.MemberMembership;
import com.swp391_be.exception.BusinessException;
import com.swp391_be.repository.MemberMembershipRepository;
import java.time.Clock;
import java.time.LocalDate;
import org.springframework.stereotype.Service;

@Service
public class MemberMembershipService {

    private final MemberMembershipRepository memberships;
    private final Clock clock;

    public MemberMembershipService(MemberMembershipRepository memberships, Clock clock) {
        this.memberships = memberships;
        this.clock = clock;
    }

    public void requireValidForSession(Long memberId, LocalDate sessionDate) {
        LocalDate today = LocalDate.now(clock);
        if (memberships.findCoveringMemberships(memberId, today, sessionDate).isEmpty()) {
            throw new BusinessException("Membership package is expired or does not cover this class session");
        }
    }

    @org.springframework.transaction.annotation.Transactional
    public MemberMembership activateMembership(Long memberId, Long packageId, int durationDays) {
        if (durationDays <= 0) {
            throw new BusinessException("Package duration must be greater than zero");
        }
        LocalDate today = LocalDate.now(clock);
        java.util.List<MemberMembership> existing = memberships.findByMemberIdOrderByEndDateDesc(memberId);

        LocalDate startDate = today;
        java.util.Optional<LocalDate> latestEndDate = existing.stream()
                .map(MemberMembership::getEndDate)
                .filter(endDate -> !endDate.isBefore(today))
                .max(LocalDate::compareTo);

        if (latestEndDate.isPresent()) {
            startDate = latestEndDate.get().plusDays(1);
        }

        LocalDate endDate = startDate.plusDays(durationDays - 1);
        MemberMembership newMembership = new MemberMembership(memberId, packageId, startDate, endDate);
        return memberships.save(newMembership);
    }

    public java.util.List<MemberMembership> getMembershipsByMemberId(Long memberId) {
        return memberships.findByMemberIdOrderByEndDateDesc(memberId);
    }
}
