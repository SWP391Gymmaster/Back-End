package com.sportscenter.service;

import com.sportscenter.entity.Class;
import com.sportscenter.repository.ClassRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ClassService {

    private final ClassRepository repository;

    public ClassService(ClassRepository repository) {
        this.repository = repository;
    }

    public List<Class> getAll() {
        return repository.findAll();
    }

    public Class getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Class not found"));
    }

    public Class create(Class classEntity) {
        return repository.save(classEntity);
    }

    public Class update(Long id, Class classEntity) {
        Class existing = getById(id);

        existing.setName(classEntity.getName());
        existing.setDescription(classEntity.getDescription());
        existing.setCapacity(classEntity.getCapacity());
        existing.setCoachId(classEntity.getCoachId());

        return repository.save(existing);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }
}