package com.swp391_be.dto;

import com.swp391_be.entity.PaymentMethod;
import com.swp391_be.entity.PaymentStatus;
import java.time.Instant;

public class PaymentResponse {

    private Long paymentId;
    private String paymentCode;
    private Long invoiceId;
    private String invoiceCode;
    private Long memberId;
    private String memberName;
    private Long packageId;
    private String packageName;
    private Double amount;
    private PaymentMethod paymentMethod;
    private PaymentStatus status;
    private String transactionReference;
    private Instant createdAt;
    private Instant completedAt;
    private String note;

    public PaymentResponse() {
    }

    public PaymentResponse(
            Long paymentId,
            String paymentCode,
            Long invoiceId,
            String invoiceCode,
            Long memberId,
            String memberName,
            Long packageId,
            String packageName,
            Double amount,
            PaymentMethod paymentMethod,
            PaymentStatus status,
            String transactionReference,
            Instant createdAt,
            Instant completedAt,
            String note) {
        this.paymentId = paymentId;
        this.paymentCode = paymentCode;
        this.invoiceId = invoiceId;
        this.invoiceCode = invoiceCode;
        this.memberId = memberId;
        this.memberName = memberName;
        this.packageId = packageId;
        this.packageName = packageName;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.status = status;
        this.transactionReference = transactionReference;
        this.createdAt = createdAt;
        this.completedAt = completedAt;
        this.note = note;
    }

    public Long getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(Long paymentId) {
        this.paymentId = paymentId;
    }

    public String getPaymentCode() {
        return paymentCode;
    }

    public void setPaymentCode(String paymentCode) {
        this.paymentCode = paymentCode;
    }

    public Long getInvoiceId() {
        return invoiceId;
    }

    public void setInvoiceId(Long invoiceId) {
        this.invoiceId = invoiceId;
    }

    public String getInvoiceCode() {
        return invoiceCode;
    }

    public void setInvoiceCode(String invoiceCode) {
        this.invoiceCode = invoiceCode;
    }

    public Long getMemberId() {
        return memberId;
    }

    public void setMemberId(Long memberId) {
        this.memberId = memberId;
    }

    public String getMemberName() {
        return memberName;
    }

    public void setMemberName(String memberName) {
        this.memberName = memberName;
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

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
    }

    public String getTransactionReference() {
        return transactionReference;
    }

    public void setTransactionReference(String transactionReference) {
        this.transactionReference = transactionReference;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
