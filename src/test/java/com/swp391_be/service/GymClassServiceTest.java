package com.swp391_be.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.swp391_be.entity.GymClass;
import com.swp391_be.entity.Role;
import com.swp391_be.entity.Schedule;
import com.swp391_be.exception.BusinessException;
import com.swp391_be.repository.ClassEnrollmentRepository;
import com.swp391_be.repository.ClassRegistrationRepository;
import com.swp391_be.repository.CoachSubjectRepository;
import com.swp391_be.repository.GymClassRepository;
import com.swp391_be.repository.ScheduleRepository;
import com.swp391_be.repository.UserRepository;
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
class GymClassServiceTest {

    @Mock
    private GymClassRepository classes;

    @Mock
    private ScheduleRepository schedules;

    @Mock
    private UserRepository users;

    @Mock
    private ClassEnrollmentRepository enrollments;

    @Mock
    private ClassRegistrationRepository registrations;

    private GymClassService service;

    @Mock
    private CoachSubjectRepository coachSubjects;

    @BeforeEach
    void setUp() {
        service = new GymClassService(classes, schedules, users, enrollments, registrations, coachSubjects,
                Clock.fixed(Instant.parse("2026-09-29T00:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh")));
        lenient().when(coachSubjects.existsByCoachIdAndSubjectKey(any(), any())).thenReturn(true);
    }

    @Test
    void cannotDeleteClassWhoseSchedulesHaveEnrollments() {
        GymClass gymClass = new GymClass("Yoga", "Yoga", "Desc", 5, 3L);
        Schedule schedule = schedule(10L);
        when(classes.findById(20L)).thenReturn(Optional.of(gymClass));
        when(schedules.findByClassIdOrderByStartTime(20L)).thenReturn(List.of(schedule));
        when(enrollments.existsByScheduleId(10L)).thenReturn(true);

        assertThatThrownBy(() -> service.delete(20L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Class has schedules with enrollments");
        verify(classes, never()).delete(gymClass);
    }

    @Test
    void cannotReduceClassCapacityBelowLargestScheduledEnrollmentCount() {
        GymClass existing = new GymClass("Yoga", "Yoga", "Desc", 5, 3L);
        GymClass update = new GymClass("Yoga", "Yoga", "Desc", 1, 3L);
        Schedule schedule = schedule(10L);
        when(classes.findById(20L)).thenReturn(Optional.of(existing));
        when(users.existsByIdAndRole(3L, Role.COACH)).thenReturn(true);
        when(schedules.findByClassIdOrderByStartTime(20L)).thenReturn(List.of(schedule));
        when(enrollments.countByScheduleId(10L)).thenReturn(2L);

        assertThatThrownBy(() -> service.update(20L, update))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Capacity cannot be lower than current enrollment count");
        verify(classes, never()).save(existing);
    }

    @Test
    void rejectsStartDateTodayOrEarlierOnCreate() {
        GymClass gymClass = new GymClass("Yoga", "Yoga", "Desc", 5, 3L);
        gymClass.setStartDate(LocalDate.of(2026, 9, 29)); // hôm nay, phải từ ngày mai

        assertThatThrownBy(() -> service.create(gymClass))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Start date must be from tomorrow");
        verify(classes, never()).save(gymClass);
    }

    @Test
    void rejectsEarlyRegistrationInThePastOrClosingAfterStart() {
        GymClass past = new GymClass("Yoga", "Yoga", "Desc", 5, 3L);
        past.setRegistrationOpenAt(LocalDateTime.of(2026, 9, 29, 6, 0)); // 07:00 là hiện tại
        past.setRegistrationCloseAt(LocalDateTime.of(2026, 9, 29, 23, 0));
        assertThatThrownBy(() -> service.create(past))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Registration open time must not be in the past");

        GymClass late = new GymClass("Yoga", "Yoga", "Desc", 5, 3L);
        late.setStartDate(LocalDate.of(2026, 9, 30));
        late.setRegistrationOpenAt(LocalDateTime.of(2026, 9, 29, 11, 0));
        late.setRegistrationCloseAt(LocalDateTime.of(2026, 9, 30, 8, 0)); // sau 00:00 ngày bắt đầu
        assertThatThrownBy(() -> service.create(late))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Registration must close before the class start date");
    }

    @Test
    void updateKeepsPastStartDateWhenUnchanged() {
        GymClass existing = new GymClass("Yoga", "Yoga", "Desc", 5, 3L);
        existing.setStartDate(LocalDate.of(2026, 9, 1));
        GymClass update = new GymClass("Yoga 2", "Yoga", "Desc", 5, 3L);
        update.setStartDate(LocalDate.of(2026, 9, 1)); // không đổi nên không bị coi là quá khứ
        when(classes.findById(20L)).thenReturn(Optional.of(existing));
        when(users.existsByIdAndRole(3L, Role.COACH)).thenReturn(true);
        when(classes.save(existing)).thenReturn(existing);

        assertThat(service.update(20L, update).getName()).isEqualTo("Yoga 2");
    }

    private Schedule schedule(Long id) {
        Schedule schedule = new Schedule(20L, 3L, 4L,
                LocalDateTime.of(2026, 10, 1, 9, 0),
                LocalDateTime.of(2026, 10, 1, 10, 0));
        try {
            java.lang.reflect.Field field = Schedule.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(schedule, id);
            return schedule;
        } catch (ReflectiveOperationException ex) {
            throw new AssertionError(ex);
        }
    }

    @Test
    void rejectsCoachWhoDoesNotTeachSubject() {
        GymClass gymClass = new GymClass("Yoga", "Yoga", "Desc", 5, 3L);
        when(users.existsByIdAndRole(3L, Role.COACH)).thenReturn(true);
        when(coachSubjects.existsByCoachIdAndSubjectKey(3L, "YOGA")).thenReturn(false);

        assertThatThrownBy(() -> service.create(gymClass))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Coach does not teach this subject");
        verify(classes, never()).save(gymClass);
    }

    @Test
    void rejectsSixthOverlappingClassForSameCoach() {
        GymClass gymClass = classWithDates("New", LocalDate.of(2026, 11, 1), LocalDate.of(2027, 1, 31));
        when(users.existsByIdAndRole(3L, Role.COACH)).thenReturn(true);
        when(classes.findByCoachId(3L)).thenReturn(List.of(
                classWithDates("A", LocalDate.of(2026, 10, 1), LocalDate.of(2026, 12, 31)),
                classWithDates("B", LocalDate.of(2026, 10, 5), LocalDate.of(2026, 12, 31)),
                classWithDates("C", LocalDate.of(2026, 10, 10), LocalDate.of(2026, 12, 31)),
                classWithDates("D", LocalDate.of(2026, 10, 15), LocalDate.of(2026, 12, 31)),
                classWithDates("E", LocalDate.of(2026, 10, 20), LocalDate.of(2026, 12, 31))));

        assertThatThrownBy(() -> service.create(gymClass))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Coach already has the maximum number of classes in this period");
        verify(classes, never()).save(gymClass);
    }

    @Test
    void closedClassesDoNotCountTowardsCoachLimit() {
        GymClass gymClass = classWithDates("New", LocalDate.of(2026, 11, 1), LocalDate.of(2027, 1, 31));
        when(users.existsByIdAndRole(3L, Role.COACH)).thenReturn(true);
        when(classes.findByCoachId(3L)).thenReturn(List.of(
                classWithDates("A", LocalDate.of(2026, 10, 1), LocalDate.of(2026, 12, 31)),
                classWithDates("B", LocalDate.of(2026, 10, 5), LocalDate.of(2026, 12, 31)),
                classWithDates("C", LocalDate.of(2026, 10, 10), LocalDate.of(2026, 12, 31)),
                classWithDates("D", LocalDate.of(2026, 10, 15), LocalDate.of(2026, 12, 31)),
                classWithDates("Old", LocalDate.of(2026, 8, 1), LocalDate.of(2026, 9, 1)), // đã đóng
                classWithDates("Later", LocalDate.of(2027, 3, 1), LocalDate.of(2027, 5, 1)))); // không giao ngày
        when(classes.save(gymClass)).thenReturn(gymClass);

        assertThat(service.create(gymClass)).isSameAs(gymClass);
    }

    @Test
    void rejectsSameCoachWithSameStartAndEndDates() {
        GymClass gymClass = classWithDates("New", LocalDate.of(2026, 11, 1), LocalDate.of(2026, 12, 31));
        when(users.existsByIdAndRole(3L, Role.COACH)).thenReturn(true);
        when(classes.findByCoachId(3L)).thenReturn(List.of(
                classWithDates("A", LocalDate.of(2026, 11, 1), LocalDate.of(2026, 12, 31))));

        assertThatThrownBy(() -> service.create(gymClass))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Coach already has a class with the same start and end dates");
    }

    @Test
    void reopeningClosedClassClearsOldRegistrations() {
        GymClass existing = classWithDates("Yoga", LocalDate.of(2026, 8, 1), LocalDate.of(2026, 9, 1)); // đã kết thúc
        GymClass update = classWithDates("Yoga", LocalDate.of(2026, 10, 10), LocalDate.of(2026, 11, 10));
        when(classes.findById(20L)).thenReturn(Optional.of(existing));
        when(users.existsByIdAndRole(3L, Role.COACH)).thenReturn(true);
        when(classes.save(existing)).thenReturn(existing);

        service.update(20L, update);

        verify(registrations).deleteAllByClassId(20L);
        assertThat(existing.getStartDate()).isEqualTo(LocalDate.of(2026, 10, 10));
    }

    @Test
    void editingRunningClassKeepsRegistrations() {
        GymClass existing = classWithDates("Yoga", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 12, 1));
        GymClass update = classWithDates("Yoga 2", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 12, 1));
        when(classes.findById(20L)).thenReturn(Optional.of(existing));
        when(users.existsByIdAndRole(3L, Role.COACH)).thenReturn(true);
        when(classes.save(existing)).thenReturn(existing);

        service.update(20L, update);

        verify(registrations, never()).deleteAllByClassId(any());
    }

    private static GymClass classWithDates(String name, LocalDate start, LocalDate end) {
        GymClass gymClass = new GymClass(name, "Yoga", "Desc", 5, 3L);
        gymClass.setStartDate(start);
        gymClass.setEndDate(end);
        return gymClass;
    }
}