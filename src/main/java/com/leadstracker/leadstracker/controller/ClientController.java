package com.leadstracker.leadstracker.controller;

import com.leadstracker.leadstracker.DTO.ClientDto;
import com.leadstracker.leadstracker.request.ClientDetails;
import com.leadstracker.leadstracker.response.ClientRest;
import com.leadstracker.leadstracker.services.ClientService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v1/clients")
public class ClientController {

    @Autowired
    ModelMapper modelMapper;

    @Autowired
    ClientService clientService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ClientRest> createClient(@RequestBody ClientDetails clientDetails) {
        ClientDto clientDto = modelMapper.map(clientDetails, ClientDto.class);

        ClientDto createdClient = clientService.createUser(clientDto);
        ClientRest clientRest = modelMapper.map(createdClient, ClientRest.class);

        return ResponseEntity.ok(clientRest);
    }
}
