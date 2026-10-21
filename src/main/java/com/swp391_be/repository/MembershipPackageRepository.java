package com.swp391_be.repository;

import com.swp391_be.entity.MembershipPackage;
import com.swp391_be.entity.PackageStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MembershipPackageRepository
        extends JpaRepository<MembershipPackage, Long> {

    /** T5.1 — Tra cứu theo mã gói (thay vì tên). */
    Optional<MembershipPackage> findByPackageCode(String packageCode);

    /** T5.2 — Kiểm tra trùng mã gói (khi tạo / cập nhật). */
    boolean existsByPackageCode(String packageCode);

    /** T5.2 — Kiểm tra trùng tên hiển thị (không phân biệt hoa thường). */
    boolean existsByNameIgnoreCase(String name);

    /** T5.2 — Kiểm tra trùng tên ngoại trừ chính gói đang sửa. */
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    /** T5.2 — Kiểm tra trùng mã ngoại trừ chính gói đang sửa. */
    boolean existsByPackageCodeIgnoreCaseAndIdNot(String packageCode, Long id);

    /** Lấy tất cả gói đang ACTIVE (hiển thị cho member). */
    List<MembershipPackage> findByStatus(PackageStatus status);
}