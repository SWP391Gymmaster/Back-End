package com.swp391_be.dto;

import com.swp391_be.entity.PaymentMethod;
import jakarta.validation.constraints.NotNull;

public class CreatePaymentRequest {

    @NotNull(message = "Package ID is required")
    private Long packageId;

    // Optional: Used when Manager or Receptionist creates payment on behalf of a member
    private Long memberId;

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;

    private String note;

    public CreatePaymentRequest() {
    }

    public CreatePaymentRequest(Long packageId, Long memberId, PaymentMethod paymentMethod, String note) {
        this.packageId = packageId;
        this.memberId = memberId;
        this.paymentMethod = paymentMethod;
        this.note = note;
    }

    public Long getPackageId() {
        return packageId;
    }

    public void setPackageId(Long packageId) {
        this.packageId = packageId;
    }

    public Long getMemberId() {
        return memberId;
    }

    public void setMemberId(Long memberId) {
        this.memberId = memberId;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
