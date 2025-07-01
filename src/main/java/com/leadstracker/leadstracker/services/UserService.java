package com.leadstracker.leadstracker.services;

import com.leadstracker.leadstracker.DTO.UserDto;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.Date;
import java.util.List;
import java.util.Map;

public interface UserService extends UserDetailsService {
    UserDto saveUser(UserDto user);

    UserDto getUserByUserId(String id);

    UserDto getUser(String userName);

    UserDto updateUser(String userId, UserDto user);

    List<UserDto> getAllUsers(int page, int limit);

    boolean verifyEmailToken(String token);

    boolean initiatePasswordReset(String email);

    void resetPassword(String token, String newPassword, String confirmNewPassword);

    boolean validateOtp(String email, String otp);

    void saveOtp(String email, String otp, Date expiryTime);

    void deleteUser(String id);

    Map<String, Object> resendOtp(String email);
}
