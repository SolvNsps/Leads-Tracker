package com.leadstracker.leadstracker.repositories;

import com.leadstracker.leadstracker.entities.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {
    UserEntity findByEmail(String email);

    UserEntity findByUserId(String userId);

//<<<<<<< HEAD
    UserEntity findUserByEmailVerificationToken(String token);
//=======
    UserEntity findByPasswordResetToken(String token);

//>>>>>>> origin/jakes-branch
}
