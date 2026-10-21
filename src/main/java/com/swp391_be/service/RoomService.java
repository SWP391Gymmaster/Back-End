package com.swp391_be.service;

import com.swp391_be.dto.RoomResponse;
import com.swp391_be.entity.Room;
import com.swp391_be.entity.Schedule;
import com.swp391_be.exception.BusinessException;
import com.swp391_be.exception.ResourceNotFoundException;
import com.swp391_be.repository.RoomRepository;
import com.swp391_be.repository.ScheduleRepository;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service quản lý phòng tập.
 *
 * Ràng buộc T5.4:
 * - Mã phòng (roomCode) phải duy nhất — kiểm tra khi tạo/sửa.
 * - Kiểm tra trùng lịch phòng thực hiện qua ScheduleService/ScheduleRepository.
 * - Không xoá phòng đang có lịch.
 */
@Service
public class RoomService {

    private final RoomRepository roomRepository;
    private final ScheduleRepository scheduleRepository;

    public RoomService(RoomRepository roomRepository, ScheduleRepository scheduleRepository) {
        this.roomRepository = roomRepository;
        this.scheduleRepository = scheduleRepository;
    }

    // ── Queries ────────────────────────────────────────────────────────────

    public List<RoomResponse> getAll() {
        Map<Long, Long> counts = classCounts();
        return roomRepository.findAll().stream()
                .map(room -> RoomResponse.of(room, counts.getOrDefault(room.getId(), 0L)))
                .toList();
    }

    public RoomResponse getResponseById(Long id) {
        Room room = getById(id);
        return RoomResponse.of(room, classCounts().getOrDefault(id, 0L));
    }

    public List<Schedule> getSchedules(Long id) {
        getById(id);
        return scheduleRepository.findByRoomIdOrderByStartTime(id);
    }

    public Room getById(Long id) {
        return roomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found: id=" + id));
    }

    /** T5.4 — Tra cứu theo mã phòng. */
    public Room getByRoomCode(String roomCode) {
        return roomRepository.findByRoomCode(roomCode)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found: code=" + roomCode));
    }

    // ── Commands ───────────────────────────────────────────────────────────

    /**
     * Tạo phòng mới.
     * T5.4: kiểm tra roomCode không trùng.
     */
    @Transactional
    public Room create(Room room) {
        if (room.getRoomCode() == null || room.getRoomCode().isBlank()) {
            throw new BusinessException("Room code must not be blank");
        }
        if (roomRepository.existsByRoomCode(room.getRoomCode())) {
            throw new BusinessException("Room code already exists: " + room.getRoomCode());
        }
        return roomRepository.save(room);
    }

    /**
     * Cập nhật phòng.
     * T5.4: kiểm tra roomCode không trùng (ngoại trừ chính phòng này).
     */
    @Transactional
    public Room update(Long id, Room room) {
        Room existing = getById(id);
        if (room.getRoomCode() != null && !room.getRoomCode().equals(existing.getRoomCode())) {
            if (roomRepository.existsByRoomCodeIgnoreCaseAndIdNot(room.getRoomCode(), id)) {
                throw new BusinessException("Room code already exists: " + room.getRoomCode());
            }
            existing.setRoomCode(room.getRoomCode());
        }
        existing.setName(room.getName());
        existing.setCapacity(room.getCapacity());
        existing.setType(room.getType());
        return roomRepository.save(existing);
    }

    public void delete(Long id) {
        Room room = getById(id);
        if (scheduleRepository.existsByRoomId(id)) {
            throw new BusinessException("Room is used by existing schedules");
        }
        roomRepository.delete(room);
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private Map<Long, Long> classCounts() {
        return scheduleRepository.countClassesPerRoom().stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));
    }
}
