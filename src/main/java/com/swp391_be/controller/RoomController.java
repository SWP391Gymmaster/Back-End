package com.swp391_be.controller;

import com.swp391_be.dto.RoomResponse;
import com.swp391_be.entity.Room;
import com.swp391_be.entity.Schedule;
import com.swp391_be.service.RoomService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    private final RoomService service;

    public RoomController(RoomService service) {
        this.service = service;
    }

    @GetMapping
    public List<RoomResponse> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public RoomResponse getById(@PathVariable Long id) {
        return service.getResponseById(id);
    }

    @GetMapping("/{id}/schedules")
    public List<Schedule> getSchedules(@PathVariable Long id) {
        return service.getSchedules(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Room create(@Valid @RequestBody Room room) {
        return service.create(room);
    }

    @PutMapping("/{id}")
    public Room update(
            @PathVariable Long id,
            @Valid @RequestBody Room room) {
        return service.update(id, room);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
