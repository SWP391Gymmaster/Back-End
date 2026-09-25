package com.swp391_be.service;

import com.swp391_be.entity.GymClass;
import com.swp391_be.entity.Schedule;
import com.swp391_be.exception.BusinessException;
import com.swp391_be.exception.ResourceNotFoundException;
import com.swp391_be.repository.ScheduleRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ScheduleService {

    private final ScheduleRepository repository;
    private final GymClassService classService;
    private final RoomService roomService;

    public ScheduleService(
            ScheduleRepository repository,
            GymClassService classService,
            RoomService roomService) {
        this.repository = repository;
        this.classService = classService;
        this.roomService = roomService;
    }

    public List<Schedule> getAll() {
        return repository.findAll();
    }

    public List<Schedule> getByCoach(Long coachId) {
        return repository.findByCoachIdOrderByStartTime(coachId);
    }

    public List<Schedule> getByClass(Long classId) {
        return repository.findByClassIdOrderByStartTime(classId);
    }

    public Schedule getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found"));
    }

    public Schedule create(Schedule schedule) {
        validate(schedule, null);
        return repository.save(schedule);
    }

    public Schedule update(Long id, Schedule schedule) {
        Schedule existing = getById(id);
        validate(schedule, id);
        existing.setClassId(schedule.getClassId());
        existing.setCoachId(schedule.getCoachId());
        existing.setRoomId(schedule.getRoomId());
        existing.setStartTime(schedule.getStartTime());
        existing.setEndTime(schedule.getEndTime());
        return repository.save(existing);
    }

    public void delete(Long id) {
        repository.delete(getById(id));
    }

    private void validate(Schedule schedule, Long excludedScheduleId) {
        validateTime(schedule);

        // Lớp và phòng phải tồn tại (ném 404 nếu không có)
        GymClass gymClass = classService.getById(schedule.getClassId());
        roomService.getById(schedule.getRoomId());

        // Không truyền coachId thì dùng Coach đang phụ trách lớp
        if (schedule.getCoachId() == null) {
            schedule.setCoachId(gymClass.getCoachId());
        }
        if (schedule.getCoachId() == null) {
            throw new BusinessException("Class has no coach assigned");
        }
        classService.validateCoach(schedule.getCoachId());

        boolean coachBusy = repository
                .findByCoachIdAndStartTimeLessThanAndEndTimeGreaterThan(
                        schedule.getCoachId(), schedule.getEndTime(), schedule.getStartTime())
                .stream()
                .anyMatch(existing -> !existing.getId().equals(excludedScheduleId));
        if (coachBusy) {
            throw new BusinessException("Coach already has a schedule during this time");
        }

        boolean roomBusy = repository
                .findByRoomIdAndStartTimeLessThanAndEndTimeGreaterThan(
                        schedule.getRoomId(), schedule.getEndTime(), schedule.getStartTime())
                .stream()
                .anyMatch(existing -> !existing.getId().equals(excludedScheduleId));
        if (roomBusy) {
            throw new BusinessException("Room is already booked during this time");
        }
    }

    private void validateTime(Schedule schedule) {
        if (schedule.getStartTime() == null
                || schedule.getEndTime() == null) {
            throw new BusinessException(
                    "Start time and end time are required");
        }
        if (!schedule.getStartTime()
                .isBefore(schedule.getEndTime())) {
            throw new BusinessException(
                    "Start time must be before end time");
        }
    }
}
