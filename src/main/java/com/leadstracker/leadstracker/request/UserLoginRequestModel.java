package com.leadstracker.leadstracker.request;


public class UserLoginRequestModel {

    private String email;
    private String password;

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

    @Override
    public String toString() {
        return "UserLoginRequestModel{" +
                "email='" + email + '\'' +
                ", password='" + password + '\'' +
                '}';
    }
}
