package com.swp391_be.service;

import com.swp391_be.entity.MemberMembership;
import com.swp391_be.entity.Role;
import com.swp391_be.entity.User;
import com.swp391_be.exception.BusinessException;
import com.swp391_be.repository.MemberMembershipRepository;
import com.swp391_be.repository.UserRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MemberMembershipService {

    private final MemberMembershipRepository memberships;
    private final Clock clock;
    private final UserRepository users;

    public MemberMembershipService(MemberMembershipRepository memberships, Clock clock) {
        this.memberships = memberships;
        this.clock = clock;
        this.users = null;
    }

    @Autowired
    public MemberMembershipService(MemberMembershipRepository memberships, Clock clock, UserRepository users) {
        this.memberships = memberships;
        this.clock = clock;
        this.users = users;
    }

    public void requireValidForSession(Long memberId, LocalDate sessionDate) {
        lockMember(memberId);
        LocalDate today = LocalDate.now(clock);
        if (memberships.findCoveringMemberships(memberId, today, sessionDate).isEmpty()) {
            throw new BusinessException("Membership package is expired or does not cover this class session");
        }
    }

    @Transactional
    public MemberMembership activateMembership(Long memberId, Long packageId, int durationDays) {
        lockMember(memberId);
        if (durationDays <= 0) {
            throw new BusinessException("Package duration must be greater than zero");
        }
        LocalDate today = LocalDate.now(clock);
        List<MemberMembership> existing = memberships.findByMemberIdOrderByEndDateDesc(memberId);

        LocalDate startDate = today;
        Optional<LocalDate> latestEndDate = existing.stream()
                .map(m -> m.getEndDate())
                .filter(endDate -> endDate != null && !endDate.isBefore(today))
                .max(Comparator.naturalOrder());

        if (latestEndDate.isPresent()) {
            startDate = latestEndDate.get().plusDays(1);
        }

        LocalDate endDate = startDate.plusDays(durationDays - 1);
        MemberMembership newMembership = new MemberMembership(memberId, packageId, startDate, endDate);
        return memberships.save(newMembership);
    }

    public List<MemberMembership> getMembershipsByMemberId(Long memberId) {
        return memberships.findByMemberIdOrderByEndDateDesc(memberId);
    }

    private void lockMember(Long memberId) {
        if (users == null) return;
        User member = users.findByIdForUpdate(memberId)
                .orElseThrow(() -> new BusinessException("Member not found"));
        if (member.getRole() != Role.MEMBER) throw new BusinessException("User is not a member");
    }
}
