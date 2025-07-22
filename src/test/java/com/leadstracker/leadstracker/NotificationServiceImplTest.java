package com.leadstracker.leadstracker;

import com.leadstracker.leadstracker.DTO.AmazonSES;
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
    private UserService userService;

    private ClientEntity mockClient;
    private UserEntity mockTeamLead;

    @BeforeEach
    void setUp() {
        mockClient = new ClientEntity();
        mockClient.setClientId("client123");
        mockClient.setFirstName("John");
        mockClient.setLastName("Doe");
        mockClient.setClientStatus(Statuses.PENDING);

        UserEntity createdBy = new UserEntity();
        createdBy.setFirstName("TeamMember");
        mockClient.setCreatedBy(createdBy);

        mockTeamLead = new UserEntity();
        mockTeamLead.setUserId("lead123");
        mockTeamLead.setFirstName("Jane");
    }

    @Test
    void testCreateForwardedClientNotification() {
        notificationService.createForwardedClientNotification(mockClient, mockTeamLead);

        verify(notificationRepository, times(1)).save(any(NotificationEntity.class));
    }

    @Test
    void testCreateOverdueFollowUpNotification() {
        long daysPending = 6L;

        notificationService.createOverdueFollowUpNotification(mockClient, mockTeamLead, daysPending);

        verify(notificationRepository, times(1)).save(any(NotificationEntity.class));
        verify(amazonSES, times(1)).sendOverdueFollowUpEmail(eq(mockTeamLead), eq(mockClient), eq(daysPending), eq(mockClient.getCreatedBy()));
    }

    @Test
    void testGetUnresolvedNotifications() {
        when(notificationRepository.findByResolvedFalse()).thenReturn(List.of(new NotificationEntity()));

        List<NotificationEntity> notifications = notificationService.getUnresolvedNotifications();

        assertNotNull(notifications);
        verify(notificationRepository, times(1)).findByResolvedFalse();
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
}
