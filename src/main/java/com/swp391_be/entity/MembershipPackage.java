package com.swp391_be.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * Catalog gói tập (định nghĩa sản phẩm).
 * Mỗi gói được nhận dạng bằng mã (packageCode) duy nhất, không dùng tên.
 *
 * Quan hệ:
 *   MembershipPackage 1 ──< MemberMembership (member mua gói)
 *   MembershipPackage 1 ──< Invoice          (hóa đơn liên kết)
 */
@Entity
@Table(name = "membership_packages",
        uniqueConstraints = @UniqueConstraint(name = "uk_package_code", columnNames = "package_code"))
public class MembershipPackage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Mã gói — định danh duy nhất, ví dụ: PKG-BRONZE-1M, PKG-GOLD-12M.
     * Dùng mã thay vì tên để tra cứu chính xác (theo yêu cầu task).
     */
    @NotBlank
    @Size(max = 32)
    @Column(name = "package_code", nullable = false, unique = true, length = 32)
    private String packageCode;

    /** Tên hiển thị thân thiện với người dùng (không dùng làm khoá). */
    @NotBlank
    @Column(nullable = false)
    private String name;

    private String description;

    @NotNull
    @PositiveOrZero
    private Double price;

    @NotNull
    @Positive
    private Integer durationDays;

    /**
     * Trạng thái gói: ACTIVE (đang bán) hoặc SUSPENDED (tạm ngưng).
     * Không xoá gói đã có giao dịch — chỉ SUSPENDED.
     */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PackageStatus status = PackageStatus.ACTIVE;

    public MembershipPackage() {
    }

    public MembershipPackage(
            String packageCode,
            String name,
            String description,
            Double price,
            Integer durationDays) {
        this.packageCode = packageCode;
        this.name = name;
        this.description = description;
        this.price = price;
        this.durationDays = durationDays;
        this.status = PackageStatus.ACTIVE;
    }

    // ── Backward-compat constructor (không có packageCode, dùng trong test cũ) ──
    public MembershipPackage(
            String name,
            String description,
            Double price,
            Integer durationDays) {
        String base = (name != null && !name.isBlank()) ? name.replaceAll("\\s+", "-").toUpperCase() : "PKG";
        if (base.length() > 20) {
            base = base.substring(0, 20);
        }
        this.packageCode = (base + "-" + java.util.UUID.randomUUID().toString().substring(0, 8)).toUpperCase();
        this.name = name;
        this.description = description;
        this.price = price;
        this.durationDays = durationDays;
        this.status = PackageStatus.ACTIVE;
    }

    // ── Getters / Setters ──────────────────────────────────────────────────

    public Long getId() {
        return id;
    }

    public String getPackageCode() {
        return packageCode;
    }

    public void setPackageCode(String packageCode) {
        this.packageCode = packageCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public Integer getDurationDays() {
        return durationDays;
    }

    public void setDurationDays(Integer durationDays) {
        this.durationDays = durationDays;
    }

    public PackageStatus getStatus() {
        return status;
    }

    public void setStatus(PackageStatus status) {
        this.status = status;
    }

    // ── Backward-compat helpers (dùng trong code cũ kiểm tra active) ──────

    /** @deprecated Dùng {@link #getStatus()} == ACTIVE thay thế. */
    @Deprecated
    public Boolean getActive() {
        return status == PackageStatus.ACTIVE;
    }

    /** @deprecated Dùng {@link #setStatus(PackageStatus)} thay thế. */
    @Deprecated
    public void setActive(Boolean active) {
        this.status = Boolean.TRUE.equals(active) ? PackageStatus.ACTIVE : PackageStatus.SUSPENDED;
    }
}
