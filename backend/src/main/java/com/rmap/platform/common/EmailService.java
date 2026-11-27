package com.rmap.platform.common;

import com.rmap.platform.config.RmapProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.Clock;

@Service
public class EmailService {
    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final EmailDeliveryLogRepository logRepository;
    private final RmapProperties properties;
    private final Clock clock;

    public EmailService(
            JavaMailSender mailSender,
            EmailDeliveryLogRepository logRepository,
            RmapProperties properties,
            Clock clock
    ) {
        this.mailSender = mailSender;
        this.logRepository = logRepository;
        this.properties = properties;
        this.clock = clock;
    }

    public void sendOtpEmail(String recipient, String otpCode, String purpose) {
        String subject = "EMAIL_VERIFICATION".equals(purpose)
                ? "Verify your Rmap account"
                : "Reset your Rmap password";
        String body = """
                Hello,

                Your Rmap verification code is: %s

                This code expires in %d minutes. If you did not request this, you can safely ignore this email.

                — Rmap Security
                """.formatted(otpCode, properties.security().otpTtlSeconds() / 60);

        sendPlainText(recipient, subject, body, purpose);
    }

    public void sendPlainText(String recipient, String subject, String body, String templateType) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(properties.mail().from());
        message.setTo(recipient);
        message.setSubject(subject);
        message.setText(body);

        try {
            mailSender.send(message);
            logRepository.save(new EmailDeliveryLog(recipient, subject, templateType, "SENT", null, clock.instant()));
        } catch (MailException ex) {
            log.warn("SMTP delivery failed for {} (template={}): {}", recipient, templateType, ex.getMessage());
            logRepository.save(new EmailDeliveryLog(recipient, subject, templateType, "FAILED", ex.getMessage(), clock.instant()));
            throw new ApiException(
                    org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,
                    "EMAIL_DELIVERY_FAILED",
                    "Unable to send verification email at this time. Please verify SMTP/Mailpit is running."
            );
        }
    }
}
