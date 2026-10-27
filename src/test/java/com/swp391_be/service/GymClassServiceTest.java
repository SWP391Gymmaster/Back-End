package com.swp391_be.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.swp391_be.entity.GymClass;
import com.swp391_be.entity.Role;
import com.swp391_be.entity.Schedule;
import com.swp391_be.exception.BusinessException;
import com.swp391_be.repository.ClassEnrollmentRepository;
import com.swp391_be.repository.GymClassRepository;
import com.swp391_be.repository.ScheduleRepository;
import com.swp391_be.repository.UserRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
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

    private GymClassService service;

    @BeforeEach
    void setUp() {
        service = new GymClassService(classes, schedules, users, enrollments);
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
    void cannotCreateClassWhenEndDateIsBeforeStartDate() {
        GymClass gymClass = new GymClass("Yoga", "Yoga", "Desc", 5, 3L);
        gymClass.setStartDate(LocalDate.of(2026, 11, 10));
        gymClass.setEndDate(LocalDate.of(2026, 11, 9));

        assertThatThrownBy(() -> service.create(gymClass))
                .isInstanceOf(BusinessException.class)
                .hasMessage("End date must not be before start date");
        verify(classes, never()).save(gymClass);
    }

    @Test
    void createClassAllowsSameDayOrMissingDates() {
        GymClass sameDay = new GymClass("Yoga", "Yoga", "Desc", 5, 3L);
        sameDay.setStartDate(LocalDate.of(2026, 11, 10));
        sameDay.setEndDate(LocalDate.of(2026, 11, 10));
        GymClass noDates = new GymClass("Boxing", "Boxing", "Desc", 5, 3L);
        when(users.existsByIdAndRole(3L, Role.COACH)).thenReturn(true);
        when(classes.save(sameDay)).thenReturn(sameDay);
        when(classes.save(noDates)).thenReturn(noDates);

        assertThat(service.create(sameDay).getEndDate()).isEqualTo(LocalDate.of(2026, 11, 10));
        assertThat(service.create(noDates).getStartDate()).isNull();
    }

    @Test
    void updateCopiesClassAndRegistrationDates() {
        GymClass existing = new GymClass("Yoga", "Yoga", "Desc", 5, 3L);
        GymClass update = new GymClass("Yoga", "Yoga", "Desc", 5, 3L);
        update.setStartDate(LocalDate.of(2026, 11, 1));
        update.setEndDate(LocalDate.of(2026, 12, 31));
        update.setRegistrationOpenAt(LocalDate.of(2026, 10, 1));
        update.setRegistrationCloseAt(LocalDate.of(2026, 10, 15));
        when(classes.findById(20L)).thenReturn(Optional.of(existing));
        when(users.existsByIdAndRole(3L, Role.COACH)).thenReturn(true);
        when(classes.save(existing)).thenReturn(existing);

        GymClass saved = service.update(20L, update);

        assertThat(saved.getStartDate()).isEqualTo(LocalDate.of(2026, 11, 1));
        assertThat(saved.getEndDate()).isEqualTo(LocalDate.of(2026, 12, 31));
        assertThat(saved.getRegistrationOpenAt()).isEqualTo(LocalDate.of(2026, 10, 1));
        assertThat(saved.getRegistrationCloseAt()).isEqualTo(LocalDate.of(2026, 10, 15));
    }

    @Test
    void rejectsRegistrationCloseBeforeOpen() {
        GymClass gymClass = new GymClass("Yoga", "Yoga", "Desc", 5, 3L);
        gymClass.setRegistrationOpenAt(LocalDate.of(2026, 10, 2));
        gymClass.setRegistrationCloseAt(LocalDate.of(2026, 10, 1));

        assertThatThrownBy(() -> service.create(gymClass))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Registration close date must not be before registration open date");
        verify(classes, never()).save(gymClass);
    }

    @Test
    void cannotUpdateClassWhenEndDateIsBeforeStartDate() {
        GymClass existing = new GymClass("Yoga", "Yoga", "Desc", 5, 3L);
        GymClass update = new GymClass("Yoga", "Yoga", "Desc", 5, 3L);
        update.setStartDate(LocalDate.of(2026, 12, 1));
        update.setEndDate(LocalDate.of(2026, 11, 1));
        when(classes.findById(20L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.update(20L, update))
                .isInstanceOf(BusinessException.class)
                .hasMessage("End date must not be before start date");
        verify(classes, never()).save(existing);
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
}
