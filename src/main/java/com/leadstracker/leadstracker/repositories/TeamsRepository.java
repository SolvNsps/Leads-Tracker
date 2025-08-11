package com.leadstracker.leadstracker.repositories;

import com.leadstracker.leadstracker.entities.TeamsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TeamsRepository extends JpaRepository<TeamsEntity, Long> {
    TeamsEntity findByNameIgnoreCase(String teamName);

    List<TeamsEntity> findAllByActiveTrue();

    List<TeamsEntity> findByNameContainingIgnoreCaseAndActiveTrue(String search);

    List<TeamsEntity> findByNameContainingIgnoreCase(String name);

//    TeamsEntity findByTeamId(String team);
}
