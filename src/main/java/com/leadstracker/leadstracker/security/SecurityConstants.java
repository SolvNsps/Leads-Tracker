package com.leadstracker.leadstracker.security;

import com.leadstracker.leadstracker.SpringApplicationContext;
import org.springframework.core.env.Environment;

public class SecurityConstants {
    public static final long Expiration_Time_In_Seconds = 864000000;    // 10 days
    public static final long Password_Reset_Expiration_Time = 3600000; // 1 hour for password reset
    public static final String Token_Prefix = "Bearer ";
    public static final String Token_Header = "Authorization";
    public static final String Token_Secret = "bvgshg73hue7739349nfewywfw9wldsa73waada13948uewjew2d4f5z0s6xv";
    public static final String Verify_Email = "/api/v1/leads/email-verification";
    public static final String Create_User = "/api/v1/leads";
    public static final String Login = "/leads/login";
    public static final String Forgot_Password_Request = "/api/v1/leads/forgot-password-request";
    public static final String Reset_Password = "/api/v1/leads/reset-password";


    public  static String getTokenSecret() {
        Environment environment = (Environment) SpringApplicationContext.getBean("environment");
        return environment.getProperty("tokenSecret");
    }
}
