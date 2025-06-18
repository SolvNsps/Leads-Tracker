package com.leadstracker.leadstracker.controller;

import com.leadstracker.leadstracker.DTO.UserDto;
import com.leadstracker.leadstracker.Response.OperationStatusModel;
import com.leadstracker.leadstracker.Response.RequestOperationName;
import com.leadstracker.leadstracker.Response.RequestOperationStatus;
import com.leadstracker.leadstracker.Response.UserRest;
import com.leadstracker.leadstracker.request.ForgotPasswordRequest;
import com.leadstracker.leadstracker.request.OtpVerificationRequest;
import com.leadstracker.leadstracker.request.ResetPassword;
import com.leadstracker.leadstracker.request.UserDetails;
import com.leadstracker.leadstracker.security.SecurityConstants;
import com.leadstracker.leadstracker.services.UserService;
//import jakarta.validation.Valid;
import org.modelmapper.ModelMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/api/v1/leads")
public class UserController {

    @Autowired
    UserService userService;

    @Autowired
    ModelMapper modelMapper;


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
    public ResponseEntity<String> forgotPassword(@Validated @RequestBody ForgotPasswordRequest request) {
        System.out.println("hitting endpoint");
        boolean result = userService.initiatePasswordReset(request.getEmail());

        if (result) {
            return ResponseEntity.ok("Password reset instructions have been sent to your email.");
        } else {
            return ResponseEntity.badRequest().body("No user found with the provided email.");
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
        // Validate OTP
        boolean isValid = userService.validateOtp(request.getEmail(), request.getOtp());

        if (!isValid) {
            return ResponseEntity.status(401).body("Invalid OTP");
        }

        // OTP is valid! Generate JWT
        String jwt = SecurityConstants.generateToken(request.getEmail(), SecurityConstants.Expiration_Time_In_Seconds);

        return ResponseEntity.ok(Map.of(
                "token", jwt,
                "status", "LOGIN_SUCCESS"
        ));
    }

}
