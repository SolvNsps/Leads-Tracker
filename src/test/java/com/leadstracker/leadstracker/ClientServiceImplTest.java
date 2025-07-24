package com.leadstracker.leadstracker;

import com.leadstracker.leadstracker.DTO.ClientDto;
import com.leadstracker.leadstracker.DTO.UserDto;
import com.leadstracker.leadstracker.DTO.Utils;
import com.leadstracker.leadstracker.entities.ClientEntity;
import com.leadstracker.leadstracker.entities.RoleEntity;
import com.leadstracker.leadstracker.entities.UserEntity;
import com.leadstracker.leadstracker.repositories.ClientRepository;
import com.leadstracker.leadstracker.repositories.UserRepository;
import com.leadstracker.leadstracker.response.Statuses;
import com.leadstracker.leadstracker.services.Implementations.ClientServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ClientServiceImplTest {

    @InjectMocks
    private ClientServiceImpl clientService;

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ModelMapper modelMapper;

    @Mock
    Utils utils;

    private ClientDto clientDto;
    private ClientEntity clientEntity;
    private UserEntity teamLead;
    private UserEntity teamMember;
    private RoleEntity teamLeadRole;
    private RoleEntity teamMemberRole;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        clientDto = new ClientDto();
        UserDto creator = new UserDto();
        clientDto.setCreatedBy(creator);

        clientEntity = new ClientEntity();
        clientEntity.setClientStatus(Statuses.PENDING);

        teamLead = new UserEntity();
        teamLead.setUserId("creator-id");
        teamLead.setFirstName("Lead");
        teamLead.setLastName("User");

        teamMember = new UserEntity();
        teamMember.setUserId("creator-id");
        teamMember.setFirstName("Member");
        teamMember.setLastName("User");

        teamLeadRole = new RoleEntity();
        teamLeadRole.setName("ROLE_TEAM_LEAD");

        teamMemberRole = new RoleEntity();
        teamMemberRole.setName("ROLE_TEAM_MEMBER");
    }

    @Test
    void testCreateClient_AsTeamLead_Success() {
        // Arrange
        UserDto creator = new UserDto();
        creator.setUserId("creator-id");
        teamLead.setRole(teamLeadRole);
        clientDto.setClientStatus("PENDING");
        clientDto.setCreatedBy(creator);
        when(userRepository.findByUserId("creator-id")).thenReturn(teamLead);
        when(modelMapper.map(clientDto, ClientEntity.class)).thenReturn(clientEntity);
        when(clientRepository.save(any(ClientEntity.class))).thenReturn(clientEntity);
        when(modelMapper.map(clientEntity, ClientDto.class)).thenReturn(clientDto);

        // Act
        ClientDto result = clientService.createClient(clientDto);

        // Assert
        assertNotNull(result);
        verify(clientRepository).save(clientEntity);
        assertEquals(clientDto.getCreatedBy(), result.getCreatedBy());
    }

    @Test
    void testCreateClient_AsTeamMemberWithTeamLead_Success() {
        // Arrange
        UserDto creator = new UserDto();
        creator.setUserId("creator-id");
        teamMember.setRole(teamMemberRole);
        teamMember.setTeamLead(teamLead);
        clientDto.setClientStatus("PENDING");
        clientDto.setCreatedBy(creator);

        when(userRepository.findByUserId("creator-id")).thenReturn(teamMember);
        when(modelMapper.map(clientDto, ClientEntity.class)).thenReturn(clientEntity);
        when(clientRepository.save(any(ClientEntity.class))).thenReturn(clientEntity);
        when(modelMapper.map(clientEntity, ClientDto.class)).thenReturn(clientDto);

        // Act
        ClientDto result = clientService.createClient(clientDto);

        // Assert
        assertNotNull(result);
        verify(clientRepository).save(clientEntity);
    }

    @Test
    void testCreateClient_AsTeamMemberWithoutTeamLead_ThrowsException() {
        // Arrange
        teamMember.setRole(teamMemberRole); // no teamLead set
        UserDto creator = new UserDto();
        creator.setUserId("creator-id");
        clientDto.setCreatedBy(creator);
        when(userRepository.findByUserId("creator-id")).thenReturn(teamMember);
        when(modelMapper.map(clientDto, ClientEntity.class)).thenReturn(new ClientEntity());


        // Act & Assert
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            clientService.createClient(clientDto);
        });

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
//        assertTrue(ex.getReason().contains("Team Member must be assigned"));
        verify(clientRepository, never()).save(any());
    }

    @Test
    void testCreateClient_AsUnauthorizedRole_ThrowsForbidden() {
        // Arrange
        UserDto creator = new UserDto();
        creator.setUserId("creator-id");
        clientDto.setCreatedBy(creator);
        RoleEntity randomRole = new RoleEntity();
        randomRole.setName("ADMIN");
        teamLead.setRole(randomRole); // not TEAM_LEAD or TEAM_MEMBER

        when(userRepository.findByUserId("creator-id")).thenReturn(teamLead);

        // Act & Assert
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            clientService.createClient(clientDto);
        });

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Unauthorized"));
        verify(clientRepository, never()).save(any());
    }
}
