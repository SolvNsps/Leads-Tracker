package com.leadstracker.leadstracker.services.Implementations;

import com.leadstracker.leadstracker.DTO.UserDto;
import com.leadstracker.leadstracker.entities.UserEntity;
import com.leadstracker.leadstracker.repositories.UserRepository;
import com.leadstracker.leadstracker.request.ChangePasswordRequestDto;
import com.leadstracker.leadstracker.request.UpdateUserProfileRequestDto;
import com.leadstracker.leadstracker.response.UserProfileResponseDto;
import com.leadstracker.leadstracker.services.UserProfileService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserProfileServiceImpl implements UserProfileService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public UserProfileResponseDto getProfile(String email) {
        UserEntity user = userRepository.findByEmail(email);
        if (user == null) {
            throw new RuntimeException("User not found");
        }
        return mapToUserProfileDto(user);
    }


        @Override
        @Transactional
        public UserProfileResponseDto updatePhoneNumber (String email, UpdateUserProfileRequestDto requestDto){
            UserEntity user = userRepository.findByEmail(email);

            if (user == null) {
                throw new RuntimeException("User not found or unauthorized");
            }

            user.setPhoneNumber(requestDto.getPhoneNumber());
            userRepository.save(user);

            return mapToUserProfileDto(user);
        }

    private UserProfileResponseDto mapToUserProfileDto(UserEntity user) {
        UserProfileResponseDto dto = new UserProfileResponseDto();
        dto.setFullName(user.getFirstName() + " " + user.getLastName());
        dto.setEmail(user.getEmail());
        dto.setPhoneNumber(user.getPhoneNumber());
        dto.setStaffId(user.getStaffId());
        dto.setRole(user.getRole().getName());
        dto.setCreatedAt(user.getCreatedDate());
        return dto;
    }

        @Override
        @Transactional
        public void changePassword (String email, ChangePasswordRequestDto requestDto){
            UserEntity user = userRepository.findByEmail(email);
            if (user == null) {
                throw new RuntimeException("User not found or unauthorized");
            }

            if (!passwordEncoder.matches(requestDto.getCurrentPassword(), user.getPassword())) {
                throw new RuntimeException("Current password is incorrect");
            }

            user.setPassword(passwordEncoder.encode(requestDto.getNewPassword()));
            userRepository.save(user);
        }
    }

