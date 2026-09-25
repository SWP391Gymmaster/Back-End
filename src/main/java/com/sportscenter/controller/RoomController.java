package com.sportscenter.controller;

import com.sportscenter.entity.Room;
import com.sportscenter.service.RoomService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    private final RoomService service;

    public RoomController(RoomService service) {
        this.service = service;
    }

    @GetMapping
    public List<Room> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public Room getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @PostMapping
    public Room create(@RequestBody Room room) {
        return service.create(room);
    }

    @PutMapping("/{id}")
    public Room update(
            @PathVariable Long id,
            @RequestBody Room room) {

        return service.update(id, room);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}