package com.swp391_be.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.swp391_be.dto.RoomResponse;
import com.swp391_be.entity.Room;
import com.swp391_be.entity.RoomType;
import com.swp391_be.repository.RoomRepository;
import com.swp391_be.repository.ScheduleRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class RoomServiceTest {

    @Mock
    private RoomRepository rooms;

    @Mock
    private ScheduleRepository schedules;

    @Test
    void getAllReportsClassCountAndZeroForEmptyRoom() {
        Room used = new Room("A", 10, RoomType.YOGA);
        Room empty = new Room("B", 10, RoomType.GYM);
        ReflectionTestUtils.setField(used, "id", 1L);
        ReflectionTestUtils.setField(empty, "id", 2L);
        when(rooms.findAll()).thenReturn(List.of(used, empty));
        when(schedules.countClassesPerRoom()).thenReturn(List.<Object[]>of(new Object[] {1L, 2L}));

        List<RoomResponse> result = new RoomService(rooms, schedules).getAll();

        assertEquals(2L, result.get(0).classCount());
        assertEquals(RoomType.YOGA, result.get(0).type());
        assertEquals(0L, result.get(1).classCount());
    }
}