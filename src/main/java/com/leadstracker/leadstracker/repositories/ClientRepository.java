package com.leadstracker.leadstracker.repositories;

import com.leadstracker.leadstracker.entities.ClientEntity;
import com.leadstracker.leadstracker.entities.UserEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface ClientRepository extends JpaRepository<ClientEntity, Integer> {
    List<ClientEntity> findByCreatedByAndCreatedDateBetween(UserEntity member, Date start, Date end);

    List<ClientEntity> findByCreatedByInAndCreatedDateBetween(List<UserEntity> teamMembers, Date date, Date date1);

    ClientEntity findByClientId(String clientId);

    List<ClientEntity> findByTeamLead(UserEntity userEntity, Pageable pageable);

    List<ClientEntity> findByCreatedBy(UserEntity userEntity, Pageable pageable);

//    List<ClientEntity> findByLastUpdated();
//
//    List<ClientEntity> lastUpdated(Date lastUpdated);
}
