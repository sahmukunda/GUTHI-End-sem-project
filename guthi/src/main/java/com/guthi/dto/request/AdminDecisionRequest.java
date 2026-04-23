package com.guthi.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminDecisionRequest {

    @Size(max = 2000)
    private String adminNote;
}
