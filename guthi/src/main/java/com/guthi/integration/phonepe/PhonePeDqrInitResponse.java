package com.guthi.integration.phonepe;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PhonePeDqrInitResponse {

    private boolean success;
    private String code;
    private String message;
    private PhonePeDqrData data;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PhonePeDqrData {

        private String merchantId;
        private String transactionId;
        private long amount;

        /** UPI intent string to encode as QR image. */
        @JsonProperty("qrString")
        private String qrString;
    }
}
