package com.swp391_be.controller;

import com.swp391_be.entity.GymClass;
import com.swp391_be.service.GymClassService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/classes")
public class GymClassController {

    private final GymClassService service;

    public GymClassController(GymClassService service) {
        this.service = service;
    }

    @GetMapping
    public List<GymClass> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public GymClass getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GymClass create(@Valid @RequestBody GymClass classEntity) {
        return service.create(classEntity);
    }

    @PutMapping("/{id}")
    public GymClass update(
            @PathVariable Long id,
            @Valid @RequestBody GymClass classEntity) {
        return service.update(id, classEntity);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
