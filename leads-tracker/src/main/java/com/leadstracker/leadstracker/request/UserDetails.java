package com.leadstracker.leadstracker.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

//@Data                       // Generates getters, setters, toString, equals, and hashCode
//@NoArgsConstructor          // No-arg constructor
//@AllArgsConstructor         // All-args constructor
//@Builder
public class UserDetails {
    private String firstName;
    private String lastName;
    private String email;
    private String password;

    public UserDetails() {}

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
