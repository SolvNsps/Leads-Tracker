package com.leadstracker.leadstracker.services;

import com.leadstracker.leadstracker.request.ChangePasswordRequestDto;
import com.leadstracker.leadstracker.request.UpdateUserProfileRequestDto;
import com.leadstracker.leadstracker.response.UserProfileResponseDto;
import org.springframework.stereotype.Service;

@Service
public interface UserProfileService {
    UserProfileResponseDto getProfile(String email);

    UserProfileResponseDto updatePhoneNumber(String email, UpdateUserProfileRequestDto requestDto);

    void changePassword(String email, ChangePasswordRequestDto requestDto);
}
