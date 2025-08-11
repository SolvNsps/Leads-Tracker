package com.leadstracker.leadstracker.security;

import com.leadstracker.leadstracker.entities.AuthorityEntity;
import com.leadstracker.leadstracker.entities.RoleEntity;
import com.leadstracker.leadstracker.entities.UserEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class UserPrincipal implements UserDetails {
    UserEntity userEntity;

    public UserPrincipal(UserEntity userEntity) {
        this.userEntity = userEntity;
    }
    /**
     * @return
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<GrantedAuthority> authorities = new ArrayList<>();
        List<AuthorityEntity> authorityEntities = new ArrayList<>();

        //getting user roles
        RoleEntity role = userEntity.getRole();

        if (role != null) {
            // Adding the role itself as an authority (e.g., "ROLE_USER")
            authorities.add(new SimpleGrantedAuthority(role.getName()));

            // Adding all permissions/authorities from the role
            authorityEntities.addAll(role.getAuthorities());
        }

        authorityEntities.forEach(authorityEntity -> {
            authorities.add(new SimpleGrantedAuthority(authorityEntity.getName()));
        });

        System.out.println("Assigned authorities: " + authorities);

        return authorities;

    }

    /**
     * @return
     */
    @Override
    public String getPassword() {
        return this.userEntity.getPassword();
    }

    /**
     * @return
     */
    @Override
    public String getUsername() {
        return this.userEntity.getEmail();
    }

    /**
     * @return
     */
    @Override
    public boolean isAccountNonExpired() {
//        return UserDetails.super.isAccountNonExpired();
        return true;
    }

    /**
     * @return
     */
    @Override
    public boolean isAccountNonLocked() {
//        return UserDetails.super.isAccountNonLocked();
        return true;
    }

    /**
     * @return
     */
    @Override
    public boolean isCredentialsNonExpired() {
//        return UserDetails.super.isCredentialsNonExpired();
        return true;
    }

    /**
     * @return
     */
    @Override
    public boolean isEnabled() {
//        return UserDetails.super.isEnabled();
        return this.userEntity.isEmailVerificationStatus();
    }

    public String getId() {
        return this.userEntity.getUserId();
    }
}
