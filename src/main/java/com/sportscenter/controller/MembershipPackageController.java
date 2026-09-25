package com.sportscenter.controller;

import com.sportscenter.entity.MembershipPackage;
import com.sportscenter.service.MembershipPackageService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/membership-packages")
public class MembershipPackageController {

    private final MembershipPackageService service;

    public MembershipPackageController(
            MembershipPackageService service) {
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
    public MembershipPackage create(
            @RequestBody MembershipPackage membershipPackage) {

        return service.create(membershipPackage);
    }

    @PutMapping("/{id}")
    public MembershipPackage update(
            @PathVariable Long id,
            @RequestBody MembershipPackage membershipPackage) {

        return service.update(id, membershipPackage);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}