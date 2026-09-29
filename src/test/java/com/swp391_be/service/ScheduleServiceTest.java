package com.swp391_be.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.swp391_be.entity.Schedule;
import com.swp391_be.exception.BusinessException;
import com.swp391_be.repository.ClassEnrollmentRepository;
import com.swp391_be.repository.ScheduleRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ScheduleServiceTest {

    @Mock
    private ScheduleRepository schedules;

    @Mock
    private GymClassService classes;

    @Mock
    private RoomService rooms;

    @Mock
    private ClassEnrollmentRepository enrollments;

    private ScheduleService service;

    @BeforeEach
    void setUp() {
        service = new ScheduleService(schedules, classes, rooms, enrollments);
    }

    @Test
    void cannotDeleteScheduleWithEnrollments() {
        Schedule schedule = new Schedule(1L, 2L, 3L,
                LocalDateTime.of(2026, 10, 1, 9, 0),
                LocalDateTime.of(2026, 10, 1, 10, 0));
        when(schedules.findById(10L)).thenReturn(Optional.of(schedule));
        when(enrollments.existsByScheduleId(10L)).thenReturn(true);

        assertThatThrownBy(() -> service.delete(10L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Schedule has enrollments");
        verify(schedules, never()).delete(schedule);
    }
}
