package com.lurniq.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {
    
    private final JavaMailSender javaMailSender;
    private final TemplateEngine templateEngine;
    
    @Value("${email.from:noreply@lurniq.com}")
    private String fromEmail;
    
    @Value("${email.from-name:Lurniq Team}")
    private String fromName;

    @Value("${email.activation.base-url:http://localhost:8080/auth/activate}")
    private String activationBaseUrl;
    
    @Value("${oauth2.frontend-base-url:http://localhost:4200}")
    private String frontendBaseUrl;
    
    /**
     * Send an HTML email using a Thymeleaf template
     */
    @Async
    public CompletableFuture<Void> sendHtmlEmail(String to, String subject, String templateName, Map<String, Object> templateModel) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            // Set email properties
            helper.setFrom(fromEmail, fromName);
            helper.setTo(to);
            helper.setSubject(subject);
            
            // Process the template with the model
            Context context = new Context();
            context.setVariables(templateModel);
            String htmlContent = templateEngine.process(templateName, context);
            
            // Set the HTML content
            helper.setText(htmlContent, true);
            
            // Send the email
            javaMailSender.send(message);
            
            log.info("HTML email sent successfully to: {}", to);
            return CompletableFuture.completedFuture(null);
            
        } catch (MessagingException e) {
            log.error("Failed to send HTML email to: {}. Error: {}", to, e.getMessage());
            return CompletableFuture.failedFuture(new RuntimeException("Failed to send email", e));
        } catch (MailException e) {
            log.error("Mail server error when sending email to: {}. Error: {}", to, e.getMessage());
            return CompletableFuture.failedFuture(new RuntimeException("Mail server error", e));
        } catch (Exception e) {
            log.error("Unexpected error when sending email to: {}. Error: {}", to, e.getMessage());
            return CompletableFuture.failedFuture(new RuntimeException("Failed to send email", e));
        }
    }
    
    /**
     * Send account activation email
     */
    @Async
    public CompletableFuture<Void> sendActivationEmail(String toEmail, String firstName, String activationToken) {
        try {
            String activationUrl = buildActivationUrl(activationToken);
            String subject = "Activate Your Lurniq Account";
            
            Map<String, Object> templateModel = Map.of(
                "firstName", firstName,
                "email", toEmail,
                "activationUrl", activationUrl
            );
            
            CompletableFuture<Void> result = sendHtmlEmail(toEmail, subject, "activate-account", templateModel);
            
            log.info("Activation email queued for: {}", toEmail);
            return result;
            
        } catch (Exception e) {
            log.error("Failed to send activation email to: {}. Error: {}", toEmail, e.getMessage());
            return CompletableFuture.failedFuture(new RuntimeException("Failed to send activation email", e));
        }
    }
    
    /**
     * Send password reset email
     */
    @Async
    public CompletableFuture<Void> sendPasswordResetEmail(String toEmail, String firstName, String resetToken) {
        try {
            String resetUrl = buildPasswordResetUrl(resetToken);
            String subject = "Reset Your Lurniq Password";
            
            Map<String, Object> templateModel = Map.of(
                "firstName", firstName,
                "email", toEmail,
                "resetUrl", resetUrl
            );
            
            CompletableFuture<Void> result = sendHtmlEmail(toEmail, subject, "password-reset", templateModel);
            
            log.info("Password reset email queued for: {}", toEmail);
            return result;
            
        } catch (Exception e) {
            log.error("Failed to send password reset email to: {}. Error: {}", toEmail, e.getMessage());
            return CompletableFuture.failedFuture(new RuntimeException("Failed to send password reset email", e));
        }
    }
    
    /**
     * Send welcome email after account activation
     */
    @Async
    public CompletableFuture<Void> sendWelcomeEmail(String toEmail, String firstName) {
        try {
            String subject = "Welcome to Lurniq - Let's Start Learning!";
            
            Map<String, Object> templateModel = Map.of(
                "firstName", firstName,
                "email", toEmail,
                "dashboardUrl", buildDashboardUrl()
            );
            
            CompletableFuture<Void> result = sendHtmlEmail(toEmail, subject, "welcome", templateModel);
            
            log.info("Welcome email queued for: {}", toEmail);
            return result;
            
        } catch (Exception e) {
            log.error("Failed to send welcome email to: {}. Error: {}", toEmail, e.getMessage());
            // Don't throw exception for welcome email as it's not critical
            log.warn("Welcome email failed but continuing with user activation");
            return CompletableFuture.completedFuture(null);
        }
    }
    
    private String buildActivationUrl(String token) {
        return activationBaseUrl + "?token=" + token;
    }
    
    private String buildPasswordResetUrl(String token) {
        return frontendBaseUrl + "/reset-password?token=" + token;
    }
    
    private String buildDashboardUrl() {
        return frontendBaseUrl + "/dashboard";
    }
    
    /**
     * Validate email configuration
     */
    public boolean isEmailConfigurationValid() {
        try {
            // Test if we can create a message
            javaMailSender.createMimeMessage();
            log.info("Email configuration is valid");
            return true;
        } catch (Exception e) {
            log.error("Email configuration is invalid: {}", e.getMessage());
            return false;
        }
    }
}
