package com.learnpulse.backend.service;

public interface EmailService {

    /**
     * Dispatches a password reset email to the recipient with the secure reset link.
     *
     * @param recipientEmail destination email address
     * @param resetToken     raw cryptographically secure token (used to construct the reset link)
     */
    void sendPasswordResetEmail(String recipientEmail, String resetToken);
}
