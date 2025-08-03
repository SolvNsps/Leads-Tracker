package com.leadstracker.leadstracker.services;

import com.leadstracker.leadstracker.entities.ClientEntity;
import com.leadstracker.leadstracker.entities.UserEntity;
import com.leadstracker.leadstracker.repositories.ClientRepository;
import com.leadstracker.leadstracker.repositories.UserRepository;
import com.leadstracker.leadstracker.response.Statuses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class NotificationScheduler {

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private UserRepository userRepository;

    @Scheduled(cron = "0 0 8 * * MON-FRI") // Weekdays at 8 AM
    public void checkForOverdueClients() {
        List<ClientEntity> clients = clientRepository.findAll();

        for (ClientEntity client : clients) {
            // Skip if no team lead assigned
            if (client.getTeamLead() == null) continue;

            LocalDate lastUpdated = client.getLastUpdated().toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();

            long daysPending = ChronoUnit.DAYS.between(lastUpdated, LocalDate.now());

            // Check for overdue status (3 days for PENDING, 5 days for others)
            boolean isOverdue = false;
            if (client.getClientStatus() == Statuses.PENDING && daysPending >= 3) {
                isOverdue = true;
            } else if ((client.getClientStatus() == Statuses.INTERESTED ||
                    client.getClientStatus() == Statuses.AWAITING_DOCUMENTATION) &&
                    daysPending >= 5) {
                isOverdue = true;
            }

            if (isOverdue) {
                notificationService.createOverdueFollowUpNotification(
                        client,
                        client.getTeamLead(),
                        daysPending
                );
            }
        }
    }
}


