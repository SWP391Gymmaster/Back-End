package com.swp391_be.entity;

/**
 * Trạng thái của gói tập (MembershipPackage):
 * - ACTIVE   : đang bán, member có thể mua.
 * - SUSPENDED: tạm ngưng bán; gói đã bán vẫn còn hiệu lực với member cũ.
 *   Không dùng "xoá" để giữ toàn vẹn lịch sử giao dịch.
 */
public enum PackageStatus {
    ACTIVE,
    SUSPENDED
}
