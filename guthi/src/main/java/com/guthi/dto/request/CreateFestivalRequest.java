package com.guthi.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.Data;

@Data
public class CreateFestivalRequest {

    @NotBlank
    @Size(max = 300)
    private String name;

    @Size(max = 4000)
    private String description;

    @NotNull
    private LocalDate festivalDate;

}
