package com.leadstracker.leadstracker.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class UpdateUserProfileRequestDto {

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^\\+233\\d{9}$", message = "Phone number must be in the format +233XXXXXXXXX")
    private String phoneNumber;

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
}

