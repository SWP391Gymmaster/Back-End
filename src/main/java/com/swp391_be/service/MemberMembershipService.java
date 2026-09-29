package com.swp391_be.service;

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
}
