
package com.leadstracker.leadstracker.services.Implementations;

import com.leadstracker.leadstracker.DTO.AmazonSES;
import com.leadstracker.leadstracker.DTO.ClientDto;
import com.leadstracker.leadstracker.DTO.NotificationDto;
import com.leadstracker.leadstracker.entities.ClientEntity;
import com.leadstracker.leadstracker.entities.NotificationEntity;
import com.leadstracker.leadstracker.entities.TeamTargetEntity;
import com.leadstracker.leadstracker.entities.UserEntity;
import com.leadstracker.leadstracker.repositories.ClientRepository;
import com.leadstracker.leadstracker.repositories.NotificationRepository;
import com.leadstracker.leadstracker.response.Statuses;
import com.leadstracker.leadstracker.services.NotificationService;
import com.leadstracker.leadstracker.services.UserService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;

import static com.leadstracker.leadstracker.response.Statuses.*;

@Service
public class NotificationServiceImpl implements NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    ModelMapper modelMapper;

    @Autowired
    private AmazonSES amazonSES;

    @Override
    public void createForwardedClientNotification(ClientEntity client, UserEntity teamLead) {
        NotificationEntity notification = new NotificationEntity();
        notification.setMessage("Client " + client.getFirstName() + " " + client.getLastName() +
                " was forwarded to " + teamLead.getFirstName());
        notification.setType("FORWARDED_CLIENT");
        notification.setCreatedAt(LocalDateTime.now());
        notification.setClient(client);
        notification.setTeamLead(teamLead);

        notificationRepository.save(notification);
    }

    @Override
    public void createOverdueFollowUpNotification(ClientEntity client, UserEntity teamLead, long daysPending) {
        NotificationEntity notification = new NotificationEntity();
        notification.setMessage("Client " + client.getFirstName() + " " + client.getLastName() +
                " is still in '" + client.getClientStatus() + "' status for " + daysPending + " days.");
        notification.setType("OVERDUE_FOLLOWUP");
        notification.setCreatedAt(LocalDateTime.now());
        notification.setClient(client);
        notification.setTeamLead(teamLead);

        notificationRepository.save(notification);

        amazonSES.sendOverdueFollowUpEmail(teamLead, client, daysPending, client.getCreatedBy());

    }

    @Override
    @Scheduled(cron = "0 0 8 * * MON-FRI")
    public List<NotificationDto> getUnresolvedNotifications() {

        List<NotificationEntity> notificationEntities = notificationRepository.findByResolvedFalse();
        System.out.println("Unresolved notifications: " + notificationEntities.size());

        List<NotificationDto> filteredNotifications = new ArrayList<>();

        for (NotificationEntity notification : notificationEntities) {
            // getting the actual client from notification
            ClientEntity client = notification.getClient();
            if (client == null || client.getLastUpdated() == null) continue;

            long daysPending = ChronoUnit.DAYS.between(
                    client.getLastUpdated().toInstant().atZone(ZoneId.systemDefault()).toLocalDate(),
                    LocalDate.now()
            );

            if (daysPending > 1 && EnumSet.of(PENDING, INTERESTED, AWAITING_DOCUMENTATION).contains(client.getClientStatus())) {
                UserEntity teamLead = client.getTeamLead();
//                notificationService.createOverdueFollowUpNotification(client, teamLead, daysPending);
//                amazonSES.sendOverdueFollowUpEmail(teamLead, client, daysPending, client.getCreatedBy());

                filteredNotifications.add(modelMapper.map(notification, NotificationDto.class));
            }
        }

        return filteredNotifications;
    }

    @Override
    public void resolveNotification(Long notificationId) {
        NotificationEntity notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));

        notification.setResolved(true);

        notificationRepository.save(notification);
    }


    @Override
    public List<NotificationEntity> getNotificationsForTeamLead(String teamLeadId) {
        return notificationRepository.findByTeamLead_UserIdAndResolvedFalse(teamLeadId);
    }


    @Override
    public void alertTeamLead(Long notificationId) {
        NotificationEntity notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));

        // Fetch related client and team lead
        ClientEntity client = notification.getClient();
        UserEntity teamLead = client.getTeamLead();
        UserEntity forwardedBy = client.getCreatedBy();

        long daysSinceAction = ChronoUnit.DAYS.between(
                client.getLastUpdated().toInstant().atZone(ZoneId.systemDefault()).toLocalDate(),
                LocalDate.now()
        );

        amazonSES.sendOverdueFollowUpEmail(teamLead, client, daysSinceAction, forwardedBy);
    }

    @Override
    public void createTeamTargetAssignedNotification(UserEntity teamLead, TeamTargetEntity target) {
        NotificationEntity notification = new NotificationEntity();

        String message = "Your team has been assigned a new target of "
                + target.getTargetValue()
                + " due on "
                + target.getDueDate() + ".";

        notification.setMessage(message);
        notification.setCreatedAt(LocalDateTime.now());
        notification.setResolved(false);

        notificationRepository.save(notification);
    }

}
