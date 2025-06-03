package com.leadstracker.leadstracker.controller;

import com.leadstracker.leadstracker.DTO.UserDto;
import com.leadstracker.leadstracker.Response.UserRest;
import com.leadstracker.leadstracker.request.UserDetails;
import com.leadstracker.leadstracker.services.UserService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;


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

    @PutMapping(path = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UserRest> updateUser(@PathVariable String id, @RequestBody UserDetails userDetails) throws Exception {

        UserDto userDto = modelMapper.map(userDetails, UserDto.class);
        UserDto updatedUser = userService.updateUser(id, userDto);

        UserRest userRest = modelMapper.map(updatedUser, UserRest.class);
        return ResponseEntity.ok(userRest);
    }




}
