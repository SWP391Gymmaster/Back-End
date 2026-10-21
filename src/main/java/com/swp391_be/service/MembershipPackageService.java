package com.swp391_be.service;

import com.swp391_be.entity.MembershipPackage;
import com.swp391_be.entity.PackageStatus;
import com.swp391_be.exception.BusinessException;
import com.swp391_be.exception.ResourceNotFoundException;
import com.swp391_be.repository.InvoiceRepository;
import com.swp391_be.repository.MembershipPackageRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service quản lý catalog gói tập (MembershipPackage).
 *
 * Business rules (T5.2):
 * 1. Tên gói không được trùng (case-insensitive).
 * 2. Mã gói (packageCode) không được trùng (case-insensitive).
 * 3. Không cho SỬA giá / thời hạn / mã của gói đã có giao dịch (Invoice).
 *    (Chỉ cho sửa tên hiển thị và mô tả.)
 * 4. Không cho XOÁ gói đã có giao dịch — chỉ được SUSPENDED.
 * 5. Trạng thái chỉ ACTIVE hoặc SUSPENDED (không xoá vật lý khi có giao dịch).
 */
@Service
public class MembershipPackageService {

    private final MembershipPackageRepository repository;
    private final InvoiceRepository invoices;

    public MembershipPackageService(
            MembershipPackageRepository repository,
            InvoiceRepository invoices) {
        this.repository = repository;
        this.invoices = invoices;
    }

    // ── Queries ────────────────────────────────────────────────────────────

    public List<MembershipPackage> getAll() {
        return repository.findAll();
    }

    public List<MembershipPackage> getAllActive() {
        return repository.findByStatus(PackageStatus.ACTIVE);
    }

    public MembershipPackage getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Membership package not found: id=" + id));
    }

    public MembershipPackage getByPackageCode(String packageCode) {
        return repository.findByPackageCode(packageCode)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Membership package not found: code=" + packageCode));
    }

    // ── Commands ───────────────────────────────────────────────────────────

    @Transactional
    public MembershipPackage create(MembershipPackage pkg) {
        // R1: Trùng mã
        if (pkg.getPackageCode() == null || pkg.getPackageCode().isBlank()) {
            throw new BusinessException("Package code must not be blank");
        }
        if (repository.existsByPackageCode(pkg.getPackageCode())) {
            throw new BusinessException(
                    "Package code already exists: " + pkg.getPackageCode());
        }
        // R2: Trùng tên
        if (pkg.getName() != null && repository.existsByNameIgnoreCase(pkg.getName())) {
            throw new BusinessException(
                    "Package name already exists: " + pkg.getName());
        }
        if (pkg.getStatus() == null) {
            pkg.setStatus(PackageStatus.ACTIVE);
        }
        return repository.save(pkg);
    }

    /**
     * Cập nhật gói tập.
     * Nếu gói đã có giao dịch:
     *   - Chỉ cho sửa name, description, status.
     *   - Khoá sửa packageCode, price, durationDays.
     */
    @Transactional
    public MembershipPackage update(Long id, MembershipPackage req) {
        MembershipPackage existing = getById(id);
        boolean hasInvoices = invoices.existsByPackageId(id);

        // Nếu đã có giao dịch → khoá các trường tài chính
        if (hasInvoices) {
            boolean codeChanged = req.getPackageCode() != null
                    && !req.getPackageCode().equals(existing.getPackageCode());
            boolean priceChanged = req.getPrice() != null
                    && !req.getPrice().equals(existing.getPrice());
            boolean durationChanged = req.getDurationDays() != null
                    && !req.getDurationDays().equals(existing.getDurationDays());

            if (codeChanged || priceChanged || durationChanged) {
                throw new BusinessException(
                        "Cannot modify packageCode, price, or durationDays of a package that already has transactions. "
                                + "You may only update name, description, or status.");
            }
        } else {
            // Không có giao dịch → cho sửa mã (kiểm tra trùng)
            if (req.getPackageCode() != null && !req.getPackageCode().equals(existing.getPackageCode())) {
                if (repository.existsByPackageCodeIgnoreCaseAndIdNot(req.getPackageCode(), id)) {
                    throw new BusinessException(
                            "Package code already exists: " + req.getPackageCode());
                }
                existing.setPackageCode(req.getPackageCode());
            }
            if (req.getPrice() != null) {
                existing.setPrice(req.getPrice());
            }
            if (req.getDurationDays() != null) {
                existing.setDurationDays(req.getDurationDays());
            }
        }

        // Trùng tên (ngoại trừ chính gói này)
        if (req.getName() != null && !req.getName().equals(existing.getName())) {
            if (repository.existsByNameIgnoreCaseAndIdNot(req.getName(), id)) {
                throw new BusinessException(
                        "Package name already exists: " + req.getName());
            }
            existing.setName(req.getName());
        }
        if (req.getDescription() != null) {
            existing.setDescription(req.getDescription());
        }
        if (req.getStatus() != null) {
            existing.setStatus(req.getStatus());
        }

        return repository.save(existing);
    }

    /**
     * Xoá vật lý gói tập.
     * Chỉ cho xoá khi chưa có bất kỳ giao dịch nào.
     * Nếu đã có giao dịch → yêu cầu dùng SUSPENDED thay thế.
     */
    @Transactional
    public void delete(Long id) {
        MembershipPackage existing = getById(id);
        if (invoices.existsByPackageId(id)) {
            throw new BusinessException(
                    "Cannot delete package that already has transactions. "
                            + "Use SUSPENDED status instead: code=" + existing.getPackageCode());
        }
        repository.delete(existing);
    }

    /**
     * Tạm ngưng gói (ACTIVE → SUSPENDED).
     */
    @Transactional
    public MembershipPackage suspend(Long id) {
        MembershipPackage existing = getById(id);
        if (existing.getStatus() == PackageStatus.SUSPENDED) {
            throw new BusinessException("Package is already SUSPENDED: code=" + existing.getPackageCode());
        }
        existing.setStatus(PackageStatus.SUSPENDED);
        return repository.save(existing);
    }

    /**
     * Kích hoạt lại gói (SUSPENDED → ACTIVE).
     */
    @Transactional
    public MembershipPackage activate(Long id) {
        MembershipPackage existing = getById(id);
        if (existing.getStatus() == PackageStatus.ACTIVE) {
            throw new BusinessException("Package is already ACTIVE: code=" + existing.getPackageCode());
        }
        existing.setStatus(PackageStatus.ACTIVE);
        return repository.save(existing);
    }
}
