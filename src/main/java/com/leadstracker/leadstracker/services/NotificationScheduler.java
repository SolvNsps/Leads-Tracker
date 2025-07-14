//package com.leadstracker.leadstracker.services;
//
//import com.leadstracker.leadstracker.entities.ClientEntity;
//import com.leadstracker.leadstracker.entities.UserEntity;
//import com.leadstracker.leadstracker.repositories.ClientRepository;
//import com.leadstracker.leadstracker.repositories.UserRepository;
//import com.leadstracker.leadstracker.response.Statuses;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.stereotype.Service;
//
//import java.time.LocalDate;
//import java.time.ZoneId;
//import java.time.temporal.ChronoUnit;
//import java.util.List;
//
//@Service
//public class NotificationScheduler {
//
//    @Autowired
//    private ClientRepository clientRepository;
//
//    @Autowired
//    private NotificationService notificationService;
//
//    @Autowired
//    private UserRepository userRepository;
//
//    @Scheduled(cron = "0 0 8 * * MON-FRI") // Every weekday at 8 AM
//    public void checkForOverdueClients() {
//        List<ClientEntity> clients = clientRepository.findAll();
//
//        for (ClientEntity client : clients) {
//            long daysPending = ChronoUnit.DAYS.between(client.getLastUpdated().toInstant().atZone(ZoneId.systemDefault()).toLocalDate(), LocalDate.now());
//            if (daysPending > 5 && (client.getClientStatus().equals(Statuses.PENDING)
//                    || client.getClientStatus().equals(Statuses.INTERESTED)
//                    || client.getClientStatus().equals(Statuses.AWAITING_DOCUMENTATION))) {
//
//                UserEntity teamLead = client.getTeamLead();
//                notificationService.createOverdueFollowUpNotification(client, teamLead, daysPending);
//            }
//        }
//    }
//}
//
//
