package com.swp391_be.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.swp391_be.entity.GymClass;
import com.swp391_be.entity.MemberMembership;
import com.swp391_be.entity.Role;
import com.swp391_be.entity.Room;
import com.swp391_be.entity.Schedule;
import com.swp391_be.entity.User;
import com.swp391_be.exception.BusinessException;
import com.swp391_be.service.ClassEnrollmentService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.RepeatedTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@SpringBootTest
class ClassEnrollmentConcurrencyTest {

    @Autowired
    private ClassEnrollmentService service;

    @Autowired
    private ClassEnrollmentRepository enrollments;

    @Autowired
    private MemberMembershipRepository memberships;

    @Autowired
    private ScheduleRepository schedules;

    @Autowired
    private GymClassRepository classes;

    @Autowired
    private RoomRepository rooms;

    @Autowired
    private UserRepository users;

    @RepeatedTest(10)
    void concurrentLastSeatEnrollmentAllowsExactlyOneMember() throws Exception {
        Long coachId = users.save(new User(unique("coach"), "Coach", unique("coach") + "@example.com", "hash", null, Role.COACH)).getId();
        Long member1Id = users.save(new User(unique("member1"), "Member One", unique("member1") + "@example.com", "hash", null, Role.MEMBER)).getId();
        Long member2Id = users.save(new User(unique("member2"), "Member Two", unique("member2") + "@example.com", "hash", null, Role.MEMBER)).getId();
        Long classId = classes.save(new GymClass("Capacity One", "Yoga", "One seat", 1, coachId)).getId();
        Long roomId = rooms.save(new Room(unique("room"), 10)).getId();
        Long scheduleId = schedules.save(new Schedule(
                classId,
                coachId,
                roomId,
                LocalDateTime.of(2026, 10, 1, 9, 0),
                LocalDateTime.of(2026, 10, 1, 10, 0))).getId();
        memberships.save(new MemberMembership(member1Id, 100L, LocalDate.of(2026, 9, 29), LocalDate.of(2026, 10, 2)));
        memberships.save(new MemberMembership(member2Id, 100L, LocalDate.of(2026, 9, 29), LocalDate.of(2026, 10, 2)));

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<String> first = executor.submit(enrollWhenReleased(member1Id, scheduleId, ready, start));
            Future<String> second = executor.submit(enrollWhenReleased(member2Id, scheduleId, ready, start));

            ready.await();
            start.countDown();

            assertThat(List.of(first.get(), second.get()))
                    .containsExactlyInAnyOrder("success", "Class session is full");
            assertThat(enrollments.countByScheduleId(scheduleId)).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }

    private Callable<String> enrollWhenReleased(Long memberId, Long scheduleId, CountDownLatch ready, CountDownLatch start) {
        return () -> {
            ready.countDown();
            start.await();
            try {
                service.enroll(memberId, scheduleId);
                return "success";
            } catch (BusinessException ex) {
                return ex.getMessage();
            }
        };
    }

    private String unique(String prefix) {
        return prefix + "_" + System.nanoTime();
    }

    @TestConfiguration
    static class FixedClockConfig {

        @Bean
        @Primary
        Clock testClock() {
            return Clock.fixed(
                    Instant.parse("2026-09-29T00:00:00Z"),
                    ZoneId.of("Asia/Ho_Chi_Minh"));
        }
    }
}
