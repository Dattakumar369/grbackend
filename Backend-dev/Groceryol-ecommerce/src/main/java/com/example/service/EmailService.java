package com.example.service;

import java.util.Random;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {
    
    private static final String FROM_EMAIL = "noreply@groceryol.com";
    
    @Autowired
    private JavaMailSender javaMailSender;

    public String generateOtp() {
        Random random = new Random();
        int otp = 100000 + random.nextInt(900000);
        return String.valueOf(otp);
    }

    public void sendOtp(String toEmail, String otp) throws MessagingException {
        String subject = "Your OTP Code";
        String content = "Your OTP for verification is: <strong>" + otp + "</strong><br><br>"
                       + "This OTP is valid for 10 minutes.";
        sendEmail(toEmail, subject, content);
    }

    public void sendRegistrationSuccessEmail(String toEmail, String name, String role) throws MessagingException {
        String subject = "Registration Successful";
        String content = "Dear " + name + ",<br><br>"
                       + "Your registration as a " + role + " with GroceryOL is successful!<br><br>"
                       + "You can now login to your account using your credentials.<br><br>"
                       + "Thank you for joining us!";
        sendEmail(toEmail, subject, content);
    }

    public void sendRegistrationFailureEmail(String toEmail, String reason) throws MessagingException {
        String subject = "Registration Issue";
        String content = "We encountered an issue with your registration:<br><br>"
                       + reason + "<br><br>"
                       + "Please try again or contact support if the problem persists.";
        sendEmail(toEmail, subject, content);
    }

    public void sendProfileUpdateEmail(String toEmail, String name) throws MessagingException {
        String subject = "Profile Updated Successfully";
        String content = "Dear " + name + ",<br><br>"
                       + "Your profile information has been successfully updated.<br><br>"
                       + "If you didn't make this change, please contact our support team immediately.";
        sendEmail(toEmail, subject, content);
    }

    public void sendPasswordChangeEmail(String toEmail, String name) throws MessagingException {
        String subject = "Password Changed Successfully";
        String content = "Dear " + name + ",<br><br>"
                       + "Your password has been successfully changed.<br><br>"
                       + "If you didn't make this change, please contact our support team immediately.";
        sendEmail(toEmail, subject, content);
    }

    public void sendAccountDeletionEmail(String toEmail, String name) throws MessagingException {
        String subject = "Account Deletion Confirmation";
        String content = "Dear " + name + ",<br><br>"
                       + "Your account has been successfully deleted from our system.<br><br>"
                       + "We're sorry to see you go. If this was a mistake, you can register again.";
        sendEmail(toEmail, subject, content);
    }

    private void sendEmail(String toEmail, String subject, String content) throws MessagingException {
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
        
        helper.setFrom(FROM_EMAIL);
        helper.setTo(toEmail);
        helper.setSubject(subject);
        helper.setText(content, true); // true indicates HTML content
        
        javaMailSender.send(mimeMessage);
    }
}