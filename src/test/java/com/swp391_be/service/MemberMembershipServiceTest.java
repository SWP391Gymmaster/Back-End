package com.swp391_be.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.swp391_be.entity.MemberMembership;
import com.swp391_be.exception.BusinessException;
import com.swp391_be.repository.MemberMembershipRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MemberMembershipServiceTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final LocalDate TODAY = LocalDate.of(2026, 9, 29);
    private static final Long MEMBER_ID = 1L;

    @Mock
    private MemberMembershipRepository memberships;

    private final Clock clock = Clock.fixed(Instant.parse("2026-09-29T00:00:00Z"), ZONE);

    private MemberMembershipService service;

    @Test
    void acceptsPackageOnBothInclusiveBoundaries() {
        LocalDate sessionDate = LocalDate.of(2026, 10, 28);
        when(memberships.findCoveringMemberships(MEMBER_ID, TODAY, sessionDate))
                .thenReturn(List.of(new MemberMembership(MEMBER_ID, 10L, TODAY, sessionDate)));
        service = new MemberMembershipService(memberships, clock);

        assertThatCode(() -> service.requireValidForSession(MEMBER_ID, sessionDate))
                .doesNotThrowAnyException();
    }

    @Test
    void acceptsWhenAnyOverlappingPackageCoversSession() {
        LocalDate sessionDate = LocalDate.of(2026, 10, 5);
        when(memberships.findCoveringMemberships(MEMBER_ID, TODAY, sessionDate))
                .thenReturn(List.of(new MemberMembership(MEMBER_ID, 11L, TODAY.minusDays(5), sessionDate.plusDays(5))));
        service = new MemberMembershipService(memberships, clock);

        assertThatCode(() -> service.requireValidForSession(MEMBER_ID, sessionDate))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsExpiredPackage() {
        LocalDate sessionDate = LocalDate.of(2026, 9, 30);
        when(memberships.findCoveringMemberships(MEMBER_ID, TODAY, sessionDate)).thenReturn(List.of());
        service = new MemberMembershipService(memberships, clock);

        assertThatThrownBy(() -> service.requireValidForSession(MEMBER_ID, sessionDate))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Membership package is expired or does not cover this class session");
    }

    @Test
    void rejectsPackageThatIsActiveTodayButEndsBeforeSession() {
        LocalDate sessionDate = LocalDate.of(2026, 10, 2);
        when(memberships.findCoveringMemberships(MEMBER_ID, TODAY, sessionDate)).thenReturn(List.of());
        service = new MemberMembershipService(memberships, clock);

        assertThatThrownBy(() -> service.requireValidForSession(MEMBER_ID, sessionDate))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Membership package is expired or does not cover this class session");
    }

    @Test
    void rejectsPackageThatStartsInFuture() {
        LocalDate sessionDate = LocalDate.of(2026, 10, 10);
        when(memberships.findCoveringMemberships(MEMBER_ID, TODAY, sessionDate)).thenReturn(List.of());
        service = new MemberMembershipService(memberships, clock);

        assertThatThrownBy(() -> service.requireValidForSession(MEMBER_ID, sessionDate))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Membership package is expired or does not cover this class session");
    }

    @Test
    void activatesNewMembershipStartingTodayWhenNoExistingMemberships() {
        when(memberships.findByMemberIdOrderByEndDateDesc(MEMBER_ID)).thenReturn(List.of());
        when(memberships.save(org.mockito.ArgumentMatchers.any(MemberMembership.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service = new MemberMembershipService(memberships, clock);
        MemberMembership result = service.activateMembership(MEMBER_ID, 10L, 30);

        org.assertj.core.api.Assertions.assertThat(result.getStartDate()).isEqualTo(TODAY);
        org.assertj.core.api.Assertions.assertThat(result.getEndDate()).isEqualTo(TODAY.plusDays(29));
        org.assertj.core.api.Assertions.assertThat(result.getPackageId()).isEqualTo(10L);
    }

    @Test
    void activatesNewMembershipExtendingFromActiveMembershipEndDate() {
        LocalDate existingEndDate = TODAY.plusDays(10);
        MemberMembership currentMembership = new MemberMembership(MEMBER_ID, 5L, TODAY.minusDays(10), existingEndDate);
        when(memberships.findByMemberIdOrderByEndDateDesc(MEMBER_ID)).thenReturn(List.of(currentMembership));
        when(memberships.save(org.mockito.ArgumentMatchers.any(MemberMembership.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service = new MemberMembershipService(memberships, clock);
        MemberMembership result = service.activateMembership(MEMBER_ID, 10L, 30);

        org.assertj.core.api.Assertions.assertThat(result.getStartDate()).isEqualTo(existingEndDate.plusDays(1));
        org.assertj.core.api.Assertions.assertThat(result.getEndDate()).isEqualTo(existingEndDate.plusDays(30));
    }

    @Test
    void rejectsZeroOrNegativeDurationDays() {
        service = new MemberMembershipService(memberships, clock);

        assertThatThrownBy(() -> service.activateMembership(MEMBER_ID, 10L, 0))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Package duration must be greater than zero");
    }
}
