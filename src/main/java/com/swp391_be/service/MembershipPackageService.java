package com.swp391_be.service;

import com.swp391_be.entity.MembershipPackage;
import com.swp391_be.exception.ResourceNotFoundException;
import com.swp391_be.repository.MembershipPackageRepository;
import java.util.List;
import org.springframework.stereotype.Service;

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
                .orElseThrow(() -> new ResourceNotFoundException("Membership package not found"));
    }

    public MembershipPackage create(MembershipPackage membershipPackage) {
        if (membershipPackage.getActive() == null) {
            membershipPackage.setActive(true);
        }
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
        if (membershipPackage.getActive() != null) {
            existing.setActive(membershipPackage.getActive());
        }
        return repository.save(existing);
    }

    public void delete(Long id) {
        repository.delete(getById(id));
    }
}
