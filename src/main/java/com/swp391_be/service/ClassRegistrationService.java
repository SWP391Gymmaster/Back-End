package com.swp391_be.service;

import com.swp391_be.dto.ClassRegistrationResponse;
import com.swp391_be.dto.ClassStatResponse;
import com.swp391_be.entity.ClassRegistration;
import com.swp391_be.entity.GymClass;
import com.swp391_be.entity.Schedule;
import com.swp391_be.exception.BusinessException;
import com.swp391_be.exception.ResourceNotFoundException;
import com.swp391_be.repository.ClassEnrollmentRepository;
import com.swp391_be.repository.ClassRegistrationRepository;
import com.swp391_be.repository.GymClassRepository;
import com.swp391_be.repository.ScheduleRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Đăng ký / hủy đăng ký LỚP của hội viên. Đăng ký buổi học nằm ở
 * ClassEnrollmentService.
 */
@Service
public class ClassRegistrationService {

    private final GymClassRepository classes;
    private final ClassRegistrationRepository registrations;
    private final ScheduleRepository schedules;
    private final ClassEnrollmentRepository enrollments;
    private final MemberMembershipService membershipService;
    private final Clock clock;

    public ClassRegistrationService(
            GymClassRepository classes,
            ClassRegistrationRepository registrations,
            ScheduleRepository schedules,
            ClassEnrollmentRepository enrollments,
            MemberMembershipService membershipService,
            Clock clock) {
        this.classes = classes;
        this.registrations = registrations;
        this.schedules = schedules;
        this.enrollments = enrollments;
        this.membershipService = membershipService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<ClassStatResponse> getClassStats(Long memberId) {
        Map<Long, Long> counts = registrations.countRegistrationsPerClass().stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));
        Set<Long> mine = new HashSet<>();
        registrations.findByMemberId(memberId).forEach(r -> mine.add(r.getClassId()));
        return classes.findAll().stream()
                .map(gymClass -> {
                    long registered = counts.getOrDefault(gymClass.getId(), 0L);
                    long remaining = Math.max(0L, gymClass.getCapacity() - registered);
                    return new ClassStatResponse(gymClass.getId(), registered, remaining,
                            mine.contains(gymClass.getId()));
                })
                .toList();
    }

    @Transactional
    public ClassRegistrationResponse register(Long memberId, Long classId) {
        // Khóa dòng lớp để hai người cùng đăng ký chỗ cuối không vượt sĩ số.
        GymClass gymClass = classes.findByIdForUpdate(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found"));
        LocalDateTime now = LocalDateTime.now(clock);
        if (!gymClass.acceptsRegistrationAt(now)) {
            throw new BusinessException("Class registration is not open");
        }
        // Gói tập phải còn hiệu lực tới ngày bắt đầu lớp (hoặc hôm nay nếu lớp đã bắt
        // đầu / chưa có ngày bắt đầu).
        LocalDate today = now.toLocalDate();
        LocalDate coverDate = gymClass.getStartDate() != null && gymClass.getStartDate().isAfter(today)
                ? gymClass.getStartDate()
                : today;
        membershipService.requireValidForSession(memberId, coverDate);
        if (registrations.existsByMemberIdAndClassId(memberId, classId)) {
            throw new BusinessException("Already registered in this class");
        }
        if (registrations.countByClassId(classId) >= gymClass.getCapacity()) {
            throw new BusinessException("Class is full");
        }
        try {
            ClassRegistration saved = registrations.saveAndFlush(
                    new ClassRegistration(memberId, classId, Instant.now(clock)));
            return new ClassRegistrationResponse(classId, saved.getRegisteredAt());
        } catch (DataIntegrityViolationException ex) {
            throw new BusinessException("Already registered in this class");
        }
    }

    /**
     * Hủy đăng ký lớp: đồng thời gỡ các buổi học SẮP TỚI của lớp đó mà hội viên đã
     * đăng ký.
     */
    @Transactional
    public void cancel(Long memberId, Long classId) {
        ClassRegistration registration = registrations.findByMemberIdAndClassId(memberId, classId)
                .orElseThrow(() -> new ResourceNotFoundException("Class registration not found"));
        LocalDateTime now = LocalDateTime.now(clock);
        List<Long> upcoming = schedules.findByClassIdOrderByStartTime(classId).stream()
                .filter(schedule -> schedule.getStartTime().isAfter(now))
                .map(Schedule::getId)
                .toList();
        if (!upcoming.isEmpty()) {
            enrollments.deleteByMemberIdAndScheduleIdIn(memberId, upcoming);
        }
        registrations.delete(registration);
    }
}