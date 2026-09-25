package com.sportscenter.service;

import com.sportscenter.entity.Schedule;
import com.sportscenter.repository.ScheduleRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ScheduleService {

    private final ScheduleRepository repository;

    public ScheduleService(ScheduleRepository repository) {
        this.repository = repository;
    }

    public List<Schedule> getAll() {
        return repository.findAll();
    }

    public Schedule getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Schedule not found"));
    }

    public Schedule create(Schedule schedule) {

        validateTime(schedule);

        if (hasCoachConflict(
                schedule.getCoachId(),
                schedule.getStartTime(),
                schedule.getEndTime(),
                null)) {

            throw new RuntimeException(
                    "Coach already has a schedule during this time");
        }

        return repository.save(schedule);
    }

    public Schedule update(Long id, Schedule schedule) {

        validateTime(schedule);

        if (hasCoachConflict(
                schedule.getCoachId(),
                schedule.getStartTime(),
                schedule.getEndTime(),
                id)) {

            throw new RuntimeException(
                    "Coach already has a schedule during this time");
        }

        Schedule existing = getById(id);

        existing.setClassId(schedule.getClassId());
        existing.setCoachId(schedule.getCoachId());
        existing.setRoomId(schedule.getRoomId());
        existing.setStartTime(schedule.getStartTime());
        existing.setEndTime(schedule.getEndTime());

        return repository.save(existing);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    private void validateTime(Schedule schedule) {

        if (schedule.getStartTime() == null
                || schedule.getEndTime() == null) {

            throw new RuntimeException(
                    "Start time and end time are required");
        }

        if (!schedule.getStartTime()
                .isBefore(schedule.getEndTime())) {

            throw new RuntimeException(
                    "Start time must be before end time");
        }
    }

    private boolean hasCoachConflict(
            Long coachId,
            LocalDateTime startTime,
            LocalDateTime endTime,
            Long excludedScheduleId) {

        return repository.findAll()
                .stream()
                .filter(existing -> coachId != null
                        && coachId.equals(existing.getCoachId()))
                .filter(existing -> excludedScheduleId == null
                        || !excludedScheduleId.equals(
                                existing.getId()))
                .anyMatch(existing -> startTime.isBefore(existing.getEndTime())
                        && endTime.isAfter(
                                existing.getStartTime()));
    }
}