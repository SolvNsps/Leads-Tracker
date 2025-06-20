package com.leadstracker.leadstracker;

import com.leadstracker.leadstracker.DTO.Utils;
import com.leadstracker.leadstracker.entities.AuthorityEntity;
import com.leadstracker.leadstracker.entities.RoleEntity;
import com.leadstracker.leadstracker.entities.UserEntity;
import com.leadstracker.leadstracker.repositories.AuthorityRepository;
import com.leadstracker.leadstracker.repositories.RoleRepository;
import com.leadstracker.leadstracker.repositories.UserRepository;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

@Component
public class InitialUsers {

    private final AuthorityRepository authorityRepository;
    private final RoleRepository roleRepository;
    private final Utils utils;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final UserRepository userRepository;

    public InitialUsers(AuthorityRepository authorityRepository, RoleRepository roleRepository, Utils utils, BCryptPasswordEncoder bCryptPasswordEncoder, UserRepository userRepository) {
        this.authorityRepository = authorityRepository;
        this.roleRepository = roleRepository;
        this.utils = utils;
        this.bCryptPasswordEncoder = bCryptPasswordEncoder;
        this.userRepository = userRepository;
    }

    @EventListener
    @Transactional
    public void onApplicationEvent(ApplicationReadyEvent event) {

        AuthorityEntity readAuthority = createAuthority("READ_AUTHORITY");
        AuthorityEntity writeAuthority = createAuthority("WRITE_AUTHORITY");
        AuthorityEntity deleteAuthority = createAuthority("DELETE_AUTHORITY");
        AuthorityEntity updateAuthority = createAuthority("UPDATE_AUTHORITY");

        RoleEntity roleAdmin = createRole("ROLE_ADMIN",
                Arrays.asList(readAuthority, writeAuthority, deleteAuthority,  updateAuthority));

        RoleEntity roleTeamLead =  createRole("ROLE_TEAM_LEAD",
                Arrays.asList(readAuthority, writeAuthority, updateAuthority));

        RoleEntity roleTeamMember = createRole("ROLE_TEAM_MEMBER",
                Arrays.asList(readAuthority, writeAuthority, updateAuthority));

        if (roleAdmin == null) {
            return;
        }

        if (userRepository.findByEmail("lordiatakyi99@gmail.com") == null) {
            UserEntity adminUser = new UserEntity();
            adminUser.setFirstName("admin");
            adminUser.setLastName("admin");
            adminUser.setEmail("lordiatakyi99@gmail.com");
            adminUser.setUserId(utils.generateUserId(50));
            adminUser.setEmailVerificationStatus(true);
            adminUser.setPassword(bCryptPasswordEncoder.encode("Xzibit5!"));
            adminUser.setRole(roleAdmin);

            userRepository.save(adminUser);

        }

        if (userRepository.findByEmail("trenyce.nd@gmail.com") == null) {
            UserEntity leadUser = new UserEntity();
            leadUser.setFirstName("lead");
            leadUser.setLastName("lead");
            leadUser.setEmail("trenyce.nd@gmail.com");
            leadUser.setUserId(utils.generateUserId(50));
            leadUser.setEmailVerificationStatus(true);
            leadUser.setPassword(bCryptPasswordEncoder.encode("3Ne.llie+"));
            leadUser.setRole(roleTeamLead);

            userRepository.save(leadUser);
        }

        if (userRepository.findByEmail("godsonsese04@gmail.com") == null) {
            UserEntity teamMemberUser = new UserEntity();
            teamMemberUser.setFirstName("teamMember");
            teamMemberUser.setLastName("teamMember");
            teamMemberUser.setEmail("godsonsese04@gmail.com");
            teamMemberUser.setUserId(utils.generateUserId(50));
            teamMemberUser.setEmailVerificationStatus(true);
            teamMemberUser.setPassword(bCryptPasswordEncoder.encode("QA1@gods"));
            teamMemberUser.setRole(roleTeamMember);

            userRepository.save(teamMemberUser);
        }

    }

    protected AuthorityEntity createAuthority(String name) {
        AuthorityEntity authority = authorityRepository.findByName(name);
        if (authority == null) {
            authority = new AuthorityEntity(name);
            authorityRepository.save(authority);
        }

        return authority;
    }

    protected RoleEntity createRole(String name, Collection<AuthorityEntity> authorities) {
        RoleEntity role = roleRepository.findByName(name);
        if (role == null) {
            role = new RoleEntity(name);
            role.setAuthorities(authorities);
            roleRepository.save(role);

        }

        return role;
    }
}
