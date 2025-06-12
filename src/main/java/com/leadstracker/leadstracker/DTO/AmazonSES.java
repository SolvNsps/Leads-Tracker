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

}
