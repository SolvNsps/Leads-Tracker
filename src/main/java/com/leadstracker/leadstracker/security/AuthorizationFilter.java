package com.leadstracker.leadstracker.security;

import com.leadstracker.leadstracker.entities.UserEntity;
import com.leadstracker.leadstracker.repositories.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class AuthorizationFilter extends BasicAuthenticationFilter {
    private final UserRepository userRepository;

    public AuthorizationFilter(AuthenticationManager authenticationManager, UserRepository userRepository) {
        super(authenticationManager);
        this.userRepository = userRepository;
    }

    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {


        String header = request.getHeader(SecurityConstants.Token_Header);

        if (header == null || !header.startsWith(SecurityConstants.Token_Prefix)) {
            chain.doFilter(request, response);
            return;
        }

        UsernamePasswordAuthenticationToken authentication = getAuthentication(request);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        chain.doFilter(request, response);
    }

    private UsernamePasswordAuthenticationToken getAuthentication(HttpServletRequest request) {

        String authorizationHeader = request.getHeader(SecurityConstants.Token_Header);

        if (authorizationHeader == null) {
            return null;
        }

        String token = authorizationHeader.replace(SecurityConstants.Token_Prefix, "");

        byte[] secretKeyBytes = Base64.getEncoder().encode(SecurityConstants.getTokenSecret().getBytes());
        SecretKey key = Keys.hmacShaKeyFor(secretKeyBytes);

        //method for parsing JWT
        JwtParser parser = Jwts.parser().setSigningKey(key).build();

        Claims claims = parser.parseClaimsJws(token).getBody(); //correct parsing
        String user = claims.getSubject();

        if(user == null) {
            return null;
        }

        // Get role and authorities from token claims
        String role = claims.get("roles", String.class);
        List<String> authorities = claims.get("authorities", List.class);

        List<GrantedAuthority> grantedAuthorities = new ArrayList<>();

        // Adding role with ROLE_ prefix
        if (role != null) {
            grantedAuthorities.add(new SimpleGrantedAuthority(role));
        }

        // Add individual authorities
        if (authorities != null) {
            authorities.forEach(auth ->
                    grantedAuthorities.add(new SimpleGrantedAuthority(auth))
            );
        }

        System.out.println("Token granted authorities: " + grantedAuthorities);
//       return new UsernamePasswordAuthenticationToken(user, null, grantedAuthorities);

//
        UserEntity userEntity = userRepository.findByEmail(user);
        UserPrincipal userPrincipal = new UserPrincipal(userEntity);

        return new UsernamePasswordAuthenticationToken(userPrincipal, null, userPrincipal.getAuthorities());
    }
}


