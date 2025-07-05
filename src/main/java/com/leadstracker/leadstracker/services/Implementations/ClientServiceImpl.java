package com.leadstracker.leadstracker.services.Implementations;

import com.leadstracker.leadstracker.DTO.ClientDto;
import com.leadstracker.leadstracker.DTO.UserDto;
import com.leadstracker.leadstracker.entities.ClientEntity;
import com.leadstracker.leadstracker.entities.UserEntity;
import com.leadstracker.leadstracker.repositories.ClientRepository;
import com.leadstracker.leadstracker.repositories.UserRepository;
import com.leadstracker.leadstracker.services.ClientService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ClientServiceImpl implements ClientService {

    @Autowired
    ModelMapper modelMapper;

    @Autowired
    ClientRepository clientRepository;

    @Autowired
    UserRepository userRepository;


    /**
     * @param clientDto
     * @return
     */
    @Override
    public ClientDto createClient(ClientDto clientDto) {
        String creatorUserId = clientDto.getCreatedByUserId();

        // Fetching the user
        UserEntity creatorEntity = userRepository.findByUserId(creatorUserId);

        String creatorRole = creatorEntity.getRole().getName();

        if (!creatorRole.equals("TEAM_LEAD") && !creatorRole.equals("TEAM_MEMBER")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Unauthorized to create client.");
        }

        ClientEntity clientEntity = modelMapper.map(clientDto, ClientEntity.class);

        // Setting the creator
        clientEntity.setCreatedBy(creatorEntity);

        // Set the team lead
        if (creatorRole.equals("TEAM_LEAD")) {
            clientEntity.setTeamLead(creatorEntity); // Team lead created the client
        } else {
            if (creatorEntity.getTeamLead() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Team Member must be assigned to a Team Lead.");
            }
            clientEntity.setTeamLead(creatorEntity.getTeamLead()); // Team member created the client
        }

        ClientEntity saved = clientRepository.save(clientEntity);
        return modelMapper.map(saved, ClientDto.class);
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
