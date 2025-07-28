package com.leadstracker.leadstracker.DTO;

import com.leadstracker.leadstracker.security.SecurityConstants;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Date;
import java.util.Random;
import java.util.UUID;

@Component
public class Utils {

    private final Random  random = new SecureRandom();

    @Value("bvgshg73hue7739349nfewywfw9wldsa73waada13948uewjew2d4f5z0s6xv")
    private String tokenSecret;

    public String generateUserId(int length) {
        return generateRandomString(length);
    }

    private String generateRandomString(int length) {
        StringBuilder randomString = new StringBuilder();

        for (int i = 0; i < length; i++) {
            String alphabet = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
            randomString.append(alphabet.charAt(random.nextInt(alphabet.length())));
        }
        return new String(randomString);
    }

    public boolean hasTokenExpired(String token) {
        byte[] secretKeyBytes = Base64.getEncoder().encode(SecurityConstants.getTokenSecret().getBytes());
        SecretKey secretKey = new SecretKeySpec(secretKeyBytes, SignatureAlgorithm.HS512.getJcaName());

        JwtParser parser =  Jwts.parser().verifyWith(secretKey).build();

        Claims claims = parser.parseClaimsJws(token).getBody();

        Date tokenExpirationDate = claims.getExpiration();
        Date todayDate =  new Date();

        return tokenExpirationDate.before(todayDate);
    }

    public String generateEmailVerificationToken(String userId) {

        String token = Jwts.builder()
                .setSubject(userId)
                .setExpiration(
                        new Date(System.currentTimeMillis() + SecurityConstants.Expiration_Time_In_Seconds))
                .signWith(SignatureAlgorithm.HS256, tokenSecret)
                .compact();
        return token;
    }
    public String generatePasswordResetToken() {
        return UUID.randomUUID().toString();
    }

    private String generatePassword(int length) {
        StringBuilder randomString = new StringBuilder();

        for (int i = 0; i < length; i++) {
            String alphabet = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[!@#$%^&*+]).{8,}$";
            randomString.append(alphabet.charAt(random.nextInt(alphabet.length())));
        }
        return new String(randomString);
    }
    public String generateDefaultPassword() {
        return generatePassword(10);
    }


    public String getExactDuration(Duration duration) {
        long seconds = duration.getSeconds();

        if (seconds < SecurityConstants.Minute_In_Seconds) {
            return "Just now";
        }
        else if (seconds < SecurityConstants.Hour_In_Seconds) {
            return (seconds / SecurityConstants.Minute_In_Seconds) + " minutes";
        }
        else if (seconds < SecurityConstants.Day_In_Seconds) {
            return (seconds / SecurityConstants.Hour_In_Seconds) + " hours";
        }
        else if (seconds < SecurityConstants.Month_In_Seconds) {
            return (seconds / SecurityConstants.Day_In_Seconds) + " days";
        }
        else if (seconds < SecurityConstants.Year_In_Seconds) {
            return (seconds / SecurityConstants.Month_In_Seconds) + " months";
        }
        else {
            return (seconds / SecurityConstants.Year_In_Seconds) + " years";
        }
    }


}
