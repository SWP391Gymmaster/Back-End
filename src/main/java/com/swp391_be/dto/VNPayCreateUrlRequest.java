package com.swp391_be.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Request tạo URL thanh toán VNPay (T5.5).
 */
public class VNPayCreateUrlRequest {

    @NotNull
    private Long packageId;

    private String orderInfo;

    private String clientIp;

    public VNPayCreateUrlRequest() {
    }

    public Long getPackageId() { return packageId; }
    public void setPackageId(Long packageId) { this.packageId = packageId; }

    public String getOrderInfo() { return orderInfo; }
    public void setOrderInfo(String orderInfo) { this.orderInfo = orderInfo; }

    public String getClientIp() { return clientIp; }
    public void setClientIp(String clientIp) { this.clientIp = clientIp; }
}
