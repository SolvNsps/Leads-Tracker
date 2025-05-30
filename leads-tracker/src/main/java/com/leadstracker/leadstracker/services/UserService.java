package com.leadstracker.leadstracker.services;

import com.leadstracker.leadstracker.DTO.UserDto;
import org.springframework.security.core.userdetails.UserDetailsService;

public interface UserService extends UserDetailsService {
    UserDto saveUser(UserDto user);

    UserDto getUserByUserId(String id);

    UserDto getUser(String userName);



    UserDto updateUser(String userId, UserDto user);
}
