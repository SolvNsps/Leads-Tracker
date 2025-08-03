
package com.leadstracker.leadstracker.services.Implementations;

import com.leadstracker.leadstracker.DTO.*;
import com.leadstracker.leadstracker.entities.ClientEntity;
import com.leadstracker.leadstracker.entities.NotificationEntity;
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
import java.util.stream.Collectors;

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
    public void createForwardedClientNotification(ClientEntity client, UserEntity teamLead, UserEntity forwardedBy) {
        NotificationEntity notification = new NotificationEntity();
        notification.setType("FORWARDED_CLIENT");
        notification.setClient(client);
        notification.setTeamLead(teamLead);
        notification.setAdmin(forwardedBy);
        notification.setCreatedAt(LocalDateTime.now());
        notification.setResolved(false);

        // Generating message

        NotificationDto dto = new NotificationDto();
        dto.setType("FORWARDED_CLIENT");
        SimpleClientDto simpleClientDto = modelMapper.map(client, SimpleClientDto.class);
        dto.setClient(simpleClientDto);
        dto.setForwardedBy(forwardedBy.getFirstName() + " " + forwardedBy.getLastName());
        notification.setMessage(dto.generateMessage());

        notificationRepository.save(notification);

        // Send email immediately
        amazonSES.sendNewClientForwardedEmail(teamLead, client, forwardedBy);
    }


    @Override
    public void createOverdueFollowUpNotification(ClientEntity client, UserEntity teamLead, long daysPending) {
        // Check if notification already exists
        if (notificationRepository.existsByClientAndTypeAndResolvedFalse(client, "OVERDUE_FOLLOWUP")) {
            return;
        }

        NotificationEntity notification = new NotificationEntity();
        notification.setType("OVERDUE_FOLLOWUP");
        notification.setClient(client);
        notification.setTeamLead(teamLead);
        notification.setCreatedAt(LocalDateTime.now());
        notification.setResolved(false);
        notification.setDaysOverdue(daysPending);

        // Generate consistent message
        NotificationDto dto = new NotificationDto();
        dto.setType("OVERDUE_FOLLOWUP");
        SimpleClientDto simpleClientDto = modelMapper.map(client, SimpleClientDto.class);
        dto.setClient(simpleClientDto);
        dto.setDaysOverdue(daysPending);
        notification.setMessage(dto.generateMessage());

        notificationRepository.save(notification);

        // Only send email if it's a new overdue notification
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


//        Sending in-app notification
        NotificationEntity inAppNotification = new NotificationEntity();
        inAppNotification.setClient(client);
        inAppNotification.setTeamLead(teamLead);
        inAppNotification.setMessage("Client " + client.getFirstName() + " needs your attention. Follow-up is overdue by " + daysSinceAction + " days.");
        inAppNotification.setCreatedAt(LocalDateTime.now());

        notificationRepository.save(inAppNotification);

    }

    /**
     * @param notificationId
     * @param actionTaken
     * @param newStatus
     */

    @Override
    public void handleFollowUp(Long notificationId, String actionTaken, String newStatus) {
        NotificationEntity notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));

        // Updating client status if changed
        if (newStatus != null) {
            ClientEntity client = notification.getClient();
            client.setClientStatus(Statuses.fromString(newStatus));
            client.setLastUpdated(new Date());
            clientRepository.save(client);
        }

        // Marking notification as resolved
        notification.setResolved(true);
        notification.setLastUpdated(LocalDateTime.now());
        notificationRepository.save(notification);
    }

    /**
     * @return
     */
    @Override
    public List<NotificationDto> getUnresolvedNotificationsForAdmin() {
        List<NotificationEntity> unresolvedNotifications = notificationRepository.findByResolvedFalse();

        return unresolvedNotifications.stream().map(notification -> {
            NotificationDto dto = new NotificationDto();
            dto.setId(notification.getId());
            dto.setMessage(notification.getMessage());
            dto.setResolved(notification.isResolved());
            dto.setType(notification.getType());
            dto.setCreatedAt(notification.getCreatedAt());
            dto.setLastUpdated(notification.getLastUpdated());
            dto.setActionRequired(notification.getActionRequired());
            dto.setForwardedBy(notification.getForwardedBy());
            dto.setDaysOverdue(notification.getDaysOverdue());

            // Manually mapping teamLead
            if (notification.getTeamLead() != null) {
                SimpleUserDto teamLeadDto = new SimpleUserDto();
                teamLeadDto.setUserId(notification.getTeamLead().getUserId());
                teamLeadDto.setFirstName(notification.getTeamLead().getFirstName());
                teamLeadDto.setLastName(notification.getTeamLead().getLastName());
                teamLeadDto.setEmail(notification.getTeamLead().getEmail());
                dto.setTeamLead(teamLeadDto);
            }

            // Manually mapping admin
            if (notification.getAdmin() != null) {
                SimpleUserDto adminDto = new SimpleUserDto();
                adminDto.setUserId(notification.getAdmin().getUserId());
                adminDto.setFirstName(notification.getAdmin().getFirstName());
                adminDto.setLastName(notification.getAdmin().getLastName());
                dto.setAdmin(adminDto);
            }


            // Manually mapping client
            if (notification.getClient() != null) {
                SimpleClientDto clientDto = new SimpleClientDto();
                clientDto.setClientId(notification.getClient().getClientId());
                clientDto.setFirstName(notification.getClient().getFirstName());
                clientDto.setLastName(notification.getClient().getLastName());
                clientDto.setPhoneNumber(notification.getClient().getPhoneNumber());

                Date createdDate = notification.getClient().getCreatedDate();
                if (createdDate != null) {
                    clientDto.setCreatedDate(
                            createdDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
                    );
                }


                if (notification.getClient().getClientStatus() != null) {
                    clientDto.setClientStatus(notification.getClient().getClientStatus().name());
                }
                if (notification.getClient().getCreatedBy() != null) {
                    clientDto.setUserId(notification.getClient().getCreatedBy().getUserId());
                }


                dto.setClient(clientDto);
            }

            return dto;
        }).collect(Collectors.toList());
    }

}
