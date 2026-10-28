package com.swp391_be.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.swp391_be.dto.AvailableClassSessionResponse;
import com.swp391_be.dto.ClassEnrollmentResponse;
import com.swp391_be.entity.ClassEnrollment;
import com.swp391_be.entity.GymClass;
import com.swp391_be.entity.Schedule;
import com.swp391_be.exception.BusinessException;
import com.swp391_be.exception.ResourceNotFoundException;
import com.swp391_be.repository.ClassEnrollmentRepository;
import com.swp391_be.repository.ClassRegistrationRepository;
import com.swp391_be.repository.GymClassRepository;
import com.swp391_be.repository.ScheduleRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ClassEnrollmentServiceTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final Instant NOW_INSTANT = Instant.parse("2026-09-29T00:00:00Z");
    private static final Long MEMBER_ID = 1L;
    private static final Long SCHEDULE_ID = 10L;
    private static final Long CLASS_ID = 20L;

    @Mock
    private ScheduleRepository schedules;

    @Mock
    private GymClassRepository classes;

    @Mock
    private ClassEnrollmentRepository enrollments;

    @Mock
    private ClassRegistrationRepository registrations;

    @Mock
    private MemberMembershipService membershipService;

    private ClassEnrollmentService service;

    @BeforeEach
    void setUp() {
        service = new ClassEnrollmentService(
                schedules,
                classes,
                enrollments,
                registrations,
                membershipService,
                Clock.fixed(NOW_INSTANT, ZONE));
        lenient().when(registrations.existsByMemberIdAndClassId(any(), any())).thenReturn(true);
    }

    @Test
    void enrollsWhenMembershipValidAndSeatAvailable() {
        Schedule schedule = futureSchedule(SCHEDULE_ID, CLASS_ID);
        GymClass gymClass = gymClass(2);
        when(schedules.findByIdForUpdate(SCHEDULE_ID)).thenReturn(Optional.of(schedule));
        when(classes.findById(CLASS_ID)).thenReturn(Optional.of(gymClass));
        when(enrollments.existsByMemberIdAndScheduleId(MEMBER_ID, SCHEDULE_ID)).thenReturn(false);
        when(enrollments.countByScheduleId(SCHEDULE_ID)).thenReturn(1L);
        when(enrollments.saveAndFlush(any(ClassEnrollment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ClassEnrollmentResponse response = service.enroll(MEMBER_ID, SCHEDULE_ID);

        assertThat(response.scheduleId()).isEqualTo(SCHEDULE_ID);
        assertThat(response.classId()).isEqualTo(CLASS_ID);
        assertThat(response.className()).isEqualTo("Yoga");
        assertThat(response.enrolledAt()).isEqualTo(NOW_INSTANT);
    }

    @Test
    void rejectsWhenMembershipExpired() {
        Schedule schedule = futureSchedule(SCHEDULE_ID, CLASS_ID);
        when(schedules.findByIdForUpdate(SCHEDULE_ID)).thenReturn(Optional.of(schedule));
        when(classes.findById(CLASS_ID)).thenReturn(Optional.of(gymClass(2)));
        org.mockito.Mockito
                .doThrow(new BusinessException("Membership package is expired or does not cover this class session"))
                .when(membershipService)
                .requireValidForSession(MEMBER_ID, schedule.getStartTime().toLocalDate());

        assertThatThrownBy(() -> service.enroll(MEMBER_ID, SCHEDULE_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Membership package is expired or does not cover this class session");
        verify(enrollments, never()).saveAndFlush(any());
    }

    @Test
    void rejectsWhenClassIsFull() {
        Schedule schedule = futureSchedule(SCHEDULE_ID, CLASS_ID);
        when(schedules.findByIdForUpdate(SCHEDULE_ID)).thenReturn(Optional.of(schedule));
        when(classes.findById(CLASS_ID)).thenReturn(Optional.of(gymClass(2)));
        when(enrollments.existsByMemberIdAndScheduleId(MEMBER_ID, SCHEDULE_ID)).thenReturn(false);
        when(enrollments.countByScheduleId(SCHEDULE_ID)).thenReturn(2L);

        assertThatThrownBy(() -> service.enroll(MEMBER_ID, SCHEDULE_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Class session is full");
        verify(enrollments, never()).saveAndFlush(any());
    }

    @Test
    void rejectsDuplicateEnrollment() {
        Schedule schedule = futureSchedule(SCHEDULE_ID, CLASS_ID);
        when(schedules.findByIdForUpdate(SCHEDULE_ID)).thenReturn(Optional.of(schedule));
        when(classes.findById(CLASS_ID)).thenReturn(Optional.of(gymClass(2)));
        when(enrollments.existsByMemberIdAndScheduleId(MEMBER_ID, SCHEDULE_ID)).thenReturn(true);

        assertThatThrownBy(() -> service.enroll(MEMBER_ID, SCHEDULE_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Already enrolled in this class session");
        verify(enrollments, never()).saveAndFlush(any());
    }

    @Test
    void rejectsSessionEnrollmentDuringEarlyRegistration() {
        GymClass gymClass = gymClass(2);
        gymClass.setRegistrationOpenAt(LocalDate.of(2026, 9, 28).atStartOfDay());
        gymClass.setRegistrationCloseAt(LocalDate.of(2026, 9, 29).atTime(23, 0)); // hiện tại 29/09 07:00, chưa đóng
        when(schedules.findByIdForUpdate(SCHEDULE_ID)).thenReturn(Optional.of(futureSchedule(SCHEDULE_ID, CLASS_ID)));
        when(classes.findById(CLASS_ID)).thenReturn(Optional.of(gymClass));

        assertThatThrownBy(() -> service.enroll(MEMBER_ID, SCHEDULE_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Class session registration is not open yet");
        verify(enrollments, never()).saveAndFlush(any());
    }

    @Test
    void rejectsWhenNotRegisteredInClass() {
        when(schedules.findByIdForUpdate(SCHEDULE_ID)).thenReturn(Optional.of(futureSchedule(SCHEDULE_ID, CLASS_ID)));
        when(classes.findById(CLASS_ID)).thenReturn(Optional.of(gymClass(2)));
        when(registrations.existsByMemberIdAndClassId(MEMBER_ID, CLASS_ID)).thenReturn(false);

        assertThatThrownBy(() -> service.enroll(MEMBER_ID, SCHEDULE_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Not registered in this class");
        verify(enrollments, never()).saveAndFlush(any());
    }

    @Test
    void rejectsPastOrStartedSession() {
        when(schedules.findByIdForUpdate(SCHEDULE_ID)).thenReturn(Optional.of(
                new Schedule(CLASS_ID, 3L, 4L,
                        LocalDateTime.of(2026, 9, 29, 6, 0),
                        LocalDateTime.of(2026, 9, 29, 7, 0))));

        assertThatThrownBy(() -> service.enroll(MEMBER_ID, SCHEDULE_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Class session has already started");
        verify(classes, never()).findById(any());
    }

    @Test
    void rejectsMissingScheduleOrClass() {
        when(schedules.findByIdForUpdate(SCHEDULE_ID)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.enroll(MEMBER_ID, SCHEDULE_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Schedule not found");

        Schedule schedule = futureSchedule(SCHEDULE_ID, CLASS_ID);
        when(schedules.findByIdForUpdate(SCHEDULE_ID)).thenReturn(Optional.of(schedule));
        when(classes.findById(CLASS_ID)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.enroll(MEMBER_ID, SCHEDULE_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Class not found");
    }

    @Test
    void availableListContainsOnlyFutureSessionsWithRemainingCapacity() {
        Schedule available = futureSchedule(10L, CLASS_ID);
        Schedule full = futureSchedule(11L, CLASS_ID);
        when(schedules.findByStartTimeAfterOrderByStartTime(any(LocalDateTime.class)))
                .thenReturn(List.of(available, full));
        when(classes.findById(CLASS_ID)).thenReturn(Optional.of(gymClass(2)));
        when(enrollments.countByScheduleId(10L)).thenReturn(1L);
        when(enrollments.countByScheduleId(11L)).thenReturn(2L);
        when(enrollments.existsByMemberIdAndScheduleId(MEMBER_ID, 10L)).thenReturn(false);

        List<AvailableClassSessionResponse> responses = service.getAvailableSessions(MEMBER_ID);

        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().scheduleId()).isEqualTo(10L);
        assertThat(responses.getFirst().remainingSlots()).isEqualTo(1L);
    }

    @Test
    void availableListReportsRegisteredCountRemainingSlotsAndRegisteredByMe() {
        Schedule schedule = futureSchedule(SCHEDULE_ID, CLASS_ID);
        when(schedules.findByStartTimeAfterOrderByStartTime(any(LocalDateTime.class))).thenReturn(List.of(schedule));
        when(classes.findById(CLASS_ID)).thenReturn(Optional.of(gymClass(5)));
        when(enrollments.countByScheduleId(SCHEDULE_ID)).thenReturn(2L);
        when(enrollments.existsByMemberIdAndScheduleId(MEMBER_ID, SCHEDULE_ID)).thenReturn(true);

        AvailableClassSessionResponse response = service.getAvailableSessions(MEMBER_ID).getFirst();

        assertThat(response.capacity()).isEqualTo(5);
        assertThat(response.registeredCount()).isEqualTo(2L);
        assertThat(response.remainingSlots()).isEqualTo(3L);
        assertThat(response.registeredByMe()).isTrue();
    }

    @Test
    void getMyEnrollmentsMapsCurrentMemberRegistrations() {
        ClassEnrollment enrollment = new ClassEnrollment(MEMBER_ID, SCHEDULE_ID, NOW_INSTANT);
        Schedule schedule = futureSchedule(SCHEDULE_ID, CLASS_ID);
        when(enrollments.findByMemberIdOrderByEnrolledAtDesc(MEMBER_ID)).thenReturn(List.of(enrollment));
        when(schedules.findById(SCHEDULE_ID)).thenReturn(Optional.of(schedule));
        when(classes.findById(CLASS_ID)).thenReturn(Optional.of(gymClass(2)));

        assertThatCode(() -> service.getMyEnrollments(MEMBER_ID)).doesNotThrowAnyException();
        assertThat(service.getMyEnrollments(MEMBER_ID).getFirst().scheduleId()).isEqualTo(SCHEDULE_ID);
    }

    @Test
    void cancelsOwnEnrollmentWhenMembershipStillValid() {
        Schedule schedule = futureSchedule(SCHEDULE_ID, CLASS_ID);
        ClassEnrollment enrollment = new ClassEnrollment(MEMBER_ID, SCHEDULE_ID, NOW_INSTANT);
        when(schedules.findByIdForUpdate(SCHEDULE_ID)).thenReturn(Optional.of(schedule));
        when(enrollments.findByMemberIdAndScheduleId(MEMBER_ID, SCHEDULE_ID)).thenReturn(Optional.of(enrollment));

        service.cancel(MEMBER_ID, SCHEDULE_ID);

        verify(enrollments).delete(enrollment);
    }

    @Test
    void rejectsCancellationWhenMembershipExpired() {
        Schedule schedule = futureSchedule(SCHEDULE_ID, CLASS_ID);
        when(schedules.findByIdForUpdate(SCHEDULE_ID)).thenReturn(Optional.of(schedule));
        org.mockito.Mockito
                .doThrow(new BusinessException("Membership package is expired or does not cover this class session"))
                .when(membershipService)
                .requireValidForSession(MEMBER_ID, schedule.getStartTime().toLocalDate());

        assertThatThrownBy(() -> service.cancel(MEMBER_ID, SCHEDULE_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Membership package is expired or does not cover this class session");
        verify(enrollments, never()).delete(any());
    }

    @Test
    void rejectsCancellationAfterSessionStarts() {
        when(schedules.findByIdForUpdate(SCHEDULE_ID)).thenReturn(Optional.of(
                new Schedule(CLASS_ID, 3L, 4L,
                        LocalDateTime.of(2026, 9, 29, 6, 0),
                        LocalDateTime.of(2026, 9, 29, 7, 0))));

        assertThatThrownBy(() -> service.cancel(MEMBER_ID, SCHEDULE_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Class session has already started");
        verify(enrollments, never()).delete(any());
    }

    @Test
    void rejectsCancellationWhenNotEnrolled() {
        Schedule schedule = futureSchedule(SCHEDULE_ID, CLASS_ID);
        when(schedules.findByIdForUpdate(SCHEDULE_ID)).thenReturn(Optional.of(schedule));
        when(enrollments.findByMemberIdAndScheduleId(MEMBER_ID, SCHEDULE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cancel(MEMBER_ID, SCHEDULE_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Enrollment not found");
    }

    @Test
    void cannotCancelAnotherMembersEnrollment() {
        Schedule schedule = futureSchedule(SCHEDULE_ID, CLASS_ID);
        when(schedules.findByIdForUpdate(SCHEDULE_ID)).thenReturn(Optional.of(schedule));
        when(enrollments.findByMemberIdAndScheduleId(MEMBER_ID, SCHEDULE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cancel(MEMBER_ID, SCHEDULE_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Enrollment not found");
        verify(enrollments).findByMemberIdAndScheduleId(MEMBER_ID, SCHEDULE_ID);
    }

    private Schedule futureSchedule(Long scheduleId, Long classId) {
        Schedule schedule = new Schedule(
                classId,
                3L,
                4L,
                LocalDateTime.of(2026, 10, 1, 9, 0),
                LocalDateTime.of(2026, 10, 1, 10, 0));
        setId(schedule, scheduleId);
        return schedule;
    }

    private GymClass gymClass(int capacity) {
        GymClass gymClass = new GymClass("Yoga", "Yoga", "Morning yoga", capacity, 3L);
        setId(gymClass, CLASS_ID);
        return gymClass;
    }

    private void setId(Object entity, Long id) {
        try {
            java.lang.reflect.Field field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (ReflectiveOperationException ex) {
            throw new AssertionError(ex);
        }
    }
}