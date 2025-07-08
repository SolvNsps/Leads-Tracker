package com.leadstracker.leadstracker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.leadstracker.leadstracker.DTO.ClientDto;
import com.leadstracker.leadstracker.DTO.TeamPerformanceDto;
import com.leadstracker.leadstracker.DTO.UserDto;
import com.leadstracker.leadstracker.controller.ClientController;
import com.leadstracker.leadstracker.request.ClientDetails;
import com.leadstracker.leadstracker.response.ClientRest;
import com.leadstracker.leadstracker.services.ClientService;
import com.leadstracker.leadstracker.services.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.*;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ClientController.class)
@AutoConfigureMockMvc
public class ClientControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ClientService clientService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private ModelMapper modelMapper;

//    @MockitoBean
//    private JwtAuthenticationFilter jwtAuthenticationFilter;
//
//    @MockitoBean
//    private CustomUserDetailsService customUserDetailsService;

    private UserDto mockUser;
    private ClientDto mockClientDto;
    private ClientDetails clientDetails;

    @BeforeEach
    void setUp() {
        mockUser = new UserDto();
        mockUser.setUserId("user123");
        mockUser.setEmail("lead@example.com");
        mockUser.setRole("TEAM_LEAD");

        clientDetails = new ClientDetails();
        clientDetails.setFirstName("John");
        clientDetails.setLastName("Doe");
        clientDetails.setPhoneNumber("+233123456789");
        clientDetails.setGPSLocation("Accra");

        mockClientDto = new ClientDto();
        mockClientDto.setId(1L);
        mockClientDto.setFirstName("John");
        mockClientDto.setLastName("Doe");
        mockClientDto.setPhoneNumber("+233123456789");
        mockClientDto.setGPSLocation("Accra");
    }

    @Test
    @WithMockUser(roles = "TEAM_LEAD")
    void testCreateClient() throws Exception {
        when(userService.getUserByEmail(anyString())).thenReturn(mockUser);
        when(modelMapper.map(any(ClientDetails.class), eq(ClientDto.class))).thenReturn(mockClientDto);
        when(clientService.createClient(any(ClientDto.class))).thenReturn(mockClientDto);
        when(modelMapper.map(any(ClientDto.class), eq(ClientRest.class))).thenReturn(new ClientRest());

        mockMvc.perform(post("/api/v1/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(clientDetails)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "TEAM_LEAD")
    void testGetTeamPerformance() throws Exception {
        TeamPerformanceDto performanceDto = new TeamPerformanceDto();
        performanceDto.setTeamLeadName("Jane Doe");
        performanceDto.setTotalClientsAdded(5);

        when(clientService.getTeamPerformance("week")).thenReturn(performanceDto);

        mockMvc.perform(get("/api/v1/clients/team-performance")
                        .param("duration", "week"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.teamLeadName").value("Jane Doe"))
                .andExpect(jsonPath("$.totalClientsAdded").value(5));
    }

}
