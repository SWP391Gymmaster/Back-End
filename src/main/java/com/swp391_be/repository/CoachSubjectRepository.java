package com.swp391_be.repository;

import com.swp391_be.entity.CoachSubject;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CoachSubjectRepository extends JpaRepository<CoachSubject, Long> {

    List<CoachSubject> findByCoachIdOrderBySubject(Long coachId);

    boolean existsByCoachIdAndSubjectKey(Long coachId, String subjectKey);

    // Xóa bằng câu lệnh trực tiếp để lần lưu lại cùng bộ môn ngay sau đó không vướng ràng buộc duy nhất
    @Modifying(flushAutomatically = true)
    @Query("delete from CoachSubject s where s.coachId = :coachId")
    void deleteAllByCoachId(@Param("coachId") Long coachId);
}
