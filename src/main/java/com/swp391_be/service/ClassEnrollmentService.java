package com.swp391_be.service;

import com.swp391_be.dto.AvailableClassSessionResponse;
import com.swp391_be.dto.ClassEnrollmentResponse;
import com.swp391_be.entity.ClassEnrollment;
import com.swp391_be.entity.GymClass;
import com.swp391_be.entity.Schedule;
import com.swp391_be.exception.BusinessException;
import com.swp391_be.exception.ResourceNotFoundException;
import com.swp391_be.repository.ClassEnrollmentRepository;
import com.swp391_be.repository.GymClassRepository;
import com.swp391_be.repository.ScheduleRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.swp391_be.repository.ClassRegistrationRepository;

@Service
public class ClassEnrollmentService {

    private final ScheduleRepository schedules;
    private final GymClassRepository classes;
    private final ClassEnrollmentRepository enrollments;
    private final MemberMembershipService membershipService;
    private final Clock clock;
    private final ClassRegistrationRepository registrations;

    public ClassEnrollmentService(
            ScheduleRepository schedules,
            GymClassRepository classes,
            ClassEnrollmentRepository enrollments,
            ClassRegistrationRepository registrations,
            MemberMembershipService membershipService,
            Clock clock) {
        this.schedules = schedules;
        this.classes = classes;
        this.enrollments = enrollments;
        this.registrations = registrations;
        this.membershipService = membershipService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<AvailableClassSessionResponse> getAvailableSessions(Long memberId) {
        LocalDateTime now = LocalDateTime.now(clock);
        return schedules.findByStartTimeAfterOrderByStartTime(now)
                .stream()
                .flatMap(schedule -> availableResponse(memberId, schedule).stream())
                .toList();
    }

    @Transactional
    public ClassEnrollmentResponse enroll(Long memberId, Long scheduleId) {
        Schedule schedule = schedules.findByIdForUpdate(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found"));
        if (!schedule.getStartTime().isAfter(LocalDateTime.now(clock))) {
            throw new BusinessException("Class session has already started");
        }
        GymClass gymClass = classes.findById(schedule.getClassId())
                .orElseThrow(() -> new ResourceNotFoundException("Class not found"));
        if (!registrationAllowed(gymClass, LocalDateTime.now(clock))) {
            throw new BusinessException("Class registration is not open");
        }
        if (!gymClass.acceptsSessionEnrollmentAt(LocalDateTime.now(clock))) {
            throw new BusinessException("Class session registration is not open yet");
        }
        if (!registrations.existsByMemberIdAndClassId(memberId, schedule.getClassId())) {
            throw new BusinessException("Not registered in this class");
        }
        membershipService.requireValidForSession(memberId, schedule.getStartTime().toLocalDate());
        if (enrollments.existsByMemberIdAndScheduleId(memberId, scheduleId)) {
            throw new BusinessException("Already enrolled in this class session");
        }
        long registered = enrollments.countByScheduleId(scheduleId);
        if (registered >= gymClass.getCapacity()) {
            throw new BusinessException("Class session is full");
        }
        try {
            ClassEnrollment saved = enrollments.saveAndFlush(
                    new ClassEnrollment(memberId, scheduleId, Instant.now(clock)));
            return toResponse(saved, schedule, gymClass);
        } catch (DataIntegrityViolationException ex) {
            throw new BusinessException("Already enrolled in this class session");
        }
    }

    @Transactional
    public void cancel(Long memberId, Long scheduleId) {
        Schedule schedule = schedules.findByIdForUpdate(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found"));
        if (!schedule.getStartTime().isAfter(LocalDateTime.now(clock))) {
            throw new BusinessException("Class session has already started");
        }
        membershipService.requireValidForSession(memberId, schedule.getStartTime().toLocalDate());
        ClassEnrollment enrollment = enrollments.findByMemberIdAndScheduleId(memberId, scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found"));
        enrollments.delete(enrollment);
    }

    @Transactional(readOnly = true)
    public List<ClassEnrollmentResponse> getMyEnrollments(Long memberId) {
        return enrollments.findByMemberIdOrderByEnrolledAtDesc(memberId)
                .stream()
                .map(enrollment -> {
                    Schedule schedule = schedules.findById(enrollment.getScheduleId())
                            .orElseThrow(() -> new ResourceNotFoundException("Schedule not found"));
                    GymClass gymClass = classes.findById(schedule.getClassId())
                            .orElseThrow(() -> new ResourceNotFoundException("Class not found"));
                    return toResponse(enrollment, schedule, gymClass);
                })
                .toList();
    }

    private Optional<AvailableClassSessionResponse> availableResponse(Long memberId, Schedule schedule) {
        GymClass gymClass = classes.findById(schedule.getClassId())
                .orElseThrow(() -> new ResourceNotFoundException("Class not found"));
        if (!registrationAllowed(gymClass, LocalDateTime.now(clock))) {
            return Optional.empty();
        }
        if (!gymClass.acceptsSessionEnrollmentAt(LocalDateTime.now(clock))) {
            return Optional.empty();
        }
        long registered = enrollments.countByScheduleId(schedule.getId());
        long remaining = Math.max(0L, gymClass.getCapacity() - registered);
        if (remaining == 0) {
            return Optional.empty();
        }
        return Optional.of(new AvailableClassSessionResponse(
                schedule.getId(),
                gymClass.getId(),
                gymClass.getName(),
                gymClass.getSubject(),
                schedule.getCoachId(),
                schedule.getRoomId(),
                schedule.getStartTime(),
                schedule.getEndTime(),
                gymClass.getCapacity(),
                registered,
                remaining,
                enrollments.existsByMemberIdAndScheduleId(memberId, schedule.getId())));
    }

    private boolean registrationAllowed(GymClass gymClass, LocalDateTime now) {
        return gymClass.acceptsRegistrationAt(now);
    }

    private ClassEnrollmentResponse toResponse(ClassEnrollment enrollment, Schedule schedule, GymClass gymClass) {
        return new ClassEnrollmentResponse(
                enrollment.getId(),
                schedule.getId(),
                gymClass.getId(),
                gymClass.getName(),
                schedule.getStartTime(),
                schedule.getEndTime(),
                enrollment.getEnrolledAt());
    }
}