package com.sportscenter.service;

import com.sportscenter.entity.Room;
import com.sportscenter.repository.RoomRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RoomService {

    private final RoomRepository roomRepository;

    public RoomService(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    public List<Room> getAll() {
        return roomRepository.findAll();
    }

    public Room getById(Long id) {
        return roomRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Room not found"));
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
        roomRepository.deleteById(id);
    }
}