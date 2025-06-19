package com.leadstracker.leadstracker.services.Implementations;

import com.leadstracker.leadstracker.DTO.AmazonSES;
import com.leadstracker.leadstracker.DTO.UserDto;
import com.leadstracker.leadstracker.DTO.Utils;
import com.leadstracker.leadstracker.entities.UserEntity;
import com.leadstracker.leadstracker.repositories.UserRepository;
import com.leadstracker.leadstracker.security.SecurityConstants;
import com.leadstracker.leadstracker.security.UserPrincipal;
import com.leadstracker.leadstracker.services.UserService;
import io.jsonwebtoken.Jwts;
import org.modelmapper.ModelMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;


@Service
public class UserServiceImpl implements UserService {

    @Autowired
    UserRepository userRepository;


private AmazonSES amazonSES;
//    @Autowired
//    AmazonSES amazonSES;

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
        userEntity.setEmailVerificationStatus(false);
        userEntity.setEmailVerificationToken(utils.generateEmailVerificationToken(publicUserId));

        UserEntity savedUser = userRepository.save(userEntity);

        UserDto returnUser = mapper.map(savedUser, UserDto.class);

        amazonSES.verifyEmail(returnUser);
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
        amazonSES.sendPasswordResetRequest(user.getFirstName(), user.getEmail(), user.getPasswordResetToken());

        // http://localhost:8080/reset-password?token=xyz123
        System.out.println("Password reset link: http://localhost:8080/reset-password?token=" + token);

        return true;
    }

    @Override
    public void resetPassword(String token, String newPassword, String confirmNewPassword) {

        // Validate token
//        String email = Jwts.builder()
//                .setSigningKey(Base64.getEncoder().encode(SecurityConstants.getTokenSecret().getBytes()))
//                .build()
//                .parseClaimsJws(token)
//                .getBody()
//                .getSubject();

        UserEntity user = userRepository.findByPasswordResetToken(token);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid password reset token");
        }
        if (user.getPasswordResetExpiration() == null || user.getPasswordResetExpiration().before(new Date())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password reset token has expired");
        }

        if (!newPassword.equals(confirmNewPassword)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Passwords do not match");
        }

        user.setPassword(bCryptPasswordEncoder.encode(newPassword));
        user.setPasswordResetToken(null);
        user.setPasswordResetExpiration(null);
        user.setDefaultPassword(false);

        userRepository.save(user);
    }

//    /**
//    * @param token
//     * @return
//     */
    @Override
    public boolean verifyEmailToken(String token) {
        boolean returnUser = false;
        UserEntity userEntity = userRepository.findUserByEmailVerificationToken(token);

        if (userEntity != null) {
            boolean hasTokenExpired = utils.hasTokenExpired(token);

            if(!hasTokenExpired) {
                userEntity.setEmailVerificationToken(null);
                userEntity.setEmailVerificationStatus(Boolean.TRUE);
                userRepository.save(userEntity);
                returnUser = true;
            }
        }
        return returnUser;
    }


    //otp implementation
    public void saveOtp(String email, String otp, Date expiryTime) {
        UserEntity user = userRepository.findByEmail(email);

        user.setOtp(otp);
        user.setOtpExpiryDate(expiryTime);
        userRepository.save(user);
    }

    public boolean validateOtp(String email, String otp) {
        UserEntity user = userRepository.findByEmail(email);

       //locking the account after some failed attempts
        if(user.getOtpFailedAttempts() >= 5) {
            throw new RuntimeException("Too many attempts");
        }

        if (user.getOtp() == null || !otp.equals(user.getOtp())) {
            user.setOtpFailedAttempts(user.getOtpFailedAttempts() + 1);
            userRepository.save(user);
            return false;
        }

        if (new Date().after(user.getOtpExpiryDate())) {
            throw new RuntimeException("OTP expired");
        }

        //resetting attempts on success
        user.setOtpFailedAttempts(0);
        user.setOtp(null);
        userRepository.save(user);
        return true;

        //invalidating OTP after successful usage
//        if (isValid) {
//            user.setOtp(null);
//            user.setOtpExpiryDate(null);
//            userRepository.save(user);
//        }
//        return isValid;
    }


    //clearing expired OTPs
    @Scheduled(fixedRate = 3600000) // Runs hourly
    public void cleanupExpiredOtps() {
        List<UserEntity> users = userRepository.findByOtpIsNotNull();
        Date now = new Date();

        users.forEach(user -> {
            if (user.getOtpExpiryDate() != null &&
                    now.after(user.getOtpExpiryDate())) {
                user.setOtp(null);
                user.setOtpExpiryDate(null);
                userRepository.save(user);
            }
        });
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        UserEntity userEntity = userRepository.findByEmail(email);

        if (userEntity == null) throw new UsernameNotFoundException(email);

        return new UserPrincipal(userEntity);

    }
    @Override
    public void deleteUser(String userId) {
        UserEntity userEntity = userRepository.findByUserId(userId);
        userRepository.delete(userEntity);
    }
}
