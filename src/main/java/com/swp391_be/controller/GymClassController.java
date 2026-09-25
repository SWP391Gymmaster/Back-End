package com.sportscenter.controller;

import com.sportscenter.entity.Class;
import com.sportscenter.service.ClassService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/classes")
public class ClassController {

    private final ClassService service;

    public ClassController(ClassService service) {
        this.service = service;
    }

    @GetMapping
    public List<Class> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public Class getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @PostMapping
    public Class create(@RequestBody Class classEntity) {
        return service.create(classEntity);
    }

    @PutMapping("/{id}")
    public Class update(
            @PathVariable Long id,
            @RequestBody Class classEntity) {

        return service.update(id, classEntity);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}