package com.swp391_be.service;

import com.swp391_be.dto.ScheduleResponse;
import com.swp391_be.entity.GymClass;
import com.swp391_be.entity.Room;
import com.swp391_be.entity.Schedule;
import com.swp391_be.exception.BusinessException;
import com.swp391_be.exception.ResourceNotFoundException;
import com.swp391_be.repository.ClassEnrollmentRepository;
import com.swp391_be.repository.ScheduleRepository;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ScheduleService {

    private final ScheduleRepository repository;
    private final GymClassService classService;
    private final RoomService roomService;
    private final ClassEnrollmentRepository enrollmentRepository;

    public ScheduleService(
            ScheduleRepository repository,
            GymClassService classService,
            RoomService roomService,
            ClassEnrollmentRepository enrollmentRepository) {
        this.repository = repository;
        this.classService = classService;
        this.roomService = roomService;
        this.enrollmentRepository = enrollmentRepository;
    }

    public List<ScheduleResponse> getAll() {
        return toResponses(repository.findAll());
    }

    public List<ScheduleResponse> getByCoach(Long coachId) {
        return toResponses(repository.findByCoachIdOrderByStartTime(coachId));
    }

    public List<ScheduleResponse> getByClass(Long classId) {
        return toResponses(repository.findByClassIdOrderByStartTime(classId));
    }

    public ScheduleResponse getResponseById(Long id) {
        Schedule schedule = getById(id);
        long registeredCount = enrollmentRepository.countByScheduleId(id);
        return ScheduleResponse.of(schedule, registeredCount);
    }

    public Schedule getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found"));
    }

    @Transactional
    public Schedule create(Schedule schedule) {
        validate(schedule, null);
        return repository.save(schedule);
    }

    @Transactional
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
        Schedule schedule = getById(id);
        if (enrollmentRepository.existsByScheduleId(id)) {
            throw new BusinessException("Schedule has enrollments");
        }
        repository.delete(schedule);
    }

    private void validate(Schedule schedule, Long excludedScheduleId) {
        validateTime(schedule);

        // Lớp và phòng phải tồn tại (ném 404 nếu không có)
        GymClass gymClass = classService.getById(schedule.getClassId());
        Room room = roomService.getById(schedule.getRoomId());

        // Sĩ số lớp không được lớn hơn sức chứa phòng
        if (gymClass.getCapacity() != null && room.getCapacity() != null
                && gymClass.getCapacity() > room.getCapacity()) {
            throw new BusinessException("Class capacity exceeds room capacity");
        }

        // Loại phòng phải hợp bộ môn của lớp
        validateRoomTypeMatchSubject(room, gymClass);

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

    private static final Map<String, java.util.Set<com.swp391_be.entity.RoomType>> SUBJECT_ALLOWED_ROOM_TYPES = Map.of(
            "GYM", java.util.Set.of(com.swp391_be.entity.RoomType.GYM, com.swp391_be.entity.RoomType.CARDIO),
            "YOGA", java.util.Set.of(com.swp391_be.entity.RoomType.YOGA),
            "BOXING", java.util.Set.of(com.swp391_be.entity.RoomType.BOXING),
            "CARDIO", java.util.Set.of(com.swp391_be.entity.RoomType.CARDIO, com.swp391_be.entity.RoomType.CYCLING),
            "ZUMBA", java.util.Set.of(com.swp391_be.entity.RoomType.GROUP_X),
            "PILATES", java.util.Set.of(com.swp391_be.entity.RoomType.YOGA, com.swp391_be.entity.RoomType.GROUP_X),
            "GROUP_X", java.util.Set.of(com.swp391_be.entity.RoomType.GROUP_X),
            "CYCLING", java.util.Set.of(com.swp391_be.entity.RoomType.CYCLING)
    );

    private void validateRoomTypeMatchSubject(Room room, GymClass gymClass) {
        if (room.getType() == null || gymClass.getSubject() == null || gymClass.getSubject().isBlank()) {
            return;
        }
        // (mọi bộ môn): OTHER luôn được phép (theo mục C3)
        if (room.getType() == com.swp391_be.entity.RoomType.OTHER) {
            return;
        }
        String normalizedSubject = gymClass.getSubject().trim().replaceAll("[\\s\\-]+", "_").toUpperCase();
        if (normalizedSubject.equals("GROUPX")) {
            normalizedSubject = "GROUP_X";
        }

        java.util.Set<com.swp391_be.entity.RoomType> allowedTypes = SUBJECT_ALLOWED_ROOM_TYPES.get(normalizedSubject);
        boolean allowed;
        if (allowedTypes != null) {
            allowed = allowedTypes.contains(room.getType());
        } else {
            allowed = room.getType().name().equalsIgnoreCase(normalizedSubject);
        }

        if (!allowed) {
            throw new BusinessException("Room type does not match class subject");
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

    private List<ScheduleResponse> toResponses(List<Schedule> list) {
        Map<Long, Long> counts = enrollmentCounts();
        return list.stream()
                .map(s -> ScheduleResponse.of(s, counts.getOrDefault(s.getId(), 0L)))
                .toList();
    }

    private Map<Long, Long> enrollmentCounts() {
        return enrollmentRepository.countEnrollmentsPerSchedule().stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));
    }
}
