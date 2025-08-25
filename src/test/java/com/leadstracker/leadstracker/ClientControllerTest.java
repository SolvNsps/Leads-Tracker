package com.leadstracker.leadstracker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.leadstracker.leadstracker.DTO.ClientDto;
import com.leadstracker.leadstracker.DTO.TeamPerformanceDto;
import com.leadstracker.leadstracker.DTO.UserDto;
import com.leadstracker.leadstracker.DTO.Utils;
import com.leadstracker.leadstracker.controller.ClientController;
import com.leadstracker.leadstracker.entities.NotificationEntity;
import com.leadstracker.leadstracker.entities.RoleEntity;
import com.leadstracker.leadstracker.entities.UserEntity;
import com.leadstracker.leadstracker.request.ClientDetails;
import com.leadstracker.leadstracker.response.ClientRest;
import com.leadstracker.leadstracker.response.GlobalExceptionHandler;
import com.leadstracker.leadstracker.response.PaginatedResponse;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
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

    @MockitoBean
    Utils utils;


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
        clientDetails.setGpsLocation("Accra");
        clientDetails.setClientStatus("INTERESTED");


        mockClientDto = new ClientDto();
        mockClientDto.setId(1L);
        mockClientDto.setFirstName("John");
        mockClientDto.setLastName("Doe");
        mockClientDto.setPhoneNumber("+233123456789");
        mockClientDto.setGpsLocation("Accra");
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

//        when(clientService.getTeamPerformance("user123","week")).thenReturn(performanceDto);

        mockMvc.perform(get("/api/v1/clients/team-performance")
                        .param("duration", "week"))
                .andExpect(status().isOk())
                .andReturn();
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
                .andExpect(jsonPath("$.message").value("Team Lead alerted successfully."));

        verify(notificationService, times(1)).alertTeamLead(notificationId);
    }

//    @Test
//    void testGetOverdueClients() throws Exception {
//        // Arrange
//        String requestedUserId = "user456";
//        String loggedInUserId = "user123";
//        String roleName = "ROLE_TEAM_LEAD";
//
//        // --- Build RoleEntity ---
//        RoleEntity roleEntity = new RoleEntity();
//        roleEntity.setName(roleName);
//        roleEntity.setAuthorities(List.of()); // No extra authorities needed for test
//
//        // --- Build UserEntity ---
//        UserEntity userEntity = new UserEntity();
//        userEntity.setUserId(loggedInUserId);
//        userEntity.setEmail("lead@example.com");
//        userEntity.setPassword("password");
//        userEntity.setRole(roleEntity);
//        userEntity.setEmailVerificationStatus(true);
//
//        // --- Create UserPrincipal ---
//        UserPrincipal realPrincipal = new UserPrincipal(userEntity);
//
//        // --- Inject into SecurityContext ---
//        SecurityContextHolder.getContext().setAuthentication(
//                new UsernamePasswordAuthenticationToken(realPrincipal, null, realPrincipal.getAuthorities())
//        );
//
//        // Prepare mock overdue client
//        ClientRest overdueClient = new ClientRest();
//        overdueClient.setFirstName("Overdue");
//        overdueClient.setLastName("Client");
//
//        // Prepare paginated response
//        PaginatedResponse<ClientRest> paginatedResponse = new PaginatedResponse<>();
//        paginatedResponse.setData(List.of(overdueClient));
//        paginatedResponse.setCurrentPage(0);
//        paginatedResponse.setTotalPages(1);
//        paginatedResponse.setTotalItems(1L);
//        paginatedResponse.setPageSize(10);
//        paginatedResponse.setHasNext(false);
//        paginatedResponse.setHasPrevious(false);
//
//        Pageable expectedPageable = PageRequest.of(0, 10);
//
//        // Mock service layer
//        when(clientService.getOverdueClientsForUserRole(
//                loggedInUserId, roleName, requestedUserId, expectedPageable
//        )).thenReturn(paginatedResponse);
//
//        // Act & Assert
//        mockMvc.perform(get("/api/v1/clients/user/{userId}/overdueClients", requestedUserId)
//                        .param("page", "0")
//                        .param("size", "10"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.data[0].firstName").value("Overdue"))
//                .andExpect(jsonPath("$.pageSize").value(10))
//                .andExpect(jsonPath("$.totalItems").value(1));
//
//        // Verify
//        verify(clientService, times(1))
//                .getOverdueClientsForUserRole(loggedInUserId, roleName, requestedUserId, expectedPageable);
//    }
}
