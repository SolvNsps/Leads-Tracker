package com.leadstracker.leadstracker.controller;

import com.leadstracker.leadstracker.DTO.UserDto;
import com.leadstracker.leadstracker.DTO.Utils;
import com.leadstracker.leadstracker.config.SpringApplicationContext;
import com.leadstracker.leadstracker.response.OperationStatusModel;
import com.leadstracker.leadstracker.response.RequestOperationName;
import com.leadstracker.leadstracker.response.RequestOperationStatus;
import com.leadstracker.leadstracker.response.UserRest;
import com.leadstracker.leadstracker.request.ForgotPasswordRequest;
import com.leadstracker.leadstracker.request.OtpVerificationRequest;
import com.leadstracker.leadstracker.request.ResetPassword;
import com.leadstracker.leadstracker.request.UserDetails;
import com.leadstracker.leadstracker.security.SecurityConstants;
import com.leadstracker.leadstracker.services.UserService;
//import jakarta.validation.Valid;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.modelmapper.ModelMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.time.Instant;
import java.util.*;


@RestController
@RequestMapping("/api/v1/leads")
public class UserController {

    @Autowired
    UserService userService;

    @Autowired
    ModelMapper modelMapper;

    @Autowired
    Utils utils;


    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UserRest> createUser(@RequestBody UserDetails userDetails) throws Exception {
        UserDto userDto = modelMapper.map(userDetails, UserDto.class);

        UserDto createdUser = userService.saveUser(userDto);
        UserRest userRest = modelMapper.map(createdUser, UserRest.class);

        return ResponseEntity.ok(userRest);

    }

    @GetMapping(path = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UserRest> getUser(@PathVariable String id) throws Exception {

        UserDto userDto = userService.getUserByUserId(id);
        UserRest userRest = modelMapper.map(userDto, UserRest.class);

        return ResponseEntity.ok(userRest);
    }


    @PutMapping(path = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UserRest> updateUser(@PathVariable String id, @RequestBody UserDetails userDetails) throws Exception {
        UserDto userDto = modelMapper.map(userDetails, UserDto.class);
        UserDto updatedUser = userService.updateUser(id, userDto);

        UserRest userRest = modelMapper.map(updatedUser, UserRest.class);
        return ResponseEntity.ok(userRest);
    }


    @GetMapping
    public List<UserRest> getAllUsers(@RequestParam(value = "page", defaultValue = "0")
                                      int page, @RequestParam(value = "limit", defaultValue = "10") int limit) throws Exception {
    List<UserRest> userRest = new ArrayList<>();

        List<UserDto> userDtos = userService.getAllUsers(page, limit);
        for (UserDto userDto : userDtos) {
            UserRest userRest1 = new UserRest();
            BeanUtils.copyProperties(userDto, userRest1);
            userRest.add(userRest1);
        }
        return userRest;
    }


//    Email verification endpoint
    @PostMapping("/email-verification")
    public OperationStatusModel verifyEmailToken(@RequestBody String token) {
        OperationStatusModel operationStatusModel = new OperationStatusModel();
        operationStatusModel.setOperationName(RequestOperationName.VERIFY_EMAIL.name());

        boolean isVerified = userService.verifyEmailToken(token);

        if (isVerified) {
            operationStatusModel.setOperationResult(RequestOperationStatus.SUCCESS.name());
        } else {
            operationStatusModel.setOperationResult(RequestOperationStatus.ERROR.name());
        }
        return operationStatusModel;

    }


    @PostMapping("/forgot-password-request")
    public ResponseEntity<?> forgotPassword(@Validated @RequestBody ForgotPasswordRequest request) {
        System.out.println("hitting endpoint");
        boolean result = userService.initiatePasswordReset(request.getEmail());

        String token = utils.generatePasswordResetToken();

        if (result) {
            return ResponseEntity.ok(Map.of(
                    "message", "Password reset instructions have been sent to your email.",
                    "token", token
            ));
        } else {
            return ResponseEntity.ok(Map.of(
                    "message", "No user found with the provided email."
            ));
        }
    }


    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Validated @RequestBody ResetPassword request) {

        try {
            userService.resetPassword(
                    request.getToken(),
                    request.getNewPassword(),
                    request.getConfirmNewPassword());

            return ResponseEntity.ok(Map.of(
                    "message", "Password reset successful.",
                    "status", "SUCCESS"
            ));

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", e.getMessage(),
                    "status", "FAILED"
            ));
        }
    }


    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@Validated @RequestBody OtpVerificationRequest request) {
        // Validating OTP
        boolean isValid = userService.validateOtp(request.getEmail(), request.getOtp());

        if (!isValid) {
            return ResponseEntity.ok(Map.of(
                    "message", "Invalid OTP.",
                    "status", "FAILED"
            ));
        }

        // OTP is valid. Generating JWT
//        String jwt = SecurityConstants.generateToken(request.getEmail(), SecurityConstants.Expiration_Time_In_Seconds);
        String tokenSecret = (String) SpringApplicationContext.getBean("secretKey");

        byte[] secretKeyBytes = Base64.getEncoder().encode(tokenSecret.getBytes());
        SecretKey secretKey = new SecretKeySpec(secretKeyBytes, SignatureAlgorithm.HS512.getJcaName());
        Instant now = Instant.now();

        long expirationTime = SecurityConstants.Expiration_Time_In_Seconds;

        String token = Jwts.builder()
                .setSubject(request.getEmail())
                .setExpiration(Date.from(now.plusMillis(expirationTime)))
                .setIssuedAt(Date.from(now))
                .signWith(secretKey, SignatureAlgorithm.HS512)
                .compact();

        return ResponseEntity.ok(Map.of(
                "token", token,
                "status", "LOGIN_SUCCESS"
        ));
    }
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<String> deleteUser(@PathVariable String id) {
        userService.deleteUser(id);
        return ResponseEntity.ok("User deleted successfully.");
    }

}
