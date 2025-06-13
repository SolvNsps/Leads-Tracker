package com.leadstracker.leadstracker.controller;

import com.leadstracker.leadstracker.DTO.UserDto;
import com.leadstracker.leadstracker.Response.OperationStatusModel;
import com.leadstracker.leadstracker.Response.RequestOperationName;
import com.leadstracker.leadstracker.Response.RequestOperationStatus;
import com.leadstracker.leadstracker.Response.UserRest;
import com.leadstracker.leadstracker.request.UserDetails;
import com.leadstracker.leadstracker.services.UserService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;


@RestController
@RequestMapping("/api/v1/leads")
public class UserController {

    @Autowired
    UserService userService;

    @Autowired
    ModelMapper mapper;


    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UserRest> createUser(@RequestBody UserDetails userDetails) throws Exception {
        UserDto userDto = mapper.map(userDetails, UserDto.class);
        UserDto createdUser = userService.saveUser(userDto);
        UserRest userRest = mapper.map(createdUser, UserRest.class);
        return ResponseEntity.ok(userRest);
    }

    @GetMapping(path = "/{id}")

        public UserRest getUser(@PathVariable String id) throws Exception {
            UserRest userRest = new UserRest();
            UserDto userDto = userService.getUserByUserId(id);
            BeanUtils.copyProperties(userDto, userRest);

            return userRest;
    }

    @PutMapping(path = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public UserRest updateUser(@PathVariable String id, @RequestBody UserDetails userDetails) throws Exception {
        UserRest userRest = new UserRest();

        UserDto userDto = new UserDto();
        BeanUtils.copyProperties(userDetails, userDto);

        UserDto updatedUser = userService.updateUser(id, userDto);
        BeanUtils.copyProperties(updatedUser, userRest);

        return userRest;
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
        }
        else {
            operationStatusModel.setOperationResult(RequestOperationStatus.ERROR.name());
        }
        return operationStatusModel;
    }

}
