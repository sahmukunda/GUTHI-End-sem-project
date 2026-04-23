package com.guthi.integration.phonepe;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PhonePeDqrInitInnerRequest {

    @JsonProperty("merchantId")
    String merchantId;

    @JsonProperty("transactionId")
    String transactionId;

    @JsonProperty("merchantOrderId")
    String merchantOrderId;

    /** Amount in paise (API expects LONG). */
    @JsonProperty("amount")
    long amount;

    @JsonProperty("storeId")
    String storeId;

    @JsonProperty("terminalId")
    String terminalId;

    @JsonProperty("expiresIn")
    long expiresIn;

    @JsonProperty("message")
    String message;
}
