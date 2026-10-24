package com.swp391_be.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.swp391_be.dto.ScheduleResponse;
import com.swp391_be.entity.GymClass;
import com.swp391_be.entity.Room;
import com.swp391_be.entity.RoomType;
import com.swp391_be.entity.Schedule;
import com.swp391_be.exception.BusinessException;
import com.swp391_be.repository.ClassEnrollmentRepository;
import com.swp391_be.repository.ScheduleRepository;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

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

    @Test
    void getAllIncludesRegisteredCount() {
        Schedule s1 = new Schedule(1L, 2L, 3L,
                LocalDateTime.of(2026, 10, 1, 9, 0),
                LocalDateTime.of(2026, 10, 1, 10, 0));
        ReflectionTestUtils.setField(s1, "id", 101L);

        when(schedules.findAll()).thenReturn(List.of(s1));
        when(enrollments.countEnrollmentsPerSchedule()).thenReturn(List.<Object[]>of(new Object[] {101L, 5L}));

        List<ScheduleResponse> result = service.getAll();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).registeredCount()).isEqualTo(5L);
        assertThat(result.get(0).classId()).isEqualTo(1L);
        assertThat(result.get(0).roomId()).isEqualTo(3L);
    }

    @Test
    void getResponseByIdIncludesRegisteredCount() {
        Schedule s1 = new Schedule(1L, 2L, 3L,
                LocalDateTime.of(2026, 10, 1, 9, 0),
                LocalDateTime.of(2026, 10, 1, 10, 0));
        ReflectionTestUtils.setField(s1, "id", 101L);

        when(schedules.findById(101L)).thenReturn(Optional.of(s1));
        when(enrollments.countByScheduleId(101L)).thenReturn(7L);

        ScheduleResponse response = service.getResponseById(101L);
        assertThat(response.id()).isEqualTo(101L);
        assertThat(response.registeredCount()).isEqualTo(7L);
    }

    @Test
    void cannotCreateScheduleWhenCapacityExceedsRoomCapacity() {
        Schedule schedule = new Schedule(1L, 2L, 3L,
                LocalDateTime.of(2026, 10, 1, 9, 0),
                LocalDateTime.of(2026, 10, 1, 10, 0));
        GymClass gymClass = new GymClass("Yoga Basic", "Yoga", "Desc", 40, 2L);
        Room room = new Room("Room Yoga", 30, RoomType.YOGA);

        when(classes.getById(1L)).thenReturn(gymClass);
        when(rooms.getById(3L)).thenReturn(room);

        assertThatThrownBy(() -> service.create(schedule))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Class capacity exceeds room capacity");
    }

    @Test
    void cannotCreateScheduleWhenRoomTypeDoesNotMatchSubject() {
        Schedule schedule = new Schedule(1L, 2L, 3L,
                LocalDateTime.of(2026, 10, 1, 9, 0),
                LocalDateTime.of(2026, 10, 1, 10, 0));
        GymClass gymClass = new GymClass("Boxing 1", "Boxing", "Desc", 20, 2L);
        Room room = new Room("Room Yoga", 30, RoomType.YOGA);

        when(classes.getById(1L)).thenReturn(gymClass);
        when(rooms.getById(3L)).thenReturn(room);

        assertThatThrownBy(() -> service.create(schedule))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Room type does not match class subject");
    }

    @Test
    void cannotCreateScheduleWhenStartTimeNotBeforeEndTime() {
        Schedule schedule = new Schedule(1L, 2L, 3L,
                LocalDateTime.of(2026, 10, 1, 10, 0),
                LocalDateTime.of(2026, 10, 1, 9, 0));

        assertThatThrownBy(() -> service.create(schedule))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Start time must be before end time");
    }

    @Test
    void createScheduleSucceedsWhenValid() {
        Schedule schedule = new Schedule(1L, 2L, 3L,
                LocalDateTime.of(2026, 10, 1, 9, 0),
                LocalDateTime.of(2026, 10, 1, 10, 0));
        GymClass gymClass = new GymClass("Yoga Flow", "Yoga", "Desc", 20, 2L);
        Room room = new Room("Room Yoga", 30, RoomType.YOGA);

        when(classes.getById(1L)).thenReturn(gymClass);
        when(rooms.getById(3L)).thenReturn(room);
        when(schedules.findByCoachIdAndStartTimeLessThanAndEndTimeGreaterThan(any(), any(), any()))
                .thenReturn(Collections.emptyList());
        when(schedules.findByRoomIdAndStartTimeLessThanAndEndTimeGreaterThan(any(), any(), any()))
                .thenReturn(Collections.emptyList());
        when(schedules.save(schedule)).thenReturn(schedule);

        Schedule created = service.create(schedule);
        assertThat(created).isNotNull();
        verify(schedules).save(schedule);
    }

    @Test
    void createScheduleSupportsTableC3Mappings() {
        Schedule schedule = new Schedule(1L, 2L, 3L,
                LocalDateTime.of(2026, 10, 1, 9, 0),
                LocalDateTime.of(2026, 10, 1, 10, 0));
        when(schedules.findByCoachIdAndStartTimeLessThanAndEndTimeGreaterThan(any(), any(), any()))
                .thenReturn(Collections.emptyList());
        when(schedules.findByRoomIdAndStartTimeLessThanAndEndTimeGreaterThan(any(), any(), any()))
                .thenReturn(Collections.emptyList());
        when(schedules.save(schedule)).thenReturn(schedule);

        // 1. Gym allowed in CARDIO
        GymClass gymClass = new GymClass("Gym Strength", "Gym", "Desc", 20, 2L);
        Room cardioRoom = new Room("Room Cardio", 30, RoomType.CARDIO);
        when(classes.getById(1L)).thenReturn(gymClass);
        when(rooms.getById(3L)).thenReturn(cardioRoom);
        assertThat(service.create(schedule)).isNotNull();

        // 2. Cardio allowed in CYCLING
        GymClass cardioClass = new GymClass("Cardio Blast", "Cardio", "Desc", 20, 2L);
        Room cycleRoom = new Room("Room Cycle", 30, RoomType.CYCLING);
        when(classes.getById(1L)).thenReturn(cardioClass);
        when(rooms.getById(3L)).thenReturn(cycleRoom);
        assertThat(service.create(schedule)).isNotNull();

        // 3. Zumba allowed in GROUP_X
        GymClass zumbaClass = new GymClass("Zumba Party", "Zumba", "Desc", 20, 2L);
        Room gxRoom = new Room("Room GX", 30, RoomType.GROUP_X);
        when(classes.getById(1L)).thenReturn(zumbaClass);
        when(rooms.getById(3L)).thenReturn(gxRoom);
        assertThat(service.create(schedule)).isNotNull();

        // 4. Pilates allowed in YOGA and GROUP_X
        GymClass pilatesClass = new GymClass("Pilates Core", "Pilates", "Desc", 20, 2L);
        Room yogaRoom = new Room("Room Yoga", 30, RoomType.YOGA);
        when(classes.getById(1L)).thenReturn(pilatesClass);
        when(rooms.getById(3L)).thenReturn(yogaRoom);
        assertThat(service.create(schedule)).isNotNull();

        // 5. Any subject allowed in OTHER
        GymClass boxingClass = new GymClass("Boxing Pro", "Boxing", "Desc", 20, 2L);
        Room otherRoom = new Room("Room Other", 30, RoomType.OTHER);
        when(classes.getById(1L)).thenReturn(boxingClass);
        when(rooms.getById(3L)).thenReturn(otherRoom);
        assertThat(service.create(schedule)).isNotNull();
    }
}
