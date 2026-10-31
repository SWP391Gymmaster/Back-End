package com.swp391_be.service;

import com.swp391_be.entity.GymClass;
import com.swp391_be.entity.Role;
import com.swp391_be.exception.BusinessException;
import com.swp391_be.exception.ResourceNotFoundException;
import com.swp391_be.repository.ClassEnrollmentRepository;
import com.swp391_be.repository.ClassRegistrationRepository;
import com.swp391_be.repository.CoachSubjectRepository;
import com.swp391_be.repository.GymClassRepository;
import com.swp391_be.repository.ScheduleRepository;
import com.swp391_be.repository.UserRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service quản lý lớp tập (GymClass).
 *
 * Ràng buộc T5.4:
 * - Mã lớp (classCode) phải duy nhất — kiểm tra khi tạo/sửa.
 *
 * Ràng buộc coach:
 * - Coach phải được phép dạy bộ môn của lớp (bảng coach_subjects).
 * - Một coach không đảm nhận quá MAX_CLASSES_PER_COACH lớp có khoảng ngày giao
 * nhau.
 * - Không có hai lớp cùng coach, cùng ngày bắt đầu và cùng ngày kết thúc.
 */
@Service
public class GymClassService {

    private final GymClassRepository repository;
    private final ScheduleRepository scheduleRepository;
    private final UserRepository userRepository;
    private final ClassEnrollmentRepository enrollmentRepository;
    private final ClassRegistrationRepository registrationRepository;
    private final CoachSubjectRepository coachSubjectRepository;
    private final Clock clock;

    /** Số lớp tối đa một coach đảm nhận trong cùng một khoảng thời gian. */
    static final int MAX_CLASSES_PER_COACH = 5;

    public GymClassService(
            GymClassRepository repository,
            ScheduleRepository scheduleRepository,
            UserRepository userRepository,
            ClassEnrollmentRepository enrollmentRepository,
            ClassRegistrationRepository registrationRepository,
            CoachSubjectRepository coachSubjectRepository,
            Clock clock) {
        this.repository = repository;
        this.scheduleRepository = scheduleRepository;
        this.userRepository = userRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.registrationRepository = registrationRepository;
        this.coachSubjectRepository = coachSubjectRepository;
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
        validateCoachAssignment(classEntity, null, true, true);
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

        // Chỉ kiểm tra lại ràng buộc coach khi coach, bộ môn hoặc ngày thật sự thay
        // đổi,
        // để vẫn sửa được các lớp cũ (tạo trước khi có ràng buộc này) ở những trường
        // khác
        boolean coachChanged = !Objects.equals(classEntity.getCoachId(), existing.getCoachId());
        boolean subjectChanged = !SubjectKey.of(classEntity.getSubject()).equals(SubjectKey.of(existing.getSubject()));
        boolean datesChanged = !Objects.equals(classEntity.getStartDate(), existing.getStartDate())
                || !Objects.equals(classEntity.getEndDate(), existing.getEndDate());
        validateCoachAssignment(classEntity, id, coachChanged || subjectChanged, coachChanged || datesChanged);

        // Lớp đã kết thúc mà được đổi ngày = mở kỳ mới: xóa đăng ký lớp của kỳ cũ
        boolean reopened = existing.getEndDate() != null
                && existing.getEndDate().isBefore(LocalDate.now(clock))
                && datesChanged;
        if (reopened) {
            registrationRepository.deleteAllByClassId(id);
        } else {
            validateCapacityNotBelowCurrentEnrollments(id, classEntity.getCapacity());
        }
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

    /**
     * Ràng buộc gán coach cho lớp.
     * checkSubject: kiểm tra coach được dạy bộ môn của lớp.
     * checkSchedule: kiểm tra trùng ngày và giới hạn số lớp.
     * excludeId: lớp đang sửa (null khi tạo mới) để không tự tính vào số lớp của
     * coach.
     */
    private void validateCoachAssignment(GymClass incoming, Long excludeId, boolean checkSubject,
            boolean checkSchedule) {
        Long coachId = incoming.getCoachId();
        if (coachId == null) {
            return;
        }
        if (checkSubject && hasText(incoming.getSubject())
                && !coachSubjectRepository.existsByCoachIdAndSubjectKey(coachId,
                        SubjectKey.of(incoming.getSubject()))) {
            throw new BusinessException("Coach does not teach this subject");
        }
        if (!checkSchedule) {
            return;
        }
        // validateCoach đã khóa dòng user của coach nên hai lần tạo lớp đồng thời không
        // vượt được giới hạn
        List<GymClass> others = repository.findByCoachId(coachId).stream()
                .filter(other -> excludeId == null || !excludeId.equals(other.getId()))
                .toList();
        LocalDate start = incoming.getStartDate();
        LocalDate end = incoming.getEndDate();
        if (start != null && end != null) {
            boolean sameDates = others.stream()
                    .anyMatch(other -> start.equals(other.getStartDate()) && end.equals(other.getEndDate()));
            if (sameDates) {
                throw new BusinessException("Coach already has a class with the same start and end dates");
            }
        }
        LocalDate today = LocalDate.now(clock);
        long load = others.stream()
                .filter(other -> other.getEndDate() == null || !other.getEndDate().isBefore(today)) // bỏ lớp đã đóng
                .filter(other -> overlaps(other, start, end))
                .count();
        if (load >= MAX_CLASSES_PER_COACH) {
            throw new BusinessException("Coach already has the maximum number of classes in this period");
        }
    }

    // Chưa nhập đủ ngày thì coi như giao nhau với mọi lớp chưa đóng
    private static boolean overlaps(GymClass other, LocalDate start, LocalDate end) {
        if (start == null || end == null) {
            return true;
        }
        return (other.getStartDate() == null || !other.getStartDate().isAfter(end))
                && (other.getEndDate() == null || !other.getEndDate().isBefore(start));
    }

    /**
     * Coach được chọn riêng cho một buổi (khác coach phụ trách lớp) cũng phải dạy
     * bộ môn của lớp.
     */
    void validateCoachTeaches(Long coachId, GymClass gymClass) {
        if (coachId == null || coachId.equals(gymClass.getCoachId()) || !hasText(gymClass.getSubject())) {
            return;
        }
        if (!coachSubjectRepository.existsByCoachIdAndSubjectKey(coachId, SubjectKey.of(gymClass.getSubject()))) {
            throw new BusinessException("Coach does not teach this subject");
        }
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
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