package com.swp391_be.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Phòng tập.
 * Mỗi phòng được nhận dạng bằng mã (roomCode) duy nhất, ví dụ: ROOM-GYM-A.
 * Mã phòng dùng thay tên để tránh sai sót khi tra cứu.
 *
 * Ràng buộc trùng lịch phòng được kiểm tra trong ScheduleService thông qua
 * ScheduleRepository.findByRoomIdAndStartTimeLessThanAndEndTimeGreaterThan().
 */
@Entity
@Table(name = "rooms",
        uniqueConstraints = @UniqueConstraint(name = "uk_room_code", columnNames = "room_code"))
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Mã phòng — định danh duy nhất, ví dụ: ROOM-GYM-A, ROOM-YOGA-B.
     * Dùng mã thay vì tên để tra cứu chính xác.
     */
    @NotBlank
    @Size(min = 3, max = 32)
    @Pattern(regexp = "^[A-Z0-9]+(-[A-Z0-9]+)*$")
    @Column(name = "room_code", nullable = false, unique = true, length = 32)
    private String roomCode;

    /** Tên hiển thị thân thiện (không dùng làm khoá). */
    @NotBlank
    @Column(nullable = false)
    private String name;

    @Positive
    private Integer capacity;

    @NotNull
    @Enumerated(EnumType.STRING)
    private RoomType type;

    public Room() {
    }

    public Room(String roomCode, String name, Integer capacity, RoomType type) {
        this.roomCode = roomCode;
        this.name = name;
        this.capacity = capacity;
        this.type = type;
    }

    // ── Backward-compat constructor (không có roomCode, dùng trong test cũ) ──
    public Room(String name, Integer capacity, RoomType type) {
        String base = (name != null && !name.isBlank()) ? name.replaceAll("[^a-zA-Z0-9]+", "-").replaceAll("^-+|-+$", "").toUpperCase() : "ROOM";
        if (base.isBlank()) {
            base = "ROOM";
        }
        if (base.length() > 20) {
            base = base.substring(0, 20).replaceAll("-+$", "");
        }
        this.roomCode = (base + "-" + java.util.UUID.randomUUID().toString().substring(0, 8)).toUpperCase();
        this.name = name;
        this.capacity = capacity;
        this.type = type;
    }

    // ── Getters / Setters ──────────────────────────────────────────────────

    public Long getId() {
        return id;
    }

    public String getRoomCode() {
        return roomCode;
    }

    public void setRoomCode(String roomCode) {
        this.roomCode = roomCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public RoomType getType() {
        return type;
    }

    public void setType(RoomType type) {
        this.type = type;
    }
}
