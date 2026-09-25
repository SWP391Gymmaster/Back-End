package com.swp391_be.controller;

import com.swp391_be.entity.MembershipPackage;
import com.swp391_be.service.MembershipPackageService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/membership-packages")
public class MembershipPackageController {

    private final MembershipPackageService service;

    public MembershipPackageController(MembershipPackageService service) {
        this.service = service;
    }

    @GetMapping
    public List<MembershipPackage> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public MembershipPackage getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MembershipPackage create(@Valid @RequestBody MembershipPackage membershipPackage) {
        return service.create(membershipPackage);
    }

    @PutMapping("/{id}")
    public MembershipPackage update(
            @PathVariable Long id,
            @Valid @RequestBody MembershipPackage membershipPackage) {
        return service.update(id, membershipPackage);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
