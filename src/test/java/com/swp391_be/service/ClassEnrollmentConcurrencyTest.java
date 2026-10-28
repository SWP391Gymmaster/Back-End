package com.swp391_be.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.swp391_be.entity.ClassRegistration;
import com.swp391_be.entity.GymClass;
import com.swp391_be.entity.Schedule;
import com.swp391_be.exception.BusinessException;
import com.swp391_be.repository.ClassEnrollmentRepository;
import com.swp391_be.repository.ClassRegistrationRepository;
import com.swp391_be.repository.GymClassRepository;
import com.swp391_be.repository.ScheduleRepository;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Kiểm tra đồng thời: lớp chỉ còn 1 chỗ, hai hội viên cùng đăng ký một buổi học
 * thì đúng 1 người thành công, người còn lại nhận "Class session is full".
 *
 * Cần database thật (khoá bi quan findByIdForUpdate), nên không đánh
 * dấu @Transactional
 * cho test; dữ liệu được dọn trong @AfterEach.
 */
@SpringBootTest
class ClassEnrollmentConcurrencyTest {

    @Autowired
    private ClassEnrollmentService service;

    @Autowired
    private GymClassRepository classes;

    @Autowired
    private ScheduleRepository schedules;

    @Autowired
    private ClassEnrollmentRepository enrollments;

    @Autowired
    private ClassRegistrationRepository registrations;

    // Bỏ qua kiểm tra gói tập: test này chỉ quan tâm tới sĩ số buổi học.
    @MockitoBean
    private MemberMembershipService membershipService;

    private Long classId;
    private Long scheduleId;

    @AfterEach
    void cleanUp() {
        if (scheduleId != null) {
            enrollments.findAll().stream()
                    .filter(e -> scheduleId.equals(e.getScheduleId()))
                    .forEach(enrollments::delete);
            schedules.deleteById(scheduleId);
        }
        if (classId != null) {
            registrations.findAll().stream()
                    .filter(r -> classId.equals(r.getClassId()))
                    .forEach(registrations::delete);
            classes.deleteById(classId);
        }
    }

    @Test
    void onlyOneMemberGetsTheLastSeat() throws Exception {
        long member1Id = 900_001L;
        long member2Id = 900_002L;

        GymClass gymClass = new GymClass("Concurrency Yoga", "Yoga", "Concurrency test", 1, 3L);
        classId = classes.saveAndFlush(gymClass).getId();
        LocalDateTime start = LocalDateTime.now().plusDays(3).withNano(0);
        Schedule schedule = schedules.saveAndFlush(new Schedule(classId, 3L, 4L, start, start.plusHours(1)));
        scheduleId = schedule.getId();

        // Cả hai hội viên phải đăng ký lớp trước thì mới đăng ký được buổi học
        registrations.save(new ClassRegistration(member1Id, classId, Instant.now()));
        registrations.save(new ClassRegistration(member2Id, classId, Instant.now()));

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<Boolean>> results = new ArrayList<>();
        for (long memberId : new long[] { member1Id, member2Id }) {
            Callable<Boolean> task = () -> {
                ready.countDown();
                go.await();
                try {
                    service.enroll(memberId, scheduleId);
                    return true;
                } catch (BusinessException ex) {
                    assertThat(ex.getMessage()).isEqualTo("Class session is full");
                    return false;
                }
            };
            results.add(pool.submit(task));
        }
        ready.await();
        go.countDown();

        int success = 0;
        for (Future<Boolean> result : results) {
            if (result.get()) {
                success++;
            }
        }
        pool.shutdown();

        assertThat(success).isEqualTo(1);
        assertThat(enrollments.countByScheduleId(scheduleId)).isEqualTo(1L);
    }
}