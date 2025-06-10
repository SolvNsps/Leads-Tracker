package com.leadstracker.leadstracker.repositories;

import com.leadstracker.leadstracker.entities.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {
    UserEntity findByEmail(String email);

    UserEntity findByUserId(String userId);

    UserEntity findByPasswordResetToken(String token);

}
