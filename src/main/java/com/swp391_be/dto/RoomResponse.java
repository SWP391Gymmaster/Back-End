package com.swp391_be.dto;

import com.swp391_be.entity.Room;
import com.swp391_be.entity.RoomType;

// classCount = số lớp (distinct) đã xếp vào phòng
public record RoomResponse(Long id, String name, Integer capacity, RoomType type, long classCount) {

    public static RoomResponse of(Room room, long classCount) {
        return new RoomResponse(room.getId(), room.getName(), room.getCapacity(), room.getType(), classCount);
    }
}