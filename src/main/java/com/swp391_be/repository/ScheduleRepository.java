package com.swp391_be.repository;

import com.swp391_be.entity.Schedule;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

    // Các buổi của Coach giao nhau với khoảng [start, end)
    List<Schedule> findByCoachIdAndStartTimeLessThanAndEndTimeGreaterThan(
            Long coachId, LocalDateTime end, LocalDateTime start);

    // Các buổi trong phòng giao nhau với khoảng [start, end)
    List<Schedule> findByRoomIdAndStartTimeLessThanAndEndTimeGreaterThan(
            Long roomId, LocalDateTime end, LocalDateTime start);

    List<Schedule> findByCoachIdOrderByStartTime(Long coachId);

    List<Schedule> findByClassIdOrderByStartTime(Long classId);

    List<Schedule> findByRoomIdOrderByStartTime(Long roomId);

    // [roomId, số lớp distinct] cho mọi phòng đã có lịch
    @Query("select s.roomId, count(distinct s.classId) from Schedule s group by s.roomId")
    List<Object[]> countClassesPerRoom();

    boolean existsByRoomId(Long roomId);

    boolean existsByCoachId(Long coachId);

    boolean existsByClassId(Long classId);

    List<Schedule> findByStartTimeAfterOrderByStartTime(LocalDateTime startTime);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select schedule from Schedule schedule where schedule.id = :id")
    Optional<Schedule> findByIdForUpdate(@Param("id") Long id);
}
