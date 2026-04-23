package com.guthi.dto.request;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class CreateVolunteerEventRequest {

    @NotBlank
    @Size(max = 300)
    private String title;

    @Size(max = 4000)
    private String description;

    @NotNull
    @Min(1)
    private Integer volunteersRequired;

    @NotNull
    @FutureOrPresent(message = "Event date and time must be today or in the future")
    private LocalDateTime eventDate;
}
