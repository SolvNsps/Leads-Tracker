package com.leadstracker.leadstracker.controller;

import com.leadstracker.leadstracker.DTO.ClientDto;
import com.leadstracker.leadstracker.DTO.UserDto;
import com.leadstracker.leadstracker.request.ClientDetails;
import com.leadstracker.leadstracker.response.ClientRest;
import com.leadstracker.leadstracker.security.UserPrincipal;
import com.leadstracker.leadstracker.services.ClientService;
import com.leadstracker.leadstracker.services.UserService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;


@RestController
@RequestMapping("api/v1/clients")
public class ClientController {

    @Autowired
    ModelMapper modelMapper;

    @Autowired
    UserService userService;

    @Autowired
    ClientService clientService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('TEAM_LEAD', 'TEAM_MEMBER')")
    public ResponseEntity<ClientRest> createClient(
            @RequestBody ClientDetails clientDetails,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        // Getting current logged in user
        String loggedInEmail = userPrincipal.getUsername();
        UserDto creatorUser = userService.getUserByEmail(loggedInEmail);

        // Setting creator in the DTO
        ClientDto clientDto = modelMapper.map(clientDetails, ClientDto.class);
        clientDto.setCreatedByUserId(creatorUser.getUserId());

        // Saving the client
        ClientDto createdClient = clientService.createClient(clientDto);
        ClientRest clientRest = modelMapper.map(createdClient, ClientRest.class);

        return ResponseEntity.ok(clientRest);
    }

}
