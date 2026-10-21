package com.swp391_be.job;

import com.swp391_be.repository.ClassEnrollmentRepository;
import com.swp391_be.repository.ScheduleRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Job tự động đóng đăng ký lớp khi buổi tập đã bắt đầu (T5.3).
 *
 * Chạy mỗi phút; tìm các schedule đã qua giờ bắt đầu
 * nhưng còn enrollment chưa "đóng". Hiện tại không lưu
 * trạng thái đóng riêng (ClassEnrollment là record vĩnh viễn
 * sau khi buổi bắt đầu — Service kiểm tra startTime tại thời
 * điểm enroll/cancel). Job này chủ yếu log thống kê và có thể
 * mở rộng để gửi thông báo hoặc mark trạng thái.
 *
 * Nếu cần mark "closed" ở DB, hãy thêm cột `status` vào Schedule
 * và job này set ScheduleStatus.CLOSED cho các record quá hạn.
 */
@Component
public class ClassSessionCloseJob {

    private static final Logger log = LoggerFactory.getLogger(ClassSessionCloseJob.class);

    private final ScheduleRepository schedules;
    private final ClassEnrollmentRepository enrollments;
    private final Clock clock;

    public ClassSessionCloseJob(
            ScheduleRepository schedules,
            ClassEnrollmentRepository enrollments,
            Clock clock) {
        this.schedules = schedules;
        this.enrollments = enrollments;
        this.clock = clock;
    }

    /**
     * Chạy mỗi phút: log các buổi tập vừa bắt đầu (quá giờ enroll).
     * Cron: "0 * * * * *" = giây 0 của mỗi phút.
     *
     * Mở rộng: set Schedule.status = CLOSED nếu muốn lưu trạng thái.
     */
    @Scheduled(cron = "0 * * * * *")
    @Transactional(readOnly = true)
    public void closeStartedSessions() {
        LocalDateTime now = LocalDateTime.now(clock);
        // Lấy các buổi đã bắt đầu (startTime <= now) để thống kê / thông báo
        List<Long> startedIds = schedules.findAll().stream()
                .filter(s -> !s.getStartTime().isAfter(now))
                .map(s -> s.getId())
                .toList();

        if (!startedIds.isEmpty()) {
            long totalEnrollments = startedIds.stream()
                    .mapToLong(enrollments::countByScheduleId)
                    .sum();
            log.debug("[ClassSessionCloseJob] {} session(s) already started at {}; total enrollments: {}",
                    startedIds.size(), now, totalEnrollments);
        }
    }
}
