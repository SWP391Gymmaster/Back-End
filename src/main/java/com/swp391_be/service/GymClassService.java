package com.swp391_be.service;

import com.swp391_be.entity.GymClass;
import com.swp391_be.entity.Role;
import com.swp391_be.exception.BusinessException;
import com.swp391_be.exception.ResourceNotFoundException;
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

    public GymClassService(
            GymClassRepository repository,
            ScheduleRepository scheduleRepository,
            UserRepository userRepository) {
        this.repository = repository;
        this.scheduleRepository = scheduleRepository;
        this.userRepository = userRepository;
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
        existing.setName(classEntity.getName());
        existing.setSubject(classEntity.getSubject());
        existing.setDescription(classEntity.getDescription());
        existing.setCapacity(classEntity.getCapacity());
        existing.setCoachId(classEntity.getCoachId());
        return repository.save(existing);
    }

    public void delete(Long id) {
        GymClass existing = getById(id);
        if (scheduleRepository.existsByClassId(id)) {
            throw new BusinessException("Class still has schedules, delete them first");
        }
        repository.delete(existing);
    }

    // coachId phải là một user có role COACH (cho phép bỏ trống khi chưa phân công)
    void validateCoach(Long coachId) {
        if (coachId != null && !userRepository.existsByIdAndRole(coachId, Role.COACH)) {
            throw new BusinessException("Coach not found");
        }
    }
}
