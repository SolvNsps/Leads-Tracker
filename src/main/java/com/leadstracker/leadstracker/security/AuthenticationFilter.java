package com.leadstracker.leadstracker.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.leadstracker.leadstracker.DTO.AmazonSES;
import com.leadstracker.leadstracker.DTO.UserDto;
import com.leadstracker.leadstracker.SpringApplicationContext;
import com.leadstracker.leadstracker.request.UserLoginRequestModel;
import com.leadstracker.leadstracker.services.UserService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public class AuthenticationFilter extends UsernamePasswordAuthenticationFilter {
    public AuthenticationFilter(AuthenticationManager authenticationManager) {
        super(authenticationManager);
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) {
        try{
            UserLoginRequestModel creds = new ObjectMapper().readValue(request.getInputStream(), UserLoginRequestModel.class);

            return getAuthenticationManager().authenticate(new UsernamePasswordAuthenticationToken(creds.getEmail(),creds.getPassword()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain, Authentication authResult)
            throws IOException, ServletException {

        byte[] secretKeyBytes = Base64.getEncoder().encode(SecurityConstants.getTokenSecret().getBytes());
        SecretKey secretKey = new SecretKeySpec(secretKeyBytes, SignatureAlgorithm.HS512.getJcaName());
        Instant now = Instant.now();


        String userName = ((UserPrincipal) authResult.getPrincipal()).getUsername();

        UserService userService = (UserService) SpringApplicationContext.getBean("userServiceImpl");
        UserDto userDto = userService.getUser(userName);

        // Generate a 6-digit OTP
        String otp = String.format("%06d", new Random().nextInt(999999));

        // Set OTP expiry (e.g., 5 minutes from now)
       userService.saveOtp(userName, otp, new Date(System.currentTimeMillis() + 300000));

        AmazonSES emailService = new AmazonSES();
        emailService.sendLoginOtpEmail(userDto.getFirstName(), userName, otp);

        // Return response
        response.setContentType("application/json");
        new ObjectMapper().writeValue(
                response.getWriter(),
                Map.of(
                        "status", "OTP_SENT",
                        "email", userName,
                        "message", "OTP sent to registered email"
                )
        );

        // Checking if password needs to be reset (first login with default password)
        boolean passwordResetRequired = userDto.isDefaultPassword(); // You'll need to add this field to your UserDto/Entity

        // Generating token
        long expirationTime = passwordResetRequired ?
                SecurityConstants.Password_Reset_Expiration_Time :
                SecurityConstants.Expiration_Time_In_Seconds;

        String token = Jwts.builder()
                .setSubject(userName)
                .setExpiration(
                        Date.from(now.plusMillis(expirationTime)))
                .setIssuedAt(Date.from(now)).signWith(secretKey, SignatureAlgorithm.HS512).compact();


        var map = Map.of("userId", userDto.getUserId(),
                "token", token, "passwordResetRequired", passwordResetRequired);
        var objectMapper = new ObjectMapper();
        var body = objectMapper.writeValueAsString(map);

        response.setContentType("application/json");
        response.getWriter().write(body);
        response.getWriter().flush();
  }

}
