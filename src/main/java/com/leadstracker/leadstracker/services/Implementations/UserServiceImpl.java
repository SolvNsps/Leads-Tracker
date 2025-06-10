package com.leadstracker.leadstracker.services.Implementations;

import com.leadstracker.leadstracker.DTO.UserDto;
import com.leadstracker.leadstracker.DTO.Utils;
import com.leadstracker.leadstracker.entities.UserEntity;
import com.leadstracker.leadstracker.repositories.UserRepository;
import com.leadstracker.leadstracker.security.UserPrincipal;
import com.leadstracker.leadstracker.services.UserService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;


@Service
public class UserServiceImpl implements UserService {

    @Autowired
    UserRepository userRepository;

    @Autowired
    Utils utils;

    @Autowired
    BCryptPasswordEncoder bCryptPasswordEncoder;

    @Override
    @Transactional
    public UserDto saveUser(UserDto user) {
        if(userRepository.findByEmail(user.getEmail()) != null){
            throw new RuntimeException("User already exists");
        }
        ModelMapper mapper = new ModelMapper();
        UserEntity userEntity = mapper.map(user, UserEntity.class);

        String publicUserId = utils.generateUserId(50);
        userEntity.setUserId(publicUserId);
        userEntity.setPassword(bCryptPasswordEncoder. encode(user.getPassword()));

        UserEntity savedUser = userRepository.save(userEntity);

        UserDto returnUser = mapper.map(savedUser, UserDto.class);
        return returnUser;
    }

    @Override
    public UserDto getUserByUserId(String userId) {
        UserDto userDto = new UserDto();

        UserEntity userEntity = userRepository.findByUserId(userId);

        if (userEntity == null) {
            throw new UsernameNotFoundException("User with ID: " + userId + "not found");
        }
        BeanUtils.copyProperties(userEntity, userDto);
        return userDto;

    }

    /**
     * @param email
     * @return
     */
    @Override
    public UserDto getUser(String email) {
        UserEntity userEntity = userRepository.findByEmail(email);

        if (userEntity == null) {
            throw new UsernameNotFoundException(email);
        }
        UserDto returnUser = new UserDto();

        BeanUtils.copyProperties(userEntity, returnUser);
        return returnUser;
    }


    @Override
    public UserDto updateUser(String userId, UserDto user) {
        UserDto userdto = new UserDto();

        UserEntity userEntity = userRepository.findByUserId(userId);

        if (userEntity == null) {
            throw new UsernameNotFoundException("User with ID: " + userId + "not found");
        }

        userEntity.setFirstName(user.getFirstName());
        userEntity.setLastName(user.getLastName());

        UserEntity updatedUser = userRepository.save(userEntity);
        BeanUtils.copyProperties(updatedUser, userdto);

        return userdto;
    }

    @Override
    public List<UserDto> getAllUsers(int page, int limit) {
        List<UserDto> returnUsers = new ArrayList<>();
        //not necessarily starting the page from 0
        if (page > 0) {
            page -= 1;
        }

        Pageable pageableRequest = PageRequest.of(page, limit);
        Page<UserEntity> usersPage = userRepository.findAll(pageableRequest);
        List<UserEntity> users = usersPage.getContent();

        for (UserEntity userEntity : users) {
            UserDto userDto = new UserDto();
            BeanUtils.copyProperties(userEntity, userDto);
            returnUsers.add(userDto);
        }

        return returnUsers;

    }

    @Override
    public boolean initiatePasswordReset(String email) {
        UserEntity user = userRepository.findByEmail(email);
        if (user == null) {
            return false;
    }
        String token = utils.generatePasswordResetToken();
        user.setPasswordResetToken(token);
        user.setPasswordResetExpiration(new Date(System.currentTimeMillis() + 3600000)); // 1 hour from now

        userRepository.save(user);

        // we'd send an email here with a link like:
        // http://localhost:8080/reset-password?token=xyz123
        // For now, we'll just log/print it.
        System.out.println("Password reset link: http://localhost:8080/reset-password?token=" + token);

        return true;
    }

    @Override
    public void resetPassword(String token, String newPassword, String confirmNewPassword) {

        UserEntity user = userRepository.findByPasswordResetToken(token);
        if (user == null) {
            throw new RuntimeException("Invalid or expired password reset token");
        }

        if (!newPassword.equals(confirmNewPassword)) {
            throw new RuntimeException("Passwords do not match");
        }

        user.setPassword(bCryptPasswordEncoder.encode(newPassword));
        user.setPasswordResetToken(null);
        user.setPasswordResetExpiration(null);

        userRepository.save(user);


    }


    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        UserEntity userEntity = userRepository.findByEmail(email);

        if (userEntity == null) throw new UsernameNotFoundException(email);

        return new UserPrincipal(userEntity);

//        return new User(userEntity.getEmail(), userEntity.getPassword(), true,
//                true, true, true, new ArrayList<>());

    }
}
