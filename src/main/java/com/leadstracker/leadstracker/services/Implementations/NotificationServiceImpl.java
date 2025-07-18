
package com.leadstracker.leadstracker.services.Implementations;

import com.leadstracker.leadstracker.DTO.AmazonSES;
import com.leadstracker.leadstracker.entities.ClientEntity;
import com.leadstracker.leadstracker.entities.NotificationEntity;
import com.leadstracker.leadstracker.entities.UserEntity;
import com.leadstracker.leadstracker.repositories.NotificationRepository;
import com.leadstracker.leadstracker.services.NotificationService;
import com.leadstracker.leadstracker.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationServiceImpl implements NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private AmazonSES amazonSES;

    @Override
    public void createForwardedClientNotification(ClientEntity client, UserEntity teamLead) {
        NotificationEntity notification = new NotificationEntity();
        notification.setMessage("Client " + client.getFirstName() + " " + client.getLastName() +
                " was forwarded to " + teamLead.getFirstName());
        notification.setType("FORWARDED_CLIENT");
        notification.setCreatedAt(LocalDateTime.now());
        notification.setClientId(client.getClientId());
        notification.setTeamLeadId(teamLead.getUserId());

        notificationRepository.save(notification);
    }

    @Override
    public void createOverdueFollowUpNotification(ClientEntity client, UserEntity teamLead, long daysPending) {
        NotificationEntity notification = new NotificationEntity();
        notification.setMessage("Client " + client.getFirstName() + " " + client.getLastName() +
                " is still in '" + client.getClientStatus() + "' status for " + daysPending + " days.");
        notification.setType("OVERDUE_FOLLOWUP");
        notification.setCreatedAt(LocalDateTime.now());
        notification.setClientId(client.getClientId());
        notification.setTeamLeadId(teamLead.getUserId());

        notificationRepository.save(notification);

        amazonSES.sendOverdueFollowUpEmail(teamLead, client, daysPending, client.getCreatedBy());
    }

    @Override
    public List<NotificationEntity> getUnresolvedNotifications() {
        return notificationRepository.findByResolvedFalse();
    }

    @Override
    public void resolveNotification(Long notificationId) {
        NotificationEntity notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));

        notification.setResolved(true);
        notificationRepository.save(notification);
    }
}
