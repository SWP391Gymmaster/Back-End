package com.swp391_be.service;

import com.swp391_be.entity.GymClass;
import com.swp391_be.entity.Role;
import com.swp391_be.exception.BusinessException;
import com.swp391_be.exception.ResourceNotFoundException;
import com.swp391_be.repository.ClassEnrollmentRepository;
import com.swp391_be.repository.ClassRegistrationRepository;
import com.swp391_be.repository.GymClassRepository;
import com.swp391_be.repository.ScheduleRepository;
import com.swp391_be.repository.UserRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service quản lý lớp tập (GymClass).
 *
 * Ràng buộc T5.4:
 * - Mã lớp (classCode) phải duy nhất — kiểm tra khi tạo/sửa.
 */
@Service
public class GymClassService {

    private final GymClassRepository repository;
    private final ScheduleRepository scheduleRepository;
    private final UserRepository userRepository;
    private final ClassEnrollmentRepository enrollmentRepository;
    private final ClassRegistrationRepository registrationRepository;
    private final Clock clock;

    public GymClassService(
            GymClassRepository repository,
            ScheduleRepository scheduleRepository,
            UserRepository userRepository,
            ClassEnrollmentRepository enrollmentRepository,
            ClassRegistrationRepository registrationRepository,
            Clock clock) {
        this.repository = repository;
        this.scheduleRepository = scheduleRepository;
        this.userRepository = userRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.registrationRepository = registrationRepository;
        this.clock = clock;
    }

    // ── Queries ────────────────────────────────────────────────────────────

    public List<GymClass> getAll() {
        return repository.findAll();
    }

    public GymClass getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found: id=" + id));
    }

    /** T5.4 — Tra cứu theo mã lớp. */
    public GymClass getByClassCode(String classCode) {
        return repository.findByClassCode(classCode)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found: code=" + classCode));
    }

    // ── Commands ───────────────────────────────────────────────────────────

    /**
     * Tạo lớp mới.
     * T5.4: kiểm tra classCode không trùng.
     */
    @Transactional
    public GymClass create(GymClass classEntity) {
        if (classEntity.getClassCode() == null || classEntity.getClassCode().isBlank()) {
            throw new BusinessException("Class code must not be blank");
        }
        if (repository.existsByClassCode(classEntity.getClassCode())) {
            throw new BusinessException("Class code already exists: " + classEntity.getClassCode());
        }
        validateDates(classEntity, null);
        validateCoach(classEntity.getCoachId());
        return repository.save(classEntity);
    }

    /**
     * Cập nhật lớp.
     * T5.4: kiểm tra classCode không trùng (ngoại trừ chính lớp này).
     */
    @Transactional
    public GymClass update(Long id, GymClass classEntity) {
        GymClass existing = getById(id);
        validateDates(classEntity, existing);
        validateCoach(classEntity.getCoachId());
        validateCapacityNotBelowCurrentEnrollments(id, classEntity.getCapacity());
        if (classEntity.getClassCode() != null && !classEntity.getClassCode().equals(existing.getClassCode())) {
            if (repository.existsByClassCodeIgnoreCaseAndIdNot(classEntity.getClassCode(), id)) {
                throw new BusinessException("Class code already exists: " + classEntity.getClassCode());
            }
            existing.setClassCode(classEntity.getClassCode());
        }
        existing.setName(classEntity.getName());
        existing.setSubject(classEntity.getSubject());
        existing.setLevel(classEntity.getLevel());
        existing.setDescription(classEntity.getDescription());
        existing.setStartDate(classEntity.getStartDate());
        existing.setEndDate(classEntity.getEndDate());
        existing.setRegistrationOpenAt(classEntity.getRegistrationOpenAt());
        existing.setRegistrationCloseAt(classEntity.getRegistrationCloseAt());
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
        if (registrationRepository.existsByClassId(id)) {
            throw new BusinessException("Class has registrations");
        }
        if (scheduleIds.stream().anyMatch(enrollmentRepository::existsByScheduleId)) {
            throw new BusinessException("Class has schedules with enrollments");
        }
        if (!scheduleIds.isEmpty()) {
            throw new BusinessException("Class still has schedules, delete them first");
        }
        repository.delete(existing);
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    /**
     * Kiểm tra ngày của lớp. existing = null khi tạo mới; khi sửa, các giá trị
     * KHÔNG đổi so với bản đang lưu
     * thì không bị kiểm tra "không ở quá khứ" (để vẫn sửa được lớp đang chạy).
     */
    private void validateDates(GymClass incoming, GymClass existing) {
        LocalDate start = incoming.getStartDate();
        LocalDate end = incoming.getEndDate();
        LocalDateTime open = incoming.getRegistrationOpenAt();
        LocalDateTime close = incoming.getRegistrationCloseAt();
        // Ngày kết thúc không được trước ngày bắt đầu (cho phép bỏ trống một hoặc cả
        // hai ngày)
        if (start != null && end != null && end.isBefore(start)) {
            throw new BusinessException("End date must not be before start date");
        }
        // Đăng ký sớm: nhập đủ cả mở và đóng, đóng phải sau mở
        if ((open == null) != (close == null)) {
            throw new BusinessException("Registration open and close times must be provided together");
        }
        if (open != null && !close.isAfter(open)) {
            throw new BusinessException("Registration close time must be after registration open time");
        }
        // Đăng ký sớm phải kết thúc trước 00:00 ngày bắt đầu lớp
        if (close != null && start != null && close.isAfter(start.atStartOfDay())) {
            throw new BusinessException("Registration must close before the class start date");
        }
        // Không cho đặt mốc ở quá khứ (giờ Việt Nam theo Clock)
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDate tomorrow = now.toLocalDate().plusDays(1);
        if (start != null && (existing == null || !start.equals(existing.getStartDate())) && start.isBefore(tomorrow)) {
            throw new BusinessException("Start date must be from tomorrow");
        }
        if (open != null && (existing == null || !open.equals(existing.getRegistrationOpenAt()))
                && open.isBefore(now)) {
            throw new BusinessException("Registration open time must not be in the past");
        }
        if (close != null && (existing == null || !close.equals(existing.getRegistrationCloseAt()))
                && !close.isAfter(now)) {
            throw new BusinessException("Registration close time must be in the future");
        }
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
        maxRegistered = Math.max(maxRegistered, registrationRepository.countByClassId(classId));
        if (requestedCapacity < maxRegistered) {
            throw new BusinessException("Capacity cannot be lower than current enrollment count");
        }
    }

    // coachId phải là một user có role COACH (cho phép bỏ trống khi chưa phân công)
    void validateCoach(Long coachId) {
        if (coachId != null) {
            boolean valid = userRepository.findByIdForUpdate(coachId)
                    .map(user -> user.getRole() == Role.COACH)
                    .orElseGet(() -> userRepository.existsByIdAndRole(coachId, Role.COACH));
            if (!valid) {
                throw new BusinessException("Coach not found");
            }
        }
    }
}