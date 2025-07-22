package com.leadstracker.leadstracker.DTO;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClientBuilder;
import com.amazonaws.services.simpleemail.model.*;
import com.leadstracker.leadstracker.entities.ClientEntity;
import com.leadstracker.leadstracker.entities.UserEntity;
import org.springframework.stereotype.Service;

@Service("amazonSES")
public class AmazonSES {
    private final AmazonSimpleEmailService amazonSimpleEmailService;

    public AmazonSES(AmazonSimpleEmailService amazonSimpleEmailService) {
        this.amazonSimpleEmailService = amazonSimpleEmailService;
    }

    final String FROM = "tetteyabigail6@gmail.com";

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

    final String PASSWORD_RESET_REQUEST_HTMLBODY = "<div style=\"font-family: Arial, sans-serif; max-width: 600px; margin: auto; border: 1px solid #ddd; padding: 20px; border-radius: 10px;\">" +
            "<h2 style=\"color: #2c3e50;\">Password Reset Request</h2>" +
            "<p>Hi <strong>$firstName</strong>,</p>" +

            "<p>We received a request to reset your password for your account on <strong>Leads Tracker</strong>. " +
            "If you didn’t make this request, you can safely ignore this email.</p>" +

            "<p>If you did request a password reset, click the link below to set a new password:</p>" +

            "<p style=\"text-align: center;\">" +
            "<a href=\"http://localhost:4200/authentication/ResettingPasswordComponent\" " +
            "style=\"display: inline-block; padding: 12px 20px; background-color: #007bff; color: #fff; " +
            "text-decoration: none; border-radius: 5px;\">" +
            "Reset Password" +
            "</a>" +
            "</p>" +

            "<p>This link will expire in 15 minutes for your security.</p>" +

            "<p>If you have any questions, feel free to contact our support team.</p>" +

            "<p style=\"margin-top: 30px;\">Thank you,<br/>The Leads Tracker Team</p>" +
            "</div>";

    //The email body for recipients with non-HTML email clients
    final String PASSWORD_RESET_REQUEST_TEXTBODY =  "A request to reset your password.\n\n" +
            "Hi, $firstName!\n\n" +
            "Someone has requested to reset your password for your account on Leads Tracker. " +
            "If this wasn’t you, please ignore this message.\n\n" +
            "Otherwise, please open the link below in your browser to set a new password:\n\n" +
            "http://localhost:4200/authentication/ResettingPasswordComponent\n\n" +
            "This link will expire in 15 minutes for your security.\n\n" +
            "Thank you,\n" +
            "The Leads Tracker Team";

    // OTP Email Subject
    final String LOGIN_OTP_SUBJECT = "Your One-Time Password (OTP) for Leads Tracker";

    // HTML body for OTP email
    final String LOGIN_OTP_HTMLBODY = "<h1 style=\"color: #2c3e50;\">Your Login OTP</h1>"
            + "<p>Hi, $firstName!</p>"
            + "<p>Your One-Time Password (OTP) for login is: <strong>$otp</strong></p>"
            + "<p>This OTP is valid for 3 minutes. Do not share it with anyone.</p>"
            + "<br/><p>Thank you,<br/>Leads Tracker Team</p>";

    // Plain text body for OTP email
    final String LOGIN_OTP_TEXTBODY = "Your Login OTP\n\n"
            + "Hi, $firstName!\n\n"
            + "Your One-Time Password (OTP) for login is: $otp\n\n"
            + "This OTP is valid for 3 minutes. Do not share it with anyone.\n\n"
            + "Thank you,\nLeads Tracker Team";


    public void verifyEmail(UserDto userDto) {

        String htmlBodyWithToken = HTMLBODY.replace("$tokenValue", userDto.getEmailVerificationToken());
        String textBodyWithToken = TEXTBODY.replace("$tokenValue", userDto.getEmailVerificationToken());

        SendEmailRequest request = new SendEmailRequest().withDestination(new Destination().withToAddresses(userDto.getEmail()))
                .withMessage(new Message().withBody(new Body().withHtml(new Content().withCharset("UTF-8")
                        .withData(htmlBodyWithToken)).withText(new Content()
                        .withCharset("UTF-8").withData(textBodyWithToken))).withSubject(new Content().withCharset("UTF-8")
                        .withData(SUBJECT))).withSource(FROM);

        amazonSimpleEmailService.sendEmail(request);

        System.out.println("Email sent to user");
    }

    public void sendPasswordResetRequest(String firstName, String email, String token) {
        boolean returnUser = false;

//        String htmlBodyWithToken = PASSWORD_RESET_REQUEST_HTMLBODY.replace("$tokenValue", token);
        String htmlBodyWithName = PASSWORD_RESET_REQUEST_HTMLBODY.replace("$firstName", firstName).replace("$tokenValue", token);
        String textBodyWithToken = PASSWORD_RESET_REQUEST_TEXTBODY.replace("$firstName", firstName).replace("$tokenValue", token);

        SendEmailRequest request = new SendEmailRequest().withDestination(new Destination().withToAddresses(email))
                .withMessage(new Message().withBody(new Body().withHtml(new Content()
                                .withCharset("UTF-8").withData(htmlBodyWithName)).withText(new Content()
                                .withCharset("UTF-8").withData(textBodyWithToken)))
                        .withSubject(new Content().withCharset("UTF-8")
                                .withData(PASSWORD_RESET_REQUEST_SUBJECT))).withSource(FROM);

        SendEmailResult result = amazonSimpleEmailService.sendEmail(request);
        if (result != null && (result.getMessageId() != null && !result.getMessageId().isEmpty())) {
            returnUser = true;
        }

    }


    public void sendLoginOtpEmail(String firstName, String email, String otp) {

        // Replacing placeholders
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

        amazonSimpleEmailService.sendEmail(request);
        System.out.println("OTP sent to " + email);
    }
    public void sendSimpleEmail(String to, String subject, String body) {
        SendEmailRequest request = new SendEmailRequest()
                .withDestination(new Destination().withToAddresses(to))
                .withMessage(new Message()
                        .withSubject(new Content().withCharset("UTF-8").withData(subject))
                        .withBody(new Body()
                                .withText(new Content().withCharset("UTF-8").withData(body))))
                .withSource(FROM);

        amazonSimpleEmailService.sendEmail(request);
    }
    public void sendOnboardingEmail(String email, String fullName, String rawPassword) {
        String subject = "Welcome to Leads Tracker - Your Account Details";

        String body = String.format(
                "Hello %s,\n\n" +
                        "Welcome to the Leads Tracker system! Your account has been created successfully.\n\n" +
                        "You can log in using the following credentials:\n" +
                        "Email: %s\n" +
                        "Temporary Password: %s\n\n" +
                        "Please use the provided password to log in and reset your password immediately.\n\n" +
                        "Regards,\nLeads Tracker Admin Team",
                fullName, email, rawPassword
        );

        sendSimpleEmail(email, subject, body);
    }

    public void sendOverdueFollowUpEmail(UserEntity teamLead, ClientEntity client, long daysPending, UserEntity createdBy) {
        String subject = "Follow-up Required: Client " + client.getFirstName() + " Status Overdue";

        String body = "Hello " + teamLead.getFirstName() + ",\n\n" +
                "This is a reminder that a client assigned to you has remained in the " + client.getClientStatus() +
                " state beyond the allowed follow-up period.\n\n" +
                "Below are the client details for your action:\n\n" +
                "Client Details\n" +
                "Name: " + client.getFirstName() + " " + client.getLastName() + "\n" +
                "Phone Number: " + client.getPhoneNumber() + "\n" +
                "Status: " + client.getClientStatus() + "\n" +
                "Date Added: " + client.getCreatedDate() + "\n" +
                "Time Since Last Action: " + daysPending + " days\n" +
                "Forwarded By: " + (createdBy != null ? createdBy.getFirstName() + " " + createdBy.getLastName() : "N/A") + "\n\n" +
                "Why You’re Receiving This:\n" +
                "Our records show that no follow-up or status update has been made for this client in the last " + daysPending + " working days. " +
                "To maintain effective engagement and accurate reporting, we encourage you to follow up promptly.\n\n" +
                "Next Steps:\n" +
                "Please log in to your dashboard and review the client’s profile to take appropriate action.\n\n" +
                "If you’ve already addressed this, you may disregard this notice.\n\n" +
                "Kind regards,\n" +
                "System Administrator\n" +
                "Leads Tracker Team";

    }
}
