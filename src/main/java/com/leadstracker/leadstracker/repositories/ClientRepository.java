package com.leadstracker.leadstracker.repositories;

import com.leadstracker.leadstracker.DTO.ClientStatusCountDto;
import com.leadstracker.leadstracker.entities.ClientEntity;
import com.leadstracker.leadstracker.entities.UserEntity;
import com.leadstracker.leadstracker.response.Statuses;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Repository
public interface ClientRepository extends JpaRepository<ClientEntity, Integer> {
    List<ClientEntity> findByCreatedByAndCreatedDateBetween(UserEntity member, Date start, Date end);

    List<ClientEntity> findByCreatedByInAndCreatedDateBetween(List<UserEntity> teamMembers, Date date, Date date1);

    ClientEntity findByClientId(String clientId);

    List<ClientEntity> findByTeamLead(UserEntity userEntity, Pageable pageable);

    List<ClientEntity> findByCreatedBy(UserEntity userEntity, Pageable pageable);

//    Page<ClientEntity> findByTeamLead(UserEntity teamLead, Pageable pageable);

    // For unpaginated fetch
    List<ClientEntity> findByTeamLead(UserEntity teamLead);
    List<ClientEntity> findByCreatedBy(UserEntity createdBy);

    // For counting
    long countByTeamLead(UserEntity teamLead);
    long countByCreatedBy(UserEntity createdBy);

    List<ClientEntity> findByCreatedByIn(List<UserEntity> users, Pageable pageable);
    List<ClientEntity> findByCreatedByIn(List<UserEntity> users);
    long countByCreatedByIn(List<UserEntity> users);

    @Query("SELECT new com.leadstracker.leadstracker.DTO.ClientStatusCountDto(c.clientStatus, COUNT(c))"
            + "FROM clients c group by c.clientStatus")
    List<ClientStatusCountDto> countClientsByStatus();

//    @Query("SELECT c FROM clients c " +
//            "WHERE (:name IS NULL OR LOWER(c.firstName || c.lastName) LIKE LOWER(CONCAT('%', :name, '%'))) " +
//            "AND (:status IS NULL OR c.clientStatus = :status) " +
//            "AND (:startDate IS NULL OR c.createdDate >= :startDate) " +
//            "AND (:endDate IS NULL OR c.createdDate <= :endDate)")
//    List<ClientEntity> searchClients(@Param("name") String name, @Param("status") Statuses status, @Param("startDate") Date startDate,
//                                     @Param("endDate") Date endDate);

    @Query("""
    SELECT c FROM clients c
    WHERE (:name IS NULL OR LOWER(CONCAT(c.firstName, ' ', c.lastName) || c.firstName || c.lastName) LIKE LOWER(CONCAT('%', :name, '%')))
      AND (:status IS NULL OR c.clientStatus = :status)
      AND (:startDate IS NULL OR c.createdDate >= :startDate)
      AND (:endDate IS NULL OR c.createdDate <= :endDate)
""")
    List<ClientEntity> searchClients(@Param("name") String name,
                                     @Param("status") Statuses status,
                                     @Param("startDate") Date startDate,
                                     @Param("endDate") Date endDate);


    List<ClientEntity> findByCreatedByIdIn(List<String> userIds);

    List<ClientEntity> findByCreatedBy_UserId(String userId);

    Page<ClientEntity> findByCreatedBy_UserId(String userId, Pageable pageable);

    Page<ClientEntity> findByCreatedByIdIn(List<String> userIds, Pageable pageable);

    Page<ClientEntity> findByTeamLead_Id(String userId, Pageable pageableRequest);

    Page<ClientEntity> findByCreatedBy_Id(String userId, Pageable pageableRequest);

    Page<ClientEntity> findByCreatedBy_UserIdIn(List<String> userIds, Pageable pageable);


    @Query("""
    SELECT c FROM clients c
    WHERE c.createdBy.userId IN :userIds
    AND (:name IS NULL OR LOWER(CONCAT(c.firstName, ' ', c.lastName)) LIKE LOWER(CONCAT('%', :name, '%')))
    AND (:status IS NULL OR c.clientStatus = :status)
    AND (:startDate IS NULL OR c.createdDate >= :startDate)
    AND (:endDate IS NULL OR c.createdDate <= :endDate)
""")
    Page<ClientEntity> searchClientsWithUserIds(
            @Param("userIds") List<String> userIds,
            @Param("name") String name,
            @Param("status") Statuses status,
            @Param("startDate") Date startDate,
            @Param("endDate") Date endDate,
            Pageable pageable
    );


}
