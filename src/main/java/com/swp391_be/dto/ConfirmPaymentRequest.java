package com.swp391_be.dto;

public class ConfirmPaymentRequest {

    private String transactionReference;
    private String note;

    public ConfirmPaymentRequest() {
    }

    public ConfirmPaymentRequest(String transactionReference, String note) {
        this.transactionReference = transactionReference;
        this.note = note;
    }

    public String getTransactionReference() {
        return transactionReference;
    }

    public void setTransactionReference(String transactionReference) {
        this.transactionReference = transactionReference;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
