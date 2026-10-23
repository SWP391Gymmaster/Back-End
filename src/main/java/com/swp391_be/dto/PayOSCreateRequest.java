package com.swp391_be.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Request tạo link thanh toán PayOS.
 */
public class PayOSCreateRequest {

    @NotNull
    private Long packageId;

    private String description;

    public PayOSCreateRequest() {
    }

    public Long getPackageId() { return packageId; }
    public void setPackageId(Long packageId) { this.packageId = packageId; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
