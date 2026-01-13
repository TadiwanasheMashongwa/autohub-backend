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

    /**
     * PHASE 6, STEP 1: Trigger 1 - Checkout
     */
    public void sendOrderReceivedEmail(Order order) {
        String subject = "Order Received - Action Required: Order #" + order.getId();
        String content = String.format(
                "<h1>Order Received</h1>" +
                        "<p>Hi %s,</p>" +
                        "<p>We've received your order <strong>#%d</strong>.</p>" +
                        "<p><strong>Status:</strong> Awaiting Payment</p>" +
                        "<p>Total Amount: $%s</p>" +
                        "<p>Please complete your payment so we can start preparing your shipment.</p>" +
                        "<br><p>Best Regards,<br>AutoHub Team</p>",
                order.getUser().getFirstName(), order.getId(), order.getTotalAmount()
        );
        sendHtmlEmail(order.getUser().getEmail(), subject, content);
    }

    /**
     * PHASE 6, STEP 2: Trigger 2 - Payment Verified
     * Updates the customer that picking has started.
     */
    public void sendOrderConfirmation(Order order) {
        String itemsList = order.getItems().stream()
                .map(item -> "<li>" + item.getQuantity() + "x " + item.getPart().getName() + "</li>")
                .collect(Collectors.joining());

        String subject = "Payment Confirmed - Preparing Your Order #" + order.getId();
        String content = String.format(
                "<h1>Payment Verified!</h1>" +
                        "<p>Great news, %s!</p>" +
                        "<p>We've received your payment for order <strong>#%d</strong>. Our warehouse team is now picking your items from the shelves.</p>" +
                        "<h3>Order Summary:</h3>" +
                        "<ul>%s</ul>" +
                        "<p>You will receive another update as soon as your parts are with the courier.</p>" +
                        "<br><p>Best Regards,<br>AutoHub Warehouse Team</p>",
                order.getUser().getFirstName(), order.getId(), itemsList
        );
        sendHtmlEmail(order.getUser().getEmail(), subject, content);
    }

    /**
     * PHASE 6, STEP 3: Trigger 3 - Shipped
     */
    public void sendShippingNotification(Order order) {
        String subject = "Your Parts are Shipped! - Order #" + order.getId();
        String content = String.format(
                "<h1>On the Way!</h1>" +
                        "<p>Your order has been handed over to <strong>%s</strong>.</p>" +
                        "<p>Tracking Number: <strong>%s</strong></p>" +
                        "<p>You can track your package on the courier's website.</p>",
                order.getCourierName(), order.getTrackingNumber()
        );
        sendHtmlEmail(order.getUser().getEmail(), subject, content);
    }

    /**
     * PHASE 6, STEP 4: Trigger 4 - Delivered
     */
    public void sendDeliveryConfirmation(Order order) {
        String subject = "Delivered: Order #" + order.getId();
        String content = "<h1>Package Delivered!</h1>" +
                "<p>We hope the parts are exactly what you needed. Please log in to leave a review!</p>";
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
            System.err.println("SMTP Error: " + e.getMessage());
        }
    }
}