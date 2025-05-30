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


@RestController
@RequestMapping("/leads")
public class UserController {

    @Autowired
    UserService userService;

    @PostMapping(consumes = {MediaType.APPLICATION_JSON_VALUE},
            produces = {MediaType.APPLICATION_JSON_VALUE})
    public UserRest createUser(@RequestBody UserDetails userDetails) throws Exception {

        UserRest userRest = new UserRest();
        ModelMapper mapper = new ModelMapper();

        UserDto userDto = mapper.map(userDetails, UserDto.class);

        UserDto createdUser = userService.saveUser(userDto);
        userRest = mapper.map(createdUser, UserRest.class);

        return userRest;

    }

    @GetMapping(path = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE)

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




}
