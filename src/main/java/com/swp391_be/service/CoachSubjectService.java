package com.swp391_be.service;

import com.swp391_be.dto.CoachResponse;
import com.swp391_be.entity.CoachSubject;
import com.swp391_be.entity.Role;
import com.swp391_be.entity.User;
import com.swp391_be.exception.BusinessException;
import com.swp391_be.exception.ResourceNotFoundException;
import com.swp391_be.repository.CoachSubjectRepository;
import com.swp391_be.repository.UserRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CoachSubjectService {

    static final int MAX_SUBJECTS = 20;
    static final int MAX_SUBJECT_LENGTH = 40;

    private final CoachSubjectRepository repository;
    private final UserRepository users;

    public CoachSubjectService(CoachSubjectRepository repository, UserRepository users) {
        this.repository = repository;
        this.users = users;
    }

    /** coachId -> danh sách bộ môn (đã sắp xếp theo tên). */
    public Map<Long, List<String>> subjectsByCoach() {
        return repository.findAll().stream()
                .sorted(Comparator.comparing(CoachSubject::getSubject))
                .collect(Collectors.groupingBy(CoachSubject::getCoachId,
                        Collectors.mapping(CoachSubject::getSubject, Collectors.toList())));
    }

    /** Thay toàn bộ danh sách bộ môn của một coach. Gửi danh sách rỗng để xóa hết. */
    @Transactional
    public CoachResponse replaceSubjects(Long coachId, List<String> requested) {
        User coach = users.findById(coachId)
                .filter(user -> user.getRole() == Role.COACH)
                .orElseThrow(() -> new ResourceNotFoundException("Coach not found"));

        Map<String, String> byKey = new LinkedHashMap<>();
        for (String raw : requested) {
            String name = raw == null ? "" : raw.trim();
            if (name.isEmpty() || name.length() > MAX_SUBJECT_LENGTH) {
                throw new BusinessException("Invalid subject");
            }
            byKey.putIfAbsent(SubjectKey.of(name), name);
        }
        if (byKey.size() > MAX_SUBJECTS) {
            throw new BusinessException("Too many subjects");
        }

        repository.deleteAllByCoachId(coachId);
        repository.saveAll(byKey.entrySet().stream()
                .map(entry -> new CoachSubject(coachId, entry.getValue(), entry.getKey()))
                .toList());
        return CoachResponse.of(coach, new ArrayList<>(byKey.values()));
    }
}
