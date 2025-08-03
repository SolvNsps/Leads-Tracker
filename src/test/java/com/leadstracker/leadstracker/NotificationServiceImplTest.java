package com.leadstracker.leadstracker;

import com.leadstracker.leadstracker.DTO.AmazonSES;
import com.leadstracker.leadstracker.DTO.NotificationDto;
import com.leadstracker.leadstracker.DTO.SimpleClientDto;
import com.leadstracker.leadstracker.entities.ClientEntity;
import com.leadstracker.leadstracker.entities.NotificationEntity;
import com.leadstracker.leadstracker.entities.UserEntity;
import com.leadstracker.leadstracker.repositories.NotificationRepository;
import com.leadstracker.leadstracker.response.Statuses;
import com.leadstracker.leadstracker.services.Implementations.NotificationServiceImpl;
import com.leadstracker.leadstracker.services.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class NotificationServiceImplTest {

    @InjectMocks
    private NotificationServiceImpl notificationService;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private AmazonSES amazonSES;

    @Mock
    ModelMapper modelMapper;

    @Mock
    private UserService userService;

    private NotificationEntity mockNotification;
    private ClientEntity mockClient;
    private UserEntity mockTeamLead;
    private UserEntity mockForwardedBy;

    @BeforeEach
    void setUp() {
        mockForwardedBy = new UserEntity();
        mockForwardedBy.setUserId("user1");
        mockForwardedBy.setFirstName("Peter");

        mockTeamLead = new UserEntity();
        mockTeamLead.setUserId("lead1");
        mockTeamLead.setFirstName("John");

        mockClient = new ClientEntity();
        mockClient.setClientId("client1");
        mockClient.setFirstName("Kwame");
        mockClient.setLastName("Boateng");
        mockClient.setClientStatus(Statuses.PENDING);
        mockClient.setTeamLead(mockTeamLead);
        mockClient.setCreatedBy(mockForwardedBy);
        mockClient.setLastUpdated(Date.from(LocalDate.now().minusDays(3).atStartOfDay(ZoneId.systemDefault()).toInstant()));

        mockNotification = new NotificationEntity();
        mockNotification.setId(1L);
        mockNotification.setClient(mockClient);
    }

    @Test
    void testCreateForwardedClientNotification() {
        // Prepare mock client entity
        ClientEntity mockClient = new ClientEntity();
        mockClient.setFirstName("John");

        UserEntity mockTeamLead = new UserEntity();
        mockTeamLead.setFirstName("Doe");

        UserEntity mockForwardedBy = new UserEntity();
        mockForwardedBy.setFirstName("Admin");

        SimpleClientDto clientDto = new SimpleClientDto();
        clientDto.setFirstName("John");

        NotificationDto notificationDto = new NotificationDto();
        notificationDto.setClient(clientDto); // this avoids NPE

        // Correct stubbing for modelMapper
        when(modelMapper.map(any(ClientEntity.class), eq(SimpleClientDto.class)))
                .thenReturn(clientDto);

        // Calling the method
        notificationService.createForwardedClientNotification(mockClient, mockTeamLead, mockForwardedBy);

        verify(notificationRepository, times(1)).save(any(NotificationEntity.class));
    }




    @Test
    void testCreateOverdueFollowUpNotification() {
        long daysPending = 6L;

        // Mock returned SimpleClientDto
        SimpleClientDto mockClientDto = new SimpleClientDto();
        mockClientDto.setFirstName("John");

        when(modelMapper.map(any(ClientEntity.class), eq(SimpleClientDto.class)))
                .thenReturn(mockClientDto);

        // Run the method
        notificationService.createOverdueFollowUpNotification(mockClient, mockTeamLead, daysPending);

        // Verify interactions
        verify(notificationRepository, times(1)).save(any(NotificationEntity.class));
        verify(amazonSES, times(1)).sendOverdueFollowUpEmail(eq(mockTeamLead), eq(mockClient), eq(daysPending), eq(mockClient.getCreatedBy()));
    }


    @Test
    void testResolveNotification_Success() {
        NotificationEntity notification = new NotificationEntity();
        notification.setId(1L);
        notification.setResolved(false);

        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));

        notificationService.resolveNotification(1L);

        assertTrue(notification.isResolved());
        verify(notificationRepository, times(1)).save(notification);
    }

    @Test
    void testResolveNotification_NotFound() {
        when(notificationRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> notificationService.resolveNotification(1L));

        verify(notificationRepository, never()).save(any(NotificationEntity.class));
    }

    @Test
    void testGetNotificationsForTeamLead_ShouldReturnNotifications() {
        // Arrange
        String teamLeadId = "lead1";
        List<NotificationEntity> expectedNotifications = List.of(mockNotification);

        when(notificationRepository.findByTeamLead_UserIdAndResolvedFalse(teamLeadId))
                .thenReturn(expectedNotifications);

        // Act
        List<NotificationEntity> result = notificationService.getNotificationsForTeamLead(teamLeadId);

        // Assert
        assertEquals(expectedNotifications, result);
        verify(notificationRepository, times(1)).findByTeamLead_UserIdAndResolvedFalse(teamLeadId);
    }

    @Test
    void testAlertTeamLead_ShouldSendEmail() {
        // Arrange
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(mockNotification));

        // Act
        notificationService.alertTeamLead(1L);

        // Assert
        verify(notificationRepository, times(1)).findById(1L);
        verify(amazonSES, times(1)).sendOverdueFollowUpEmail(
                eq(mockTeamLead),
                eq(mockClient),
                eq(3L),  // 3 days since last update
                eq(mockForwardedBy)
        );
    }

    @Test
    void testAlertTeamLead_NotificationNotFound_ShouldThrowException() {
        // Arrange
        when(notificationRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> notificationService.alertTeamLead(1L));
        verify(notificationRepository, times(1)).findById(1L);
        verifyNoInteractions(amazonSES);
    }
}
