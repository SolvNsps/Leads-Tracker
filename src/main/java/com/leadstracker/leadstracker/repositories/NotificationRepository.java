package com.leadstracker.leadstracker.repositories;

import com.leadstracker.leadstracker.entities.NotificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository  extends JpaRepository<NotificationEntity, Long> {
    List<NotificationEntity> findByResolvedFalse();
    List<NotificationEntity> findByTeamLeadIdAndResolvedFalse(String teamLeadId);
    void alertTeamLead(Long notificationId);
}
