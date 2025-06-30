package com.leadstracker.leadstracker;


import com.leadstracker.leadstracker.DTO.AmazonSES;
import com.leadstracker.leadstracker.DTO.UserDto;
import com.leadstracker.leadstracker.DTO.Utils;
import com.leadstracker.leadstracker.entities.RoleEntity;
import com.leadstracker.leadstracker.entities.UserEntity;
import com.leadstracker.leadstracker.repositories.RoleRepository;
import com.leadstracker.leadstracker.repositories.UserRepository;
import com.leadstracker.leadstracker.services.Implementations.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @InjectMocks
    private UserServiceImpl userService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private Utils utils;

    @Mock
    private BCryptPasswordEncoder bCryptPasswordEncoder;

    @Mock
    private AmazonSES amazonSES;

    @Mock
    private ModelMapper modelMapper;

    private UserDto userDto;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        userDto = new UserDto();
        userDto.setEmail("test@example.com");
        userDto.setFirstName("Test");
        userDto.setLastName("User");
        userDto.setPassword("password123");
        userDto.setRole("test role");
    }

    @Test
    void saveUser_shouldSaveAndReturnUser_whenUserIsNew() {
        userDto.setPassword("password123");
        // Arrange
        when(userRepository.findByEmail(userDto.getEmail())).thenReturn(null);
        when(utils.generateUserId(anyInt())).thenReturn("randomUserId");
        when(utils.generateDefaultPassword()).thenReturn("randomUserId");
        when(bCryptPasswordEncoder.encode(anyString())).thenReturn("encodedPassword");
        userDto.setPhoneNumber("1234567890");
        userDto.setStaffId("123456");


        var role = new RoleEntity();
        role.setId(1L);
        role.setName("test role");

        UserEntity mockSavedEntity = new ModelMapper().map(userDto, UserEntity.class);
        mockSavedEntity.setUserId("randomUserId");
        mockSavedEntity.setPassword("encodedPassword");
        mockSavedEntity.setPhoneNumber("1234567890");
        mockSavedEntity.setRole(role);
        mockSavedEntity.setOtpFailedAttempts(0);

        when(roleRepository.findByName(anyString())).thenReturn(role);
        when(userRepository.save(Mockito.<UserEntity>any())).thenReturn(mockSavedEntity);


        // Act
        UserDto savedUser = userService.createUser(userDto);

        // Assert
        assertNotNull(savedUser);
        assertEquals(userDto.getEmail(), savedUser.getEmail());
        verify(userRepository).save(any(UserEntity.class));
        verify(utils).generateUserId(30);
    }

    @Test
    void saveUser_shouldThrowException_whenUserAlreadyExists() {
        // Arrange
        when(userRepository.findByEmail(userDto.getEmail())).thenReturn(new UserEntity());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> userService.createUser(userDto));
        assertEquals("400 BAD_REQUEST \"Email already exists\"", exception.getMessage());
        verify(userRepository, never()).save(any(UserEntity.class));
    }

    @Test
    void testGetUserByUserId_UserFound() {
        // Arrange
        String userId = "abc123";
        UserEntity mockUserEntity = new UserEntity();
        mockUserEntity.setUserId(userId);
        mockUserEntity.setEmail("test@example.com");
        mockUserEntity.setFirstName("John");
        mockUserEntity.setLastName("Doe");

        when(userRepository.findByUserId(userId)).thenReturn(mockUserEntity);

        // Act
        UserDto result = userService.getUserByUserId(userId);

        // Assert
        assertNotNull(result);
        assertEquals(userId, result.getUserId());
        assertEquals("test@example.com", result.getEmail());
        assertEquals("John", result.getFirstName());
        assertEquals("John", result.getFirstName());
        assertEquals("Doe", result.getLastName());
        verify(userRepository).findByUserId(userId);
    }

    @Test
    void testGetUserByUserId_UserNotFound_ShouldThrowException() {
        // Arrange
        String userId = "nonexistent";
        when(userRepository.findByUserId(userId)).thenReturn(null);

        // Act & Assert
        UsernameNotFoundException exception = assertThrows(UsernameNotFoundException.class, () -> {
            userService.getUserByUserId(userId);
        });

        assertTrue(exception.getMessage().contains(userId));
        verify(userRepository).findByUserId(userId);
    }

    @Test
    void testGetUser_UserFound() {
        // Arrange
        String email = "test@example.com";
        UserEntity mockUserEntity = new UserEntity();
        mockUserEntity.setUserId("abc123");
        mockUserEntity.setEmail(email);
        mockUserEntity.setFirstName("Jane");
        mockUserEntity.setLastName("Smith");


        when(userRepository.findByEmail(email)).thenReturn(mockUserEntity);

        // Act
        UserDto result = userService.getUser(email);

        // Assert
        assertNotNull(result);
        assertEquals("abc123", result.getUserId());
        assertEquals(email, result.getEmail());
        assertEquals("Jane", result.getFirstName());
        assertEquals("Smith", result.getLastName());
        verify(userRepository).findByEmail(email);
    }

    @Test
    void testGetUser_UserNotFound_ShouldThrowException() {
        // Arrange
        String email = "notfound@example.com";
        when(userRepository.findByEmail(email)).thenReturn(null);

        // Act & Assert
        UsernameNotFoundException exception = assertThrows(UsernameNotFoundException.class, () -> {
            userService.getUser(email);
        });

        assertEquals(email, exception.getMessage());
        verify(userRepository).findByEmail(email);
    }

    @Test
    void testUpdateUser_Success() {
        String userId = "abc123";

        // Setup mock existing user entity
        UserEntity existingUser = new UserEntity();
        existingUser.setUserId(userId);
        existingUser.setFirstName("OldFirst");
        existingUser.setLastName("OldLast");

        // Setup updated info from UserDto
        UserDto updateDto = new UserDto();
        updateDto.setFirstName("NewFirst");
        updateDto.setLastName("NewLast");

        UserEntity updatedEntity = new UserEntity();
        updatedEntity.setUserId(userId);
        updatedEntity.setFirstName("NewFirst");
        updatedEntity.setLastName("NewLast");

        when(userRepository.findByUserId(userId)).thenReturn(existingUser);
        when(userRepository.save(any(UserEntity.class))).thenReturn(updatedEntity);

        // Act
        UserDto result = userService.updateUser(userId, updateDto);

        // Assert
        assertEquals("NewFirst", result.getFirstName());
        assertEquals("NewLast", result.getLastName());
        verify(userRepository).save(any(UserEntity.class));
    }
    @Test
    void testUpdateUser_UserNotFound() {
        String userId = "nonexistent";

        when(userRepository.findByUserId(userId)).thenReturn(null);

        UserDto updateDto = new UserDto();
        updateDto.setFirstName("NewFirst");
        updateDto.setLastName("NewLast");

        assertThrows(UsernameNotFoundException.class, () -> {
            userService.updateUser(userId, updateDto);
        });

        verify(userRepository, never()).save(any(UserEntity.class));
    }

    @Test
    void testGetAllUsers_Success() {
        int page = 1;
        int limit = 2;

        // Sample user entities
        UserEntity user1 = new UserEntity();
        user1.setUserId("id1");
        user1.setFirstName("Alice");

        UserEntity user2 = new UserEntity();
        user2.setUserId("id2");
        user2.setFirstName("Bob");

        List<UserEntity> users = List.of(user1, user2);
        Page<UserEntity> userPage = new PageImpl<>(users);

        when(userRepository.findAll(PageRequest.of(page - 1, limit))).thenReturn(userPage);

        // Act
        List<UserDto> result = userService.getAllUsers(page, limit);

        // Assert
        assertEquals(2, result.size());
        assertEquals("Alice", result.get(0).getFirstName());
        assertEquals("Bob", result.get(1).getFirstName());
        verify(userRepository).findAll(PageRequest.of(page - 1, limit));
    }

    @Test
    void testInitiatePasswordReset_Success() {
        String email = "test@example.com";
        UserEntity user = new UserEntity();
        user.setEmail(email);

        when(userRepository.findByEmail(email)).thenReturn(user);
        when(utils.generatePasswordResetToken()).thenReturn("reset-token");

        boolean result = userService.initiatePasswordReset(email);

        assertTrue(result);
        assertNotNull(user.getPasswordResetToken());
        assertNotNull(user.getPasswordResetExpiration());

        verify(userRepository).save(user);
    }

    @Test
    void testInitiatePasswordReset_UserNotFound() {
        String email = "notfound@example.com";

        when(userRepository.findByEmail(email)).thenReturn(null);

        boolean result = userService.initiatePasswordReset(email);

        assertFalse(result);
        verify(userRepository, never()).save(any());
    }

    @Test
    void testResetPassword_Success() {
        String token = "valid-token";
        String newPassword = "newPassword123";
        String confirmNewPassword = "newPassword123";

        UserEntity user = new UserEntity();
        user.setPasswordResetToken(token);
        user.setPasswordResetExpiration(new Date(System.currentTimeMillis() + 3600000)); // 1 hour ahead

        when(userRepository.findByPasswordResetToken(token)).thenReturn(user);
        when(bCryptPasswordEncoder.encode(newPassword)).thenReturn("encodedPassword");

        userService.resetPassword(token, newPassword, confirmNewPassword);

        assertEquals("encodedPassword", user.getPassword());
        assertNull(user.getPasswordResetToken());
        assertNull(user.getPasswordResetExpiration());

        verify(userRepository).save(user);
    }

    @Test
    void testResetPassword_InvalidToken_ThrowsException() {
        String token = "invalid-token";

        when(userRepository.findByPasswordResetToken(token)).thenReturn(null);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            userService.resetPassword(token, "newPass", "newPass");
        });

        assertEquals("400 BAD_REQUEST \"Invalid password reset token\"", exception.getMessage());
    }

    @Test
    void testResetPassword_TokenExpired_ThrowsException() {
        String token = "expired-token";
        UserEntity user = new UserEntity();
        user.setPasswordResetToken(token);
        user.setPasswordResetExpiration(new Date(System.currentTimeMillis() - 1000)); // expired

        when(userRepository.findByPasswordResetToken(token)).thenReturn(user);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            userService.resetPassword(token, "newPass", "newPass");
        });

        assertEquals("400 BAD_REQUEST \"Password reset token has expired\"", exception.getMessage());
    }

    @Test
    void testResetPassword_PasswordsDoNotMatch_ThrowsException() {
        String token = "token-mismatch";
        UserEntity user = new UserEntity();
        user.setPasswordResetToken(token);
        user.setPasswordResetExpiration(new Date(System.currentTimeMillis() + 3600000)); // not expired

        when(userRepository.findByPasswordResetToken(token)).thenReturn(user);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            userService.resetPassword(token, "newPass", "differentPass");
        });

        assertEquals("400 BAD_REQUEST \"Passwords do not match\"", exception.getMessage());
    }

    @Test
    void testLoadUserByUsername_UserExists_ReturnsUserPrincipal() {
        String email = "test@example.com";
        UserEntity mockUser = new UserEntity();
        mockUser.setEmail(email);
        mockUser.setPassword("encodedPassword");

        when(userRepository.findByEmail(email)).thenReturn(mockUser);

        UserDetails userDetails = userService.loadUserByUsername(email);

        assertNotNull(userDetails);
        assertEquals(email, userDetails.getUsername());
        assertEquals("encodedPassword", userDetails.getPassword());
        assertTrue(userDetails.getAuthorities().isEmpty());
    }

    @Test
    void testLoadUserByUsername_UserNotFound_ThrowsException() {
        String email = "nonexistent@example.com";

        when(userRepository.findByEmail(email)).thenReturn(null);

        UsernameNotFoundException exception = assertThrows(UsernameNotFoundException.class, () -> {
            userService.loadUserByUsername(email);
        });

        assertEquals(email, exception.getMessage());
    }


}
