package com.leadstracker.leadstracker.repositories;

import com.leadstracker.leadstracker.entities.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {
    UserEntity findByEmail(String email);

    UserEntity findByUserId(String userId);

    UserEntity findUserByEmailVerificationToken(String token);

    UserEntity findByPasswordResetToken(String token);

    List<UserEntity> findByOtpIsNotNull();

    UserEntity findByPhoneNumber(String phoneNumber);

    UserEntity findByStaffId(String staffId);
}
