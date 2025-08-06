package com.leadstracker.leadstracker.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class UpdateUserProfileRequestDto {

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^(\\+233|020|023|024|025|027|050|054|055|056|057|059)\\d{7}$",
            message = "Phone number must start with +233 or a valid local prefix (e.g., 024, 050, 054) and contain 10–12 digits total")
    private String phoneNumber;

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
}

