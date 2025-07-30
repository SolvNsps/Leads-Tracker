package com.leadstracker.leadstracker.services;

import com.leadstracker.leadstracker.DTO.NotificationDto;
import com.leadstracker.leadstracker.entities.ClientEntity;
import com.leadstracker.leadstracker.entities.NotificationEntity;
import com.leadstracker.leadstracker.entities.TeamTargetEntity;
import com.leadstracker.leadstracker.entities.UserEntity;

import java.util.List;

public interface NotificationService {

    void createForwardedClientNotification(ClientEntity client, UserEntity teamLead);
    void createOverdueFollowUpNotification(ClientEntity client, UserEntity teamLead, long daysPending);
    List<NotificationDto> getUnresolvedNotifications();
    void resolveNotification(Long notificationId);
    List<NotificationEntity> getNotificationsForTeamLead(String teamLeadId);
    void alertTeamLead(Long notificationId);
    void createTeamTargetAssignedNotification(UserEntity teamLead, TeamTargetEntity target);
}
