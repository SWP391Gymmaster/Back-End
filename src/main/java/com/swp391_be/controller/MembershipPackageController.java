package com.swp391_be.controller;

import com.swp391_be.entity.MembershipPackage;
import com.swp391_be.service.MembershipPackageService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * Quản lý catalog gói tập.
 * Endpoint PATCH /{id}/suspend và /{id}/activate để đổi trạng thái (T5.2).
 */
@RestController
@RequestMapping("/api/membership-packages")
public class MembershipPackageController {

    private final MembershipPackageService service;

    public MembershipPackageController(MembershipPackageService service) {
        this.service = service;
    }

    /** Tất cả gói (admin/staff). */
    @GetMapping
    public List<MembershipPackage> getAll() {
        return service.getAll();
    }

    /** Chỉ gói ACTIVE (cho member xem để mua). */
    @GetMapping("/active")
    public List<MembershipPackage> getAllActive() {
        return service.getAllActive();
    }

    /** Tra cứu theo id. */
    @GetMapping("/{id}")
    public MembershipPackage getById(@PathVariable Long id) {
        return service.getById(id);
    }

    /** Tra cứu theo mã gói (T5.1 — dùng mã thay tên). */
    @GetMapping("/by-code/{packageCode}")
    public MembershipPackage getByCode(@PathVariable String packageCode) {
        return service.getByPackageCode(packageCode);
    }

    /** Tạo gói mới (T5.2 — kiểm tra trùng tên/mã). */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MembershipPackage create(@Valid @RequestBody MembershipPackage membershipPackage) {
        return service.create(membershipPackage);
    }

    /**
     * Cập nhật gói (T5.2 — khoá sửa price/duration/code nếu đã có giao dịch).
     * Trả 400 với message nghiệp vụ rõ ràng nếu vi phạm.
     */
    @PutMapping("/{id}")
    public MembershipPackage update(
            @PathVariable Long id,
            @Valid @RequestBody MembershipPackage membershipPackage) {
        return service.update(id, membershipPackage);
    }

    /**
     * Tạm ngưng gói (ACTIVE → SUSPENDED).
     * Dùng thay xoá khi gói đã có giao dịch (T5.2).
     */
    @PatchMapping("/{id}/suspend")
    public MembershipPackage suspend(@PathVariable Long id) {
        return service.suspend(id);
    }

    /**
     * Kích hoạt lại gói (SUSPENDED → ACTIVE).
     */
    @PatchMapping("/{id}/activate")
    public MembershipPackage activate(@PathVariable Long id) {
        return service.activate(id);
    }

    /**
     * Xoá vật lý — chỉ được khi chưa có giao dịch.
     * Trả 400 nếu đã có giao dịch (T5.2).
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
