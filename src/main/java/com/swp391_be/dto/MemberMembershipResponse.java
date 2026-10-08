package com.swp391_be.dto;

import java.time.LocalDate;

public class MemberMembershipResponse {

    private Long id;
    private Long memberId;
    private Long packageId;
    private String packageName;
    private LocalDate startDate;
    private LocalDate endDate;
    private boolean active;
    private long daysRemaining;

    public MemberMembershipResponse() {
    }

    public MemberMembershipResponse(
            Long id,
            Long memberId,
            Long packageId,
            String packageName,
            LocalDate startDate,
            LocalDate endDate,
            boolean active,
            long daysRemaining) {
        this.id = id;
        this.memberId = memberId;
        this.packageId = packageId;
        this.packageName = packageName;
        this.startDate = startDate;
        this.endDate = endDate;
        this.active = active;
        this.daysRemaining = daysRemaining;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public long getDaysRemaining() {
        return daysRemaining;
    }

    public void setDaysRemaining(long daysRemaining) {
        this.daysRemaining = daysRemaining;
    }
}
