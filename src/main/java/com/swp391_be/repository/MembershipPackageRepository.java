package com.sportscenter.repository;

import com.sportscenter.entity.MembershipPackage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MembershipPackageRepository
        extends JpaRepository<MembershipPackage, Long> {
}