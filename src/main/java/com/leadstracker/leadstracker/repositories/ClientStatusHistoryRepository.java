package com.leadstracker.leadstracker.repositories;

import com.leadstracker.leadstracker.entities.ClientEntity;
import com.leadstracker.leadstracker.entities.ClientStatusHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ClientStatusHistoryRepository extends JpaRepository<ClientStatusHistoryEntity, Long> {
    // fetch history for a client (newest first)
    List<ClientStatusHistoryEntity> findByClientOrderByChangedAtDesc(ClientEntity client);

    // convenience: fetch by clientId (if you prefer)
    @Query("SELECT h FROM ClientStatusHistoryEntity h " +
//            "LEFT JOIN FETCH h.changedBy  " +
            "WHERE h.client.clientId = :clientId " +
            "ORDER BY h.changedAt DESC")
    List<ClientStatusHistoryEntity> findByClient_ClientIdAndChangedByOrderByChangedAtDesc(@Param("clientId") String clientId);

    List<ClientStatusHistoryEntity> findByClient(ClientEntity client);
}