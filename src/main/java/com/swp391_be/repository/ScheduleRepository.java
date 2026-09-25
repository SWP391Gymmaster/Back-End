package com.swp391_be.repository;

import com.swp391_be.entity.Schedule;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

    // Các buổi của Coach giao nhau với khoảng [start, end)
    List<Schedule> findByCoachIdAndStartTimeLessThanAndEndTimeGreaterThan(
            Long coachId, LocalDateTime end, LocalDateTime start);

    // Các buổi trong phòng giao nhau với khoảng [start, end)
    List<Schedule> findByRoomIdAndStartTimeLessThanAndEndTimeGreaterThan(
            Long roomId, LocalDateTime end, LocalDateTime start);

    List<Schedule> findByCoachIdOrderByStartTime(Long coachId);

    List<Schedule> findByClassIdOrderByStartTime(Long classId);

    boolean existsByRoomId(Long roomId);

    boolean existsByClassId(Long classId);
}
