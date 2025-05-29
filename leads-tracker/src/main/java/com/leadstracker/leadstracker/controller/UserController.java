package com.leadstracker.leadstracker.controller;

import com.leadstracker.leadstracker.DTO.UserDto;
import com.leadstracker.leadstracker.Response.UserRest;
import com.leadstracker.leadstracker.request.UserDetails;
import com.leadstracker.leadstracker.services.UserService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/leads")
public class UserController {

    @Autowired
    UserService userService;

    @PostMapping(consumes = {MediaType.APPLICATION_JSON_VALUE},
            produces = {MediaType.APPLICATION_JSON_VALUE})
    public UserRest createUser(@RequestBody UserDetails userDetails) throws Exception {
        System.out.println("Received: " + userDetails.getFirstName());
        UserRest userRest = new UserRest();
        ModelMapper mapper = new ModelMapper();

        UserDto userDto = mapper.map(userDetails, UserDto.class);

        UserDto createdUser = userService.saveUser(userDto);
        userRest = mapper.map(createdUser, UserRest.class);

        return userRest;

    }
}
