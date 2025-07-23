package com.leadstracker.leadstracker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.leadstracker.leadstracker.DTO.ClientDto;
import com.leadstracker.leadstracker.DTO.TeamPerformanceDto;
import com.leadstracker.leadstracker.DTO.UserDto;
import com.leadstracker.leadstracker.controller.ClientController;
import com.leadstracker.leadstracker.entities.NotificationEntity;
import com.leadstracker.leadstracker.request.ClientDetails;
import com.leadstracker.leadstracker.response.ClientRest;
import com.leadstracker.leadstracker.response.GlobalExceptionHandler;
import com.leadstracker.leadstracker.security.UserPrincipal;
import com.leadstracker.leadstracker.services.ClientService;
import com.leadstracker.leadstracker.services.NotificationService;
import com.leadstracker.leadstracker.services.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.*;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ClientController.class)
@AutoConfigureMockMvc(addFilters = false)
public class ClientControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ClientService clientService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private ModelMapper modelMapper;

    @MockitoBean
    private NotificationService notificationService;

    @MockitoBean
    private GlobalExceptionHandler globalExceptionHandler;


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
    // Arrange
    when(userService.getUserByEmail(anyString())).thenReturn(mockUser);
    when(modelMapper.map(any(ClientDetails.class), eq(ClientDto.class))).thenReturn(mockClientDto);
    when(clientService.createClient(any(ClientDto.class))).thenReturn(mockClientDto);
    when(modelMapper.map(any(ClientDto.class), eq(ClientRest.class))).thenReturn(new ClientRest());

    //  Mock the userPrincipal manually
    UserPrincipal mockPrincipal = mock(UserPrincipal.class);
    when(mockPrincipal.getUsername()).thenReturn("lead@example.com");

    SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(mockPrincipal, null, List.of(new SimpleGrantedAuthority("ROLE_TEAM_LEAD")))
    );

    // Act & Assert
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

        when(clientService.getTeamPerformance("user123","week")).thenReturn(performanceDto);

        mockMvc.perform(get("/api/v1/clients/team-performance")
                        .param("duration", "week"))
                .andExpect(status().isOk())
                .andReturn();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testGetAllUnresolvedNotifications() throws Exception {
        List<NotificationEntity> notifications = List.of(new NotificationEntity(), new NotificationEntity());
        when(notificationService.getUnresolvedNotifications()).thenReturn(notifications);

        mockMvc.perform(get("/api/v1/clients/admin/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testResolveNotification() throws Exception {
        Long notificationId = 1L;

        mockMvc.perform(post("/api/v1/clients/admin/notifications/{id}/resolve", notificationId))
                .andExpect(status().isOk())
                .andExpect(content().string("Notification resolved"));

        verify(notificationService, times(1)).resolveNotification(notificationId);
    }

    // Test for alertTeamLead endpoint
    @Test
    @WithMockUser(roles = "ADMIN")
    void testAlertTeamLead() throws Exception {
        Long notificationId = 1L;

        // Mock behavior
        doNothing().when(notificationService).alertTeamLead(notificationId);

        mockMvc.perform(post("/api/v1/clients/admin/notifications/{id}/alert", notificationId))
                .andExpect(status().isOk())
                .andExpect(content().string("Team Lead alerted successfully."));

        verify(notificationService, times(1)).alertTeamLead(notificationId);
    }


    // Test for getTeamLeadNotifications endpoint
    @Test
    @WithMockUser(username = "lead@example.com", roles = "TEAM_LEAD")
    void testGetTeamLeadNotifications() throws Exception {
        String email = "lead@example.com";
        String teamLeadId = "teamLead123";

        UserDto mockUser = new UserDto();
        mockUser.setUserId(teamLeadId);
        mockUser.setEmail(email);

        List<NotificationEntity> mockNotifications = List.of(new NotificationEntity());

        when(userService.getUserByEmail(email)).thenReturn(mockUser);
        when(notificationService.getNotificationsForTeamLead(teamLeadId)).thenReturn(mockNotifications);

        mockMvc.perform(get("/api/v1/clients/notifications/team-lead"))
                .andExpect(status().isOk());

        verify(userService, times(1)).getUserByEmail(email);
        verify(notificationService, times(1)).getNotificationsForTeamLead(teamLeadId);
    }



}
