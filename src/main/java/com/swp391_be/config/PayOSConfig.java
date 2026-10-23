package com.swp391_be.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import vn.payos.PayOS;

/**
 * Cấu hình PayOS — thay thế VNPay.
 * Giá trị được đọc từ application.properties với prefix "payos".
 */
@Configuration
public class PayOSConfig {

    @Value("${payos.client-id}")
    private String clientId;

    @Value("${payos.api-key}")
    private String apiKey;

    @Value("${payos.checksum-key}")
    private String checksumKey;

    @Value("${payos.return-url}")
    private String returnUrl;

    @Value("${payos.cancel-url}")
    private String cancelUrl;

    @Bean
    public PayOS payOS() {
        return new PayOS(clientId, apiKey, checksumKey);
    }

    public String getReturnUrl() { return returnUrl; }
    public String getCancelUrl() { return cancelUrl; }
}
