package com.leadstracker.leadstracker.security;

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

    public AuthorizationFilter(AuthenticationManager authenticationManager) {
        super(authenticationManager);
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
        String subject = claims.getSubject();

        if(subject == null) {
            return null;
        }


        List<GrantedAuthority> authorities = new ArrayList<>();
        Object rolesObject = claims.get("roles");

        if(rolesObject instanceof List<?>) {
            List<?> rolesList = (List<?>) rolesObject;
            if (rolesList.isEmpty()) {
                //assign default role if roles list is empty
                authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
            }
            else {
                for (Object role : rolesList) {
                    authorities.add(new SimpleGrantedAuthority(role.toString()));
                }
            }

        }

        else {
            //assign default role if "roles" claim is missing or not a list
            authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        }
        return new UsernamePasswordAuthenticationToken(subject, null, authorities);
    }
}


