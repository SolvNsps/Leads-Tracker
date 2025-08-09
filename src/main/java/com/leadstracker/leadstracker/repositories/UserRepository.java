package com.leadstracker.leadstracker.repositories;

import com.leadstracker.leadstracker.entities.RoleEntity;
import com.leadstracker.leadstracker.entities.TeamsEntity;
import com.leadstracker.leadstracker.entities.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {
    UserEntity findByEmail(String email);

    UserEntity findByUserId(String userId);

    UserEntity findUserByEmailVerificationToken(String token);

    UserEntity findByPasswordResetToken(String token);

    List<UserEntity> findByOtpIsNotNull();

    UserEntity findByPhoneNumber(String phoneNumber);

    UserEntity findByStaffId(String staffId);

    List<UserEntity> findByTeamLead_UserId(String id);

    Optional<Object> findByUserIdAndTeamLead_UserId(String memberId, String userId);

    List<UserEntity> findByTeamLeadIsNotNull();

    List<UserEntity> findByTeamLead(UserEntity teamLead);

    List<UserEntity> findByRole(RoleEntity role);

    RoleEntity role(RoleEntity role);

    List<UserEntity> findByTeamLeadNotNull();

    Page<UserEntity> findByTeamLeadId(UserEntity userEntity, Pageable pageable);

    Page<UserEntity> findByTeamLead_UserId(String userId, Pageable pageable);


    List<UserEntity> findByTeam(TeamsEntity team);

    @Query("SELECT u FROM users u WHERE u.role.name = :roleName")
    List<UserEntity> findByRoleName(@Param("roleName") String roleName);

    List<UserEntity> findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCaseOrEmailStartingWithIgnoreCase(
            String keyword, String keyword1, String keyword2);

    List<UserEntity> findByCreatedDateBetween(LocalDateTime localDateTime, LocalDateTime localDateTime1);

    List<UserEntity> findByEmailContainingIgnoreCase(String keyword);
}
