package com.leadstracker.leadstracker.services;

import com.leadstracker.leadstracker.DTO.UserDto;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.List;

public interface UserService extends UserDetailsService {
    UserDto saveUser(UserDto user);

    UserDto getUserByUserId(String id);

    UserDto getUser(String userName);

    UserDto updateUser(String userId, UserDto user);

    List<UserDto> getAllUsers(int page, int limit);

    boolean verifyEmailToken(String token);
}
