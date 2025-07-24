package com.leadstracker.leadstracker.security;

import com.leadstracker.leadstracker.config.SpringApplicationContext;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.core.env.Environment;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.time.Duration;
import java.util.Base64;
import java.util.Date;

public class SecurityConstants {
    public static final long Expiration_Time_In_Seconds = 3600000;    // 1 hour
    public static final long Password_Reset_Expiration_Time = 1800000; // 30 minutes for password reset
    public static final int Max_Temp_Attempts = 2;
    public static final int Max_Perm_Attempts = 4;
    public static final long Temp_Block_Duration = 900000; // temporarily locked for 15 minutes
    public static final int Max_Resend_Attempts = 3;
    public static final Duration Resend_Cooldown = Duration.ofMinutes(15);
    public static final String Token_Prefix = "Bearer ";
    public static final String Token_Header = "Authorization";
    public static final String Token_Secret = "bvgshg73hue7739349nfewywfw9wldsa73waada13948uewjew2d4f5z0s6xv";
    public static final String Verify_Email = "/api/v1/leads/email-verification";
    public static final String Create_User = "/api/v1/leads";
    public static final String Login = "/leads/login";
    public static final String Forgot_Password_Request = "/api/v1/leads/forgot-password-request";
    public static final String Reset_Password = "/api/v1/leads/reset-password";
    public static final String Verify_OTP = "/api/v1/leads/verify-otp";
    public static final String Delete_User = "/api/v1/leads/delete/{id}";
    public static final String Resend_OTP = "/api/v1/leads/resend-otp";
//    public static final String view_Users = "/api/v1/leads/**";
    public static final String Get_A_User = "/api/v1/leads/{userId}";
    public static final String View_Team_Lead = "/api/v1/leads/team-leads/{userId}";
    public static final String All_Team_Members = "/api/v1/leads/team-members";
    public static final String Members_Under_Lead = "/api/v1/leads/team-leads/{id}/members";
    public static final String Member_Under_Lead = "/api/v1/leads/team-leads/{userId}/members/{memberId}";
    public static final String Edit_Users = "/api/v1/leads/{id}";


    public static String generateToken(String username, long expirationTimeMillis) {
        byte[] secretKeyBytes = Base64.getEncoder().encode(getTokenSecret().getBytes());
        SecretKey secretKey = new SecretKeySpec(secretKeyBytes, SignatureAlgorithm.HS512.getJcaName());

        return Jwts.builder()
                .setSubject(username)
                .setExpiration(new Date(System.currentTimeMillis() + expirationTimeMillis))
                .setIssuedAt(new Date())
                .signWith(secretKey, SignatureAlgorithm.HS512)
                .compact();
    }
    public  static String getTokenSecret() {
        Environment environment = (Environment) SpringApplicationContext.getBean("environment");
        return environment.getProperty("token.secret");
    }
}
