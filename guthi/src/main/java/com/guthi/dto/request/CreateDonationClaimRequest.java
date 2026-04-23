package com.guthi.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class CreateDonationClaimRequest {

    @NotNull
    @DecimalMin(value = "0.01", inclusive = true)
    private BigDecimal amount;

    @Size(max = 2000)
    private String note;

    @NotBlank
    @Pattern(regexp = "\\d{12}", message = "must be exactly 12 digits")
    @Size(min = 12, max = 12)
    private String upiTransactionId;
}
