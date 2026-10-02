package com.swp391_be.service;

import com.swp391_be.entity.GymClass;
import com.swp391_be.entity.Role;
import com.swp391_be.exception.BusinessException;
import com.swp391_be.exception.ResourceNotFoundException;
import com.swp391_be.repository.ClassEnrollmentRepository;
import com.swp391_be.repository.GymClassRepository;
import com.swp391_be.repository.ScheduleRepository;
import com.swp391_be.repository.UserRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class GymClassService {

    private final GymClassRepository repository;
    private final ScheduleRepository scheduleRepository;
    private final UserRepository userRepository;
    private final ClassEnrollmentRepository enrollmentRepository;

    public GymClassService(
            GymClassRepository repository,
            ScheduleRepository scheduleRepository,
            UserRepository userRepository,
            ClassEnrollmentRepository enrollmentRepository) {
        this.repository = repository;
        this.scheduleRepository = scheduleRepository;
        this.userRepository = userRepository;
        this.enrollmentRepository = enrollmentRepository;
    }

    public List<GymClass> getAll() {
        return repository.findAll();
    }

    public GymClass getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found"));
    }

    public GymClass create(GymClass classEntity) {
        validateCoach(classEntity.getCoachId());
        return repository.save(classEntity);
    }

    public GymClass update(Long id, GymClass classEntity) {
        GymClass existing = getById(id);
        validateCoach(classEntity.getCoachId());
        validateCapacityNotBelowCurrentEnrollments(id, classEntity.getCapacity());
        existing.setName(classEntity.getName());
        existing.setSubject(classEntity.getSubject());
        existing.setDescription(classEntity.getDescription());
        existing.setCapacity(classEntity.getCapacity());
        existing.setCoachId(classEntity.getCoachId());
        return repository.save(existing);
    }

    public void delete(Long id) {
        GymClass existing = getById(id);
        List<Long> scheduleIds = scheduleRepository.findByClassIdOrderByStartTime(id)
                .stream()
                .map(schedule -> schedule.getId())
                .toList();
        if (scheduleIds.stream().anyMatch(enrollmentRepository::existsByScheduleId)) {
            throw new BusinessException("Class has schedules with enrollments");
        }
        if (!scheduleIds.isEmpty()) {
            throw new BusinessException("Class still has schedules, delete them first");
        }
        repository.delete(existing);
    }

    private void validateCapacityNotBelowCurrentEnrollments(Long classId, Integer requestedCapacity) {
        if (requestedCapacity == null) {
            return;
        }
        long maxRegistered = scheduleRepository.findByClassIdOrderByStartTime(classId)
                .stream()
                .mapToLong(schedule -> enrollmentRepository.countByScheduleId(schedule.getId()))
                .max()
                .orElse(0L);
        if (requestedCapacity < maxRegistered) {
            throw new BusinessException("Capacity cannot be lower than current enrollment count");
        }
    }

    // coachId phải là một user có role COACH (cho phép bỏ trống khi chưa phân công)
    void validateCoach(Long coachId) {
        if (coachId != null && !userRepository.existsByIdAndRole(coachId, Role.COACH)) {
            throw new BusinessException("Coach not found");
        }
    }
}
