package com.leadstracker.leadstracker.DTO;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClientBuilder;
import com.amazonaws.services.simpleemail.model.*;
import org.springframework.stereotype.Service;

@Service
public class AmazonSES {

    final String FROM = "ernest5arthur@gmail.com";

    final String SUBJECT = "One last step to complete your registration";

    final String PASSWORD_RESET_REQUEST_SUBJECT = "Welcome to Leads Tracker - Reset Your Password to Get Started";

    //HTML for the body of the email
    final String HTMLBODY = "<h1>Please verify your email address</h1>"
            + "<p>Thank you for registering with us. To complete registration process and be able to log in, "
            + " click on the following link: "
            + "<a href=http://localhost:8080/verification-service/email-verification.html?token=$tokenValue>"
            + "Final step to complete your registration</a>" + "<br/><br/>"
            + "Thank you! And we are waiting for you inside!";

    final String TEXTBODY = "Please verify your email address. "
            + "Thank you registering with our mobile app. To complete registration process and be able to log in, "
            + " open then the following URL in your browser window: "
            + " http://localhost:8080/verification-service/email-verification.html?token=$tokenValue"
            + " Thank you! And we are waiting for you inside!";

    final String PASSWORD_RESET_REQUEST_HTMLBODY = "<h1>A request to reset your password</h1>"
            + "<p>Hi, $firstName!</p> "
            + "Someone has requested to reset your password with our project. If it were not you, please ignore it."
            + " otherwise please click on the link below to set a new password: "
            + "<a href=http://localhost:8080/reset-password.html?token=$tokenValue>"
            + " Click this link to reset password"
            + "</a><br/><br/>"
            + "Thank you!";

    //The email body for recipients with non-HTML email clients
    final String PASSWORD_RESET_REQUEST_TEXTBODY = "A request to reset your password. "
            + "Hi, $firstName! "
            + "Someone has requested to reset your password with our project. If it were not you, please ignore it."
            + " otherwise please open the link below in your window browser to set a new password"
            + " http://localhost:8080/reset-password.html?token=$tokenValue>"
            + "Thank you";

    // OTP Email Subject
    final String LOGIN_OTP_SUBJECT = "Your One-Time Password (OTP) for Leads Tracker";

    // HTML body for OTP email
    final String LOGIN_OTP_HTMLBODY = "<h1>Your Login OTP</h1>"
            + "<p>Hi, $firstName!</p>"
            + "<p>Your One-Time Password (OTP) for login is: <strong>$otp</strong></p>"
            + "<p>This OTP is valid for 5 minutes. Do not share it with anyone.</p>"
            + "<br/><p>Thank you,<br/>Leads Tracker Team</p>";

    // Plain text body for OTP email
    final String LOGIN_OTP_TEXTBODY = "Your Login OTP\n\n"
            + "Hi, $firstName!\n\n"
            + "Your One-Time Password (OTP) for login is: $otp\n\n"
            + "This OTP is valid for 5 minutes. Do not share it with anyone.\n\n"
            + "Thank you,\nLeads Tracker Team";


    public void verifyEmail(UserDto userDto) {

        AmazonSimpleEmailService client = AmazonSimpleEmailServiceClientBuilder
                .standard().withRegion(Regions.US_EAST_1).build();

        String htmlBodyWithToken = HTMLBODY.replace("$tokenValue", userDto.getEmailVerificationToken());
        String textBodyWithToken = TEXTBODY.replace("$tokenValue", userDto.getEmailVerificationToken());

        SendEmailRequest request = new SendEmailRequest().withDestination(new Destination().withToAddresses(userDto.getEmail()))
                .withMessage(new Message().withBody(new Body().withHtml(new Content().withCharset("UTF-8")
                        .withData(htmlBodyWithToken)).withText(new Content()
                        .withCharset("UTF-8").withData(textBodyWithToken))).withSubject(new Content().withCharset("UTF-8")
                        .withData(SUBJECT))).withSource(FROM);

        client.sendEmail(request);

        System.out.println("Email sent to user");
    }

    public void sendPasswordResetRequest(String firstName, String email, String token) {
        boolean returnUser = false;

        AmazonSimpleEmailService client = AmazonSimpleEmailServiceClientBuilder
                .standard().withRegion(Regions.US_EAST_1).build();

        String htmlBodyWithToken = PASSWORD_RESET_REQUEST_HTMLBODY.replace("$tokenValue", token);
        String textBodyWithToken = PASSWORD_RESET_REQUEST_TEXTBODY.replace("$firstName", firstName);

        SendEmailRequest request = new SendEmailRequest().withDestination(new Destination().withToAddresses(email))
                .withMessage(new Message().withBody(new Body().withHtml(new Content()
                                .withCharset("UTF-8").withData(htmlBodyWithToken)).withText(new Content()
                                .withCharset("UTF-8").withData(textBodyWithToken)))
                        .withSubject(new Content().withCharset("UTF-8")
                                .withData(PASSWORD_RESET_REQUEST_SUBJECT))).withSource(FROM);

        SendEmailResult result = client.sendEmail(request);
        if (result != null && (result.getMessageId() != null && !result.getMessageId().isEmpty())) {
            returnUser = true;
        }

    }


    public void sendLoginOtpEmail(String firstName, String email, String otp) {
        AmazonSimpleEmailService client = AmazonSimpleEmailServiceClientBuilder
                .standard().withRegion(Regions.US_EAST_1).build();

        // Replace placeholders
        String htmlBody = LOGIN_OTP_HTMLBODY.replace("$firstName", firstName).replace("$otp", otp);

        String textBody = LOGIN_OTP_TEXTBODY.replace("$firstName", firstName).replace("$otp", otp);

        // Build and send email
        SendEmailRequest request = new SendEmailRequest()
                .withDestination(new Destination().withToAddresses(email)).withMessage(new Message()
                        .withBody(new Body().withHtml(new Content().withCharset("UTF-8")
                                        .withData(htmlBody)).withText(new Content()
                                .withCharset("UTF-8").withData(textBody)))
                        .withSubject(new Content()
                                .withCharset("UTF-8")
                                .withData(LOGIN_OTP_SUBJECT))).withSource(FROM);

        client.sendEmail(request);
        System.out.println("OTP sent to " + email);
    }

}
