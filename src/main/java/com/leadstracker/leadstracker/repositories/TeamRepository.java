package com.leadstracker.leadstracker.repositories;

import com.leadstracker.leadstracker.entities.TeamEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TeamRepository extends JpaRepository<TeamEntity, Long> {
    TeamEntity findByName(String name);

    boolean existsByName(String name);

    Optional<TeamEntity> findById(Long id);  //// Optional, but usually already inherited


}
