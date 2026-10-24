package com.swp391_be.controller;

import com.swp391_be.dto.ScheduleResponse;
import com.swp391_be.entity.Schedule;
import com.swp391_be.service.ScheduleService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/schedules")
public class ScheduleController {

    private final ScheduleService service;

    public ScheduleController(ScheduleService service) {
        this.service = service;
    }

    // GET /api/schedules?coachId=3 hoặc ?classId=1 để lọc
    @GetMapping
    public List<ScheduleResponse> getAll(
            @RequestParam(required = false) Long coachId,
            @RequestParam(required = false) Long classId) {
        if (coachId != null) {
            return service.getByCoach(coachId);
        }
        if (classId != null) {
            return service.getByClass(classId);
        }
        return service.getAll();
    }

    @GetMapping("/{id}")
    public ScheduleResponse getById(@PathVariable Long id) {
        return service.getResponseById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Schedule create(@Valid @RequestBody Schedule schedule) {
        return service.create(schedule);
    }

    @PutMapping("/{id}")
    public Schedule update(
            @PathVariable Long id,
            @Valid @RequestBody Schedule schedule) {
        return service.update(id, schedule);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
