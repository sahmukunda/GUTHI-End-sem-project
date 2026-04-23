package com.guthi.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * QR behaviour when PhonePe Dynamic QR is not used ({@code guthi.phonepe.enabled=false}).
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "guthi.donation")
public class DonationQrProperties {

    /**
     * Payee UPA / VPA for offline QR (e.g. {@code yourname@oksbi}). When set, the QR encodes a
     * standard {@code upi://pay} intent with {@code am} in rupees so PhonePe / GPay / Paytm open with
     * the amount prefilled. When empty, a legacy https://guthi.np/... link is used (not real UPI;
     * the site may redirect and drop query parameters).
     */
    private String offlinePayeeUpi = "";
}
