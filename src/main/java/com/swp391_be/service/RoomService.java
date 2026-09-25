package com.swp391_be.service;

import com.swp391_be.entity.Room;
import com.swp391_be.exception.BusinessException;
import com.swp391_be.exception.ResourceNotFoundException;
import com.swp391_be.repository.RoomRepository;
import com.swp391_be.repository.ScheduleRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class RoomService {

    private final RoomRepository roomRepository;
    private final ScheduleRepository scheduleRepository;

    public RoomService(RoomRepository roomRepository, ScheduleRepository scheduleRepository) {
        this.roomRepository = roomRepository;
        this.scheduleRepository = scheduleRepository;
    }

    public List<Room> getAll() {
        return roomRepository.findAll();
    }

    public Room getById(Long id) {
        return roomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found"));
    }

    public Room create(Room room) {
        return roomRepository.save(room);
    }

    public Room update(Long id, Room room) {
        Room existing = getById(id);
        existing.setName(room.getName());
        existing.setCapacity(room.getCapacity());
        return roomRepository.save(existing);
    }

    public void delete(Long id) {
        Room room = getById(id);
        if (scheduleRepository.existsByRoomId(id)) {
            throw new BusinessException("Room is used by existing schedules");
        }
        roomRepository.delete(room);
    }
}
