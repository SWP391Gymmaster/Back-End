package com.swp391_be.dto;

import jakarta.validation.constraints.NotNull;

public class CashCheckoutRequest {

    @NotNull(message = "Member ID is required")
    private Long memberId;

    @NotNull(message = "Package ID is required")
    private Long packageId;

    private String note;

    public CashCheckoutRequest() {
    }

    public CashCheckoutRequest(Long memberId, Long packageId, String note) {
        this.memberId = memberId;
        this.packageId = packageId;
        this.note = note;
    }

    public Long getMemberId() {
        return memberId;
    }

    public void setMemberId(Long memberId) {
        this.memberId = memberId;
    }

    public Long getPackageId() {
        return packageId;
    }

    public void setPackageId(Long packageId) {
        this.packageId = packageId;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
