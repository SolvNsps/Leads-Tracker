package com.leadstracker.leadstracker.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
//import jakarta.validation.constraints.Size;

public class ForgotPasswordRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
