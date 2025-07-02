package com.leadstracker.leadstracker.services.Implementations;

import com.leadstracker.leadstracker.DTO.ClientDto;
import com.leadstracker.leadstracker.services.ClientService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class ClientServiceImpl implements ClientService {
    /**
     * @param clientDto
     * @return
     */
    @Override
    public ClientDto createUser(ClientDto clientDto) {
        return null;
    }

    /**
     * @param username
     * @return
     * @throws UsernameNotFoundException
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return null;
    }
}
