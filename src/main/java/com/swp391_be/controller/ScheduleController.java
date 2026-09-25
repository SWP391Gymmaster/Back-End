package com.sportscenter.controller;

import com.sportscenter.entity.Schedule;
import com.sportscenter.service.ScheduleService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/schedules")
public class ScheduleController {

    private final ScheduleService service;

    public ScheduleController(ScheduleService service) {
        this.service = service;
    }

    @GetMapping
    public List<Schedule> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public Schedule getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @PostMapping
    public Schedule create(@RequestBody Schedule schedule) {
        return service.create(schedule);
    }

    @PutMapping("/{id}")
    public Schedule update(
            @PathVariable Long id,
            @RequestBody Schedule schedule) {

        return service.update(id, schedule);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}