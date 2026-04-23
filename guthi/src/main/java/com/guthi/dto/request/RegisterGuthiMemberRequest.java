package com.guthi.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterGuthiMemberRequest {

    @NotBlank
    @Size(max = 200)
    private String fullName;

    @NotBlank
    @Email
    @Size(max = 200)
    private String email;

    @NotBlank
    @Size(min = 8, max = 100)
    private String password;

    @Size(max = 30)
    private String mobileNumber;

    @NotBlank
    @Size(max = 200)
    private String inviteCode;
}
