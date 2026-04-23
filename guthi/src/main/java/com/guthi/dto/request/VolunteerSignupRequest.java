package com.guthi.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class VolunteerSignupRequest {

    @NotBlank
    @Size(max = 120)
    private String participantName;

    @NotBlank
    @Size(max = 30)
    @Pattern(regexp = "^[0-9+\\-\\s]{6,30}$", message = "Invalid mobile number")
    private String mobileNumber;
}
