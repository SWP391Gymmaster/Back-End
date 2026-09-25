package com.sportscenter.service;

import com.sportscenter.entity.MembershipPackage;
import com.sportscenter.repository.MembershipPackageRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MembershipPackageService {

    private final MembershipPackageRepository repository;

    public MembershipPackageService(MembershipPackageRepository repository) {
        this.repository = repository;
    }

    public List<MembershipPackage> getAll() {
        return repository.findAll();
    }

    public MembershipPackage getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Membership package not found"));
    }

    public MembershipPackage create(MembershipPackage membershipPackage) {
        return repository.save(membershipPackage);
    }

    public MembershipPackage update(
            Long id,
            MembershipPackage membershipPackage) {

        MembershipPackage existing = getById(id);

        existing.setName(membershipPackage.getName());
        existing.setDescription(membershipPackage.getDescription());
        existing.setPrice(membershipPackage.getPrice());
        existing.setDurationDays(membershipPackage.getDurationDays());

        return repository.save(existing);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }
}