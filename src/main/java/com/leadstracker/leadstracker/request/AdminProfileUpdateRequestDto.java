package com.leadstracker.leadstracker.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class AdminProfileUpdateRequestDto {

    @NotBlank(message = "Full name is required")
    private String fullName;

    @Pattern(regexp = "^\\+233[0-9]{9}$", message = "Phone number must follow format +233XXXXXXXXX")
    private String phoneNumber;

}
