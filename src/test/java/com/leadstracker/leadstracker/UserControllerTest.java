package com.leadstracker.leadstracker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.leadstracker.leadstracker.DTO.UserDto;
import com.leadstracker.leadstracker.DTO.Utils;
import com.leadstracker.leadstracker.entities.RoleEntity;
import com.leadstracker.leadstracker.entities.UserEntity;
import com.leadstracker.leadstracker.repositories.UserRepository;
import com.leadstracker.leadstracker.request.*;
import com.leadstracker.leadstracker.response.*;
import com.leadstracker.leadstracker.controller.UserController;
import com.leadstracker.leadstracker.security.UserPrincipal;
import com.leadstracker.leadstracker.services.ClientService;
import com.leadstracker.leadstracker.services.TeamTargetService;
import com.leadstracker.leadstracker.services.UserProfileService;
import com.leadstracker.leadstracker.services.UserService;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private ModelMapper modelMapper;

    @MockitoBean
    UserRepository userRepository;

    @MockitoBean
    ClientService clientService;

    @MockitoBean
    Utils utils;

    @MockitoBean
    private GlobalExceptionHandler globalExceptionHandler;

    @MockitoBean
    private TeamTargetService teamTargetService;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserProfileService userProfileService;

    @Test
    @WithMockUser
    void createUser_ShouldReturnUserRest() throws Exception {
        UserDetails request = new UserDetails();
        request.setEmail("test@gmail.com");
        request.setPassword("password");
        request.setFirstName("John");
        request.setLastName("Doe");

        UserDto mappedDto = new UserDto();
        mappedDto.setEmail("test@gmail.com");

        UserDto createdDto = new UserDto();
        createdDto.setEmail("test@gmail.com");

        UserRest response = new UserRest();
        response.setEmail("test@gmail.com");

        when(modelMapper.map(any(UserDetails.class), eq(UserDto.class))).thenReturn(mappedDto);
        when(userService.createUser(any(UserDto.class))).thenReturn(createdDto);
        when(modelMapper.map(any(UserDto.class), eq(UserRest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/leads")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test@gmail.com"));

        verify(userService).createUser(any(UserDto.class));
    }

    @Test
    @WithMockUser
    void getUserById_ShouldReturnPerfRest() throws Exception {
        UserDto dto = new UserDto();
        dto.setUserId("abc123");
        dto.setEmail("john@gmail.com");
        dto.setRole("TEAM_LEAD");

        PerfRest perfRest = new PerfRest();
        perfRest.setEmail("john@gmail.com");

        when(userService.getUserByUserId("abc123")).thenReturn(dto);
        when(modelMapper.map(dto, PerfRest.class)).thenReturn(perfRest);

        mockMvc.perform(get("/api/v1/leads/team-leads/abc123")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("john@gmail.com"));
    }

    @Test
    @WithMockUser
    void updateUser_ShouldReturnUpdatedUser() throws Exception {
        UserDetails details = new UserDetails();
        details.setFirstName("Updated");
        details.setLastName("User");

        UserDto mappedDto = new UserDto();
        mappedDto.setFirstName("Updated");
        mappedDto.setLastName("User");

        UserDto updatedDto = new UserDto();
        updatedDto.setFirstName("Updated");
        updatedDto.setLastName("User");

        UserRest userRest = new UserRest();
        userRest.setFirstName("Updated");
        userRest.setLastName("User");

        when(modelMapper.map(any(UserDetails.class), eq(UserDto.class))).thenReturn(mappedDto);
        when(userService.updateUser(eq("abc123"), any(UserDto.class))).thenReturn(updatedDto);
        when(modelMapper.map(updatedDto, UserRest.class)).thenReturn(userRest);

        mockMvc.perform(put("/api/v1/leads/abc123")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(details)))
                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.firstName").value("Updated"))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("Changes saved successfully"))
                .andExpect(jsonPath("$.user.firstName").value("Updated"))
                .andExpect(jsonPath("$.user.lastName").value("User"));

    }


    @Test
    @WithMockUser
    void getAllUsers_ShouldReturnList() throws Exception {
        UserDto userDto = new UserDto();
        userDto.setEmail("test@gmail.com");

        UserRest userRest = new UserRest();
        userRest.setEmail("test@gmail.com");

        when(userService.getAllUsers(0, 10)).thenReturn(Arrays.asList(userDto));
        when(modelMapper.map(userDto, UserRest.class)).thenReturn(userRest);

        mockMvc.perform(get("/api/v1/leads")
                        .param("page", "0")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("test@gmail.com"));
    }

//    @Test
//    @WithMockUser
//    void forgotPassword_ShouldReturnOkResponse() throws Exception {
//        ForgotPasswordRequest request = new ForgotPasswordRequest();
//        request.setEmail("test@gmail.com");
//
//        when(userService.initiatePasswordReset("test@gmail.com")).thenReturn("vysvydss");
//        when(utils.generatePasswordResetToken()).thenReturn("dummy-token");
//
//        mockMvc.perform(post("/api/v1/leads/forgot-password-request")
//                        .with(csrf())
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(request)))
//                .andExpect(status().isOk())
//                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
//                .andExpect(jsonPath("$.message").value("Password reset instructions have been sent to your email."))
//                .andExpect(jsonPath("$.status").value("SUCCESS"))
//                .andExpect(jsonPath("$.token").exists())
//                .andExpect(jsonPath("$.token").value("dummy-token"))
//                .andDo(result -> {
//                    System.out.println("Response: " + result.getResponse().getContentAsString());
//                });
//    }



    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void resetPassword_ShouldReturnSuccessMessage() throws Exception {
        ResetPassword request = new ResetPassword();
        request.setToken("xyz123");
        request.setNewPassword("veryStrongPass2!");
        request.setConfirmNewPassword("veryStrongPass2!");

        doNothing().when(userService).resetPassword("xyz123", "veryStrongPass2!", "veryStrongPass2!");

        mockMvc.perform(post("/api/v1/leads/reset-password")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("Password reset successful."));
        ;
    }
    @Test
    @WithMockUser(roles = "ADMIN")
    void testDeleteUser_Success() throws Exception {
        String userId = "abc123";

        doNothing().when(userService).deleteUser(userId);

        mockMvc.perform(delete("/api/v1/leads/delete/{id}", userId)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User deleted successfully."));
    }

    @Test
    @WithMockUser(roles = "USER")
    void testDeleteUser_ForbiddenForNonAdmin() throws Exception {
        String userId = "abc123";

        mockMvc.perform(delete("/api/v1/leads/delete/{id}", userId))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void assignTarget_ShouldReturnResponseDto() throws Exception {
        TeamTargetRequestDto requestDto = new TeamTargetRequestDto();
        requestDto.setTeamId(1L);
        requestDto.setTargetValue(100);
        TeamTargetResponseDto responseDto = new TeamTargetResponseDto();
        responseDto.setTeamName("team123");
        responseDto.setTargetValue(100);
        responseDto.setDueDate(LocalDate.of(2025,7, 21));


        when(teamTargetService.assignTargetToTeam(any(TeamTargetRequestDto.class))).thenReturn(responseDto);

        mockMvc.perform(post("/api/v1/leads/assign/team-target")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.teamName").value("team123"))
                .andExpect(jsonPath("$.targetValue").value(100))
                .andExpect(jsonPath("$.dueDate").value(LocalDate.of(2025, 7, 21).toString()));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAllTargets_ShouldReturnListOfTargets() throws Exception {
        TeamTargetResponseDto dto = new TeamTargetResponseDto();
        dto.setTeamName("Team Alpha");
        dto.setTargetValue(500);
        dto.setDueDate(LocalDate.of(2025, 12, 31));

        List<TeamTargetResponseDto> mockResponse = List.of(dto);
        when(teamTargetService.getAllTargets()).thenReturn(mockResponse);

        mockMvc.perform(get("/api/v1/leads/targets")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();
    }

//    @Test
//    void testUpdateAdminPhoneNumber_ShouldReturnUpdatedProfile() throws Exception {
//        // Arrange: DTO to send
//        UpdateUserProfileRequestDto updateDto = new UpdateUserProfileRequestDto();
//        updateDto.setPhoneNumber("987654321");
//
//        // Arrange: Expected response
//        UserProfileResponseDto updatedResponse = new UserProfileResponseDto();
//        updatedResponse.setPhoneNumber("987654321");
//
//        // Arrange: Mock the service
//        when(userProfileService.updatePhoneNumber(eq("admin@example.com"), any(UpdateUserProfileRequestDto.class)))
//                .thenReturn(updatedResponse);
//
//        // Arrange: Create a mock UserPrincipal
//        UserEntity user = new UserEntity();
//        user.setEmail("admin@example.com");
//        RoleEntity role = new RoleEntity();
//        user.setRole(role);
//
//        UserPrincipal principal = new UserPrincipal(user);
//
//        // Act & Assert
//        mockMvc.perform(put("/api/v1/leads/changeAdminNumber/profile")
//                        .with(authentication(new UsernamePasswordAuthenticationToken(
//                                principal,
//                                null,
//                                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")))))
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(updateDto)))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.phoneNumber").value("987654321"));
//    }
//
//
//    @WithMockUser(authorities = "ROLE_ADMIN")
//    @Test
//    void testChangeAdminPassword_ShouldReturnSuccessMessage() throws Exception {
//        ChangePasswordRequestDto requestDto = new ChangePasswordRequestDto();
//        requestDto.setCurrentPassword("oldPass123");
//        requestDto.setNewPassword("newPass456");
//
//        doNothing().when(userProfileService).changePassword(eq("admin@example.com"), any(ChangePasswordRequestDto.class));
//
//        mockMvc.perform(put("/api/v1/leads/admin/profile/change-password")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(requestDto))
//                        .with(user("admin@example.com").authorities(() -> "ROLE_ADMIN")))
//                        .with(csrf())
//                .andExpect(status().isOk())
//                .andExpect(content().string("Password updated successfully."));
//    }
}

