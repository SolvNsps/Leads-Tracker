package com.leadstracker.leadstracker.services.Implementations;

import com.leadstracker.leadstracker.DTO.UserDto;
import com.leadstracker.leadstracker.entities.UserEntity;
import com.leadstracker.leadstracker.repositories.UserRepository;
import com.leadstracker.leadstracker.services.UserService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
public class UserServiceImpl implements UserService {

    @Autowired
    UserRepository userRepository;

    @Override
    @Transactional
    public UserDto saveUser(UserDto user) {
        if(userRepository.findByEmail(user.getEmail()) != null){
            throw new RuntimeException("User already exists");
        }
        ModelMapper mapper = new ModelMapper();
        UserEntity userEntity = mapper.map(user, UserEntity.class);

        userEntity.setUserId("UserId");

        UserEntity savedUser = userRepository.save(userEntity);

        UserDto returnUser = mapper.map(savedUser, UserDto.class);
        return returnUser;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return null;
    }
}
