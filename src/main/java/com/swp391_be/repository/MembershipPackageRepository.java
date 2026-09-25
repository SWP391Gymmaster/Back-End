package com.swp391_be.repository;

import com.swp391_be.entity.MembershipPackage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MembershipPackageRepository
        extends JpaRepository<MembershipPackage, Long> {
}