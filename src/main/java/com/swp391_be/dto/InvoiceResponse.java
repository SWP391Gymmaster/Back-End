package com.swp391_be.dto;

import com.swp391_be.entity.InvoiceStatus;
import java.time.Instant;

public class InvoiceResponse {

    private Long invoiceId;
    private String invoiceCode;
    private Long memberId;
    private String memberName;
    private Long packageId;
    private String packageName;
    private Double amount;
    private InvoiceStatus status;
    private Instant issuedAt;
    private Instant paidAt;
    private String note;

    public InvoiceResponse() {
    }

    public InvoiceResponse(
            Long invoiceId,
            String invoiceCode,
            Long memberId,
            String memberName,
            Long packageId,
            String packageName,
            Double amount,
            InvoiceStatus status,
            Instant issuedAt,
            Instant paidAt,
            String note) {
        this.invoiceId = invoiceId;
        this.invoiceCode = invoiceCode;
        this.memberId = memberId;
        this.memberName = memberName;
        this.packageId = packageId;
        this.packageName = packageName;
        this.amount = amount;
        this.status = status;
        this.issuedAt = issuedAt;
        this.paidAt = paidAt;
        this.note = note;
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

    public InvoiceStatus getStatus() {
        return status;
    }

    public void setStatus(InvoiceStatus status) {
        this.status = status;
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(Instant issuedAt) {
        this.issuedAt = issuedAt;
    }

    public Instant getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(Instant paidAt) {
        this.paidAt = paidAt;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
