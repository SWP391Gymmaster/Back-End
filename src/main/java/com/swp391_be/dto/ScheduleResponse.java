package com.swp391_be.dto;

import com.swp391_be.entity.Schedule;
import java.time.LocalDateTime;

public record ScheduleResponse(
        Long id,
        Long classId,
        Long coachId,
        Long roomId,
        LocalDateTime startTime,
        LocalDateTime endTime,
        long registeredCount) {

    public static ScheduleResponse of(Schedule schedule, long registeredCount) {
        return new ScheduleResponse(
                schedule.getId(),
                schedule.getClassId(),
                schedule.getCoachId(),
                schedule.getRoomId(),
                schedule.getStartTime(),
                schedule.getEndTime(),
                registeredCount);
    }
}
