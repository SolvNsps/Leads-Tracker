package com.leadstracker.leadstracker.repositories;

import com.leadstracker.leadstracker.entities.TeamsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TeamsRepository extends JpaRepository<TeamsEntity, Long> {
    TeamsEntity findByNameIgnoreCase(String teamName);

//    TeamsEntity findByTeamId(String team);
}
