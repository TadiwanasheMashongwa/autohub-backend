package com.autohub.api.service;

import com.autohub.api.model.Order;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final String FROM_EMAIL = "noreply@autohub.co.zw";

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * PHASE 6, STEP 1: Trigger 1 - Checkout
     * "Order Received - Awaiting Payment"
     */
    public void sendOrderReceivedEmail(Order order) {
        String subject = "Order Received - Action Required: Order #" + order.getId();
        String content = String.format(
                "<h1>Order Received</h1>" +
                        "<p>Hi %s,</p>" +
                        "<p>We've received your order for spare parts! Your order ID is <strong>#%d</strong>.</p>" +
                        "<p><strong>Status:</strong> Awaiting Payment</p>" +
                        "<p>Total Amount: $%s</p>" +
                        "<p>Please proceed to the payment gateway to complete your purchase so we can begin picking your items.</p>" +
                        "<br><p>Best Regards,<br>AutoHub Team</p>",
                order.getUser().getFirstName(),
                order.getId(),
                order.getTotalAmount().toString()
        );
        sendHtmlEmail(order.getUser().getEmail(), subject, content);
    }

    /**
     * Placeholder for Phase 6, Step 2: Payment Verified
     */
    public void sendOrderConfirmation(Order order) {
        String subject = "Payment Verified - Order #" + order.getId();
        String content = "<h1>Payment Received!</h1><p>We are now picking your items in the warehouse.</p>";
        sendHtmlEmail(order.getUser().getEmail(), subject, content);
    }

    /**
     * Placeholder for Phase 6, Step 3: Shipped
     */
    public void sendShippingNotification(Order order) {
        String subject = "Order Shipped! - Order #" + order.getId();
        String content = String.format(
                "<h1>Your Parts are on the Way!</h1>" +
                        "<p>Courier: %s</p><p>Tracking Number: %s</p>",
                order.getCourierName(),
                order.getTrackingNumber()
        );
        sendHtmlEmail(order.getUser().getEmail(), subject, content);
    }

    /**
     * Placeholder for Phase 6, Step 4: Delivered
     */
    public void sendDeliveryConfirmation(Order order) {
        String subject = "Package Delivered - Order #" + order.getId();
        String content = "<h1>Enjoy your parts!</h1><p>Please rate your experience on our platform.</p>";
        sendHtmlEmail(order.getUser().getEmail(), subject, content);
    }

    public void sendPasswordResetEmail(String email, String token) {
        String subject = "Password Reset Request";
        String content = "<p>Use this token to reset your password: <strong>" + token + "</strong></p>";
        sendHtmlEmail(email, subject, content);
    }

    private void sendHtmlEmail(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(FROM_EMAIL);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            mailSender.send(message);
        } catch (MessagingException e) {
            System.err.println("Failed to send email to " + to + ": " + e.getMessage());
        }
    }
}