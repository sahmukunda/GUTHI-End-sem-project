package com.guthi.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateWorkPostRequest {

    @NotBlank
    @Size(max = 300)
    private String title;

    @Size(max = 4000)
    private String description;

    @NotBlank(message = "Mobile number is required")
    @Pattern(regexp = "^[+]?[0-9]{7,15}$", message = "Enter a valid mobile number")
    private String mobileNumber;
}
