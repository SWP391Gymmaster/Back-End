package com.swp391_be.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.swp391_be.entity.ClassEnrollment;
import com.swp391_be.entity.GymClass;
import com.swp391_be.entity.Role;
import com.swp391_be.entity.Room;
import com.swp391_be.entity.RoomType;
import com.swp391_be.entity.Schedule;
import com.swp391_be.entity.User;
import java.time.Instant;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest
class ClassEnrollmentRepositoryTest {

    @Autowired
    private ClassEnrollmentRepository enrollments;

    @Autowired
    private ScheduleRepository schedules;

    @Autowired
    private GymClassRepository classes;

    @Autowired
    private RoomRepository rooms;

    @Autowired
    private UserRepository users;

    @Test
    void countsEnrollmentsForOneSchedule() {
        Long scheduleId = createSchedule();
        Long member1Id = createMember("member-one@example.com");
        Long member2Id = createMember("member-two@example.com");

        enrollments.saveAndFlush(new ClassEnrollment(member1Id, scheduleId, Instant.parse("2026-09-29T01:00:00Z")));
        enrollments.saveAndFlush(new ClassEnrollment(member2Id, scheduleId, Instant.parse("2026-09-29T01:05:00Z")));

        assertThat(enrollments.countByScheduleId(scheduleId)).isEqualTo(2);
        assertThat(schedules.findByIdForUpdate(scheduleId)).isPresent();
    }

    @Test
    void rejectsDuplicateMemberAndSchedule() {
        Long scheduleId = createSchedule();
        Long memberId = createMember("member-duplicate@example.com");

        enrollments.saveAndFlush(new ClassEnrollment(memberId, scheduleId, Instant.parse("2026-09-29T01:00:00Z")));

        assertThatThrownBy(() -> enrollments.saveAndFlush(
                new ClassEnrollment(memberId, scheduleId, Instant.parse("2026-09-29T01:05:00Z"))))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private Long createSchedule() {
        Long coachId = users.save(new User(
                "coach_repo",
                "Coach Repo",
                "coach-repo@example.com",
                "hash",
                null,
                Role.COACH)).getId();
        Long classId = classes.save(new GymClass("Yoga", "Yoga", "Morning yoga", 2, coachId)).getId();
        Long roomId = rooms.save(new Room("Room A", 10, RoomType.YOGA)).getId();
        return schedules.save(new Schedule(
                classId,
                coachId,
                roomId,
                LocalDateTime.of(2026, 10, 1, 9, 0),
                LocalDateTime.of(2026, 10, 1, 10, 0))).getId();
    }

    private Long createMember(String email) {
        return users.save(new User(
                email.substring(0, email.indexOf('@')),
                "Member",
                email,
                "hash",
                null,
                Role.MEMBER)).getId();
    }
}
