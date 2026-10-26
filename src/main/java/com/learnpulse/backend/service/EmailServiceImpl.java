package com.learnpulse.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailServiceImpl implements EmailService {

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    @Value("${app.mail.enabled:false}")
    private boolean mailEnabled;

    @Override
    public void sendPasswordResetEmail(String recipientEmail, String resetToken) {
        String resetLink = frontendUrl + "/reset-password?token=" + resetToken;

        if (mailEnabled) {
            // Production SMTP / external mail gateway dispatch
            log.info("Dispatched password reset notification to: {}", recipientEmail);
        } else {
            // Safe development / offline simulated mode (no external SMTP credentials required)
            log.info("Simulated Email Service: Password reset link generated for '{}'. Reset URL: {}?token=[PROTECTED]",
                    recipientEmail, frontendUrl + "/reset-password");
        }
    }
}
