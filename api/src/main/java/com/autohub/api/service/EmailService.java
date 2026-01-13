package com.autohub.api.service;

import com.autohub.api.model.Order;
import com.autohub.api.model.OrderItem;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final String FROM_EMAIL = "noreply@autohub.co.zw";

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /** * TRIGGER 1: Checkout (Audit #1.6)
     * Sent when the order is created but not yet paid.
     */
    public void sendOrderReceivedEmail(Order order) {
        String subject = "Order Received - Action Required: Order #" + order.getId();
        String content = String.format(
                "<div style='font-family: Arial, sans-serif;'>" +
                        "<h1>Order Received</h1>" +
                        "<p>Hi %s,</p>" +
                        "<p>We've received your order <strong>#%d</strong>.</p>" +
                        "<p><strong>Status:</strong> Awaiting Payment</p>" +
                        "<p>Total Amount: $%s</p>" +
                        "<p>Please complete your payment so we can start preparing your shipment.</p>" +
                        "</div>",
                order.getUser().getFirstName(), order.getId(), order.getTotalAmount()
        );
        sendHtmlEmail(order.getUser().getEmail(), subject, content);
    }

    /** * TRIGGER 2: Payment Verified (Phase 4 Logic)
     * Sent once the payment gateway confirms the transaction.
     */
    public void sendOrderConfirmation(Order order) {
        String itemsList = order.getItems().stream()
                .map(item -> "<li>" + item.getQuantity() + "x " + item.getPart().getName() + "</li>")
                .collect(Collectors.joining());

        String subject = "Payment Confirmed - Preparing Your Order #" + order.getId();
        String content = String.format(
                "<div style='font-family: Arial, sans-serif;'>" +
                        "<h1>Payment Verified!</h1>" +
                        "<p>Great news, %s!</p>" +
                        "<p>We've received your payment for order <strong>#%d</strong>. Our warehouse team is now picking your items from the shelves.</p>" +
                        "<h3>Order Summary:</h3><ul>%s</ul>" +
                        "</div>",
                order.getUser().getFirstName(), order.getId(), itemsList
        );
        sendHtmlEmail(order.getUser().getEmail(), subject, content);
    }

    /** * TRIGGER 3: Shipped (Phase 5 Logic)
     * Sent when the Clerk enters a tracking number.
     */
    public void sendShippingNotification(Order order) {
        String subject = "Your Parts are on the Way! - Order #" + order.getId();
        String content = String.format(
                "<div style='font-family: Arial, sans-serif;'>" +
                        "<h1>Order Shipped!</h1>" +
                        "<p>Hi %s, your order <strong>#%d</strong> has been handed over to <strong>%s</strong>.</p>" +
                        "<p>Tracking Number: <strong>%s</strong></p>" +
                        "<p>You can track your package on the courier's portal.</p>" +
                        "</div>",
                order.getUser().getFirstName(), order.getId(), order.getCourierName(), order.getTrackingNumber()
        );
        sendHtmlEmail(order.getUser().getEmail(), subject, content);
    }

    /** * FINAL TRIGGER 4: Delivered (Phase 4 Review Loop)
     * This is the bridge to the Review system.
     */
    public void sendDeliveryConfirmation(Order order) {
        String subject = "Delivered: How are your new parts? - Order #" + order.getId();
        String content = String.format(
                "<div style='font-family: Arial, sans-serif;'>" +
                        "<h1>Package Delivered!</h1>" +
                        "<p>Hi %s,</p>" +
                        "<p>According to our records, your order <strong>#%d</strong> has been successfully delivered.</p>" +
                        "<div style='border: 2px solid #007bff; padding: 20px; text-align: center; border-radius: 10px;'>" +
                        "<h3>We value your feedback!</h3>" +
                        "<p>Were the parts a perfect fit? Help other car owners by leaving a quick review.</p>" +
                        "<a href='https://autohub.co.zw/account/reviews/add?partId=%d' style='background: #007bff; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px; display: inline-block;'>Leave a Review</a>" +
                        "</div>" +
                        "<br><p>Thank you for choosing AutoHub,<br>Mike & The Team</p>" +
                        "</div>",
                order.getUser().getFirstName(),
                order.getId(),
                order.getItems().get(0).getPart().getId() // Link to first item for review
        );
        sendHtmlEmail(order.getUser().getEmail(), subject, content);
    }

    /**
     * AUTH TRIGGER: Password Recovery (Audit #1.9)
     */
    public void sendPasswordResetEmail(String email, String token) {
        String subject = "Password Reset Request - AutoHub";
        String content = String.format(
                "<div style='font-family: Arial, sans-serif;'>" +
                        "<h1>Reset Your Password</h1>" +
                        "<p>We received a request to reset your password. Use the token below to complete the process:</p>" +
                        "<p style='font-size: 24px; font-weight: bold; letter-spacing: 2px; color: #007bff;'>%s</p>" +
                        "<p>This token will expire in 15 minutes.</p>" +
                        "</div>",
                token
        );
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
            // In a production environment, you would use a Logger here.
            System.err.println("Email Error: " + e.getMessage());
        }
    }
}