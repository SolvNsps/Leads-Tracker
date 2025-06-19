package com.leadstracker.leadstracker.DTO;

import com.leadstracker.leadstracker.security.SecurityConstants;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
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

//<<<<<<< HEAD
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
//=======
    public String generatePasswordResetToken() {
        return UUID.randomUUID().toString();
    }

//>>>>>>> origin/jakes-branch
}
