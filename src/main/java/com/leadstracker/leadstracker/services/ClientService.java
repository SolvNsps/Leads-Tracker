package com.leadstracker.leadstracker.services;

import com.leadstracker.leadstracker.DTO.ClientDto;
import com.leadstracker.leadstracker.DTO.UserDto;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

@Service
public interface ClientService extends UserDetailsService {
    ClientDto createClient(ClientDto clientDto);
}
