package com.guthi.integration.phonepe;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

@Value
public class PhonePeDqrApiRequest {
    @JsonProperty("request")
    String base64Payload;
}
