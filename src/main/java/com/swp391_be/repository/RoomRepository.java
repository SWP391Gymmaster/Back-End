package com.swp391_be.repository;

import com.swp391_be.entity.Room;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomRepository extends JpaRepository<Room, Long> {

    /** T5.4 — Tra cứu theo mã phòng (thay vì tên). */
    Optional<Room> findByRoomCode(String roomCode);

    /** T5.4 — Kiểm tra trùng mã phòng khi tạo mới. */
    boolean existsByRoomCode(String roomCode);

    /** T5.4 — Kiểm tra trùng mã phòng ngoại trừ chính phòng đang sửa. */
    boolean existsByRoomCodeIgnoreCaseAndIdNot(String roomCode, Long id);
}