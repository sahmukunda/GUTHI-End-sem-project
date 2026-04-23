package com.guthi.dto.request;

import com.guthi.enums.RequestType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

@Data
public class CreateCommunityRequestDto {

    @NotNull
    private RequestType type;

    /** Ritual / infrastructure narrative, or optional extra notes for land rent. */
    @Size(max = 4000)
    private String description;

    @DecimalMin(value = "0.0", inclusive = true)
    private BigDecimal fundAmountRequested;

    private Integer peopleRequested;

    @Size(max = 500)
    private String landName;

    private LocalDate rentPeriodFrom;

    private LocalDate rentPeriodTo;
}
