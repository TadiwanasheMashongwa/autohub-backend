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

    /** TRIGGER 1: Checkout */
    public void sendOrderReceivedEmail(Order order) {
        String subject = "Order Received - Action Required: Order #" + order.getId();
        String content = String.format(
                "<h1>Order Received</h1>" +
                        "<p>Hi %s,</p>" +
                        "<p>We've received your order <strong>#%d</strong>.</p>" +
                        "<p><strong>Status:</strong> Awaiting Payment</p>" +
                        "<p>Total Amount: $%s</p>" +
                        "<p>Please complete your payment so we can start preparing your shipment.</p>",
                order.getUser().getFirstName(), order.getId(), order.getTotalAmount()
        );
        sendHtmlEmail(order.getUser().getEmail(), subject, content);
    }

    /** TRIGGER 2: Payment Verified */
    public void sendOrderConfirmation(Order order) {
        String itemsList = order.getItems().stream()
                .map(item -> "<li>" + item.getQuantity() + "x " + item.getPart().getName() + "</li>")
                .collect(Collectors.joining());

        String subject = "Payment Confirmed - Preparing Your Order #" + order.getId();
        String content = String.format(
                "<h1>Payment Verified!</h1>" +
                        "<p>Great news, %s!</p>" +
                        "<p>We've received your payment for order <strong>#%d</strong>. Our warehouse team is now picking your items from the shelves.</p>" +
                        "<h3>Order Summary:</h3><ul>%s</ul>",
                order.getUser().getFirstName(), order.getId(), itemsList
        );
        sendHtmlEmail(order.getUser().getEmail(), subject, content);
    }

    /** TRIGGER 3: Shipped */
    public void sendShippingNotification(Order order) {
        String subject = "Your Parts are on the Way! - Order #" + order.getId();
        String content = String.format(
                "<h1>Order Shipped!</h1>" +
                        "<p>Hi %s, your order <strong>#%d</strong> has been handed over to <strong>%s</strong>.</p>" +
                        "<p>Tracking Number: <strong>%s</strong></p>",
                order.getUser().getFirstName(), order.getId(), order.getCourierName(), order.getTrackingNumber()
        );
        sendHtmlEmail(order.getUser().getEmail(), subject, content);
    }

    /** * FINAL TRIGGER 4: Delivered
     * Requests a review to boost Mike's SEO and platform trust.
     */
    public void sendDeliveryConfirmation(Order order) {
        String subject = "Delivered: How are your new parts? - Order #" + order.getId();
        String content = String.format(
                "<h1>Package Delivered!</h1>" +
                        "<p>Hi %s,</p>" +
                        "<p>According to our records, your order <strong>#%d</strong> has been successfully delivered.</p>" +
                        "<div style='border: 2px solid #007bff; padding: 20px; text-align: center; border-radius: 10px;'>" +
                        "<h3>We value your feedback!</h3>" +
                        "<p>Were the parts a perfect fit? Help other car owners by leaving a quick review.</p>" +
                        "<a href='https://autohub.co.zw/account/orders/%d' style='background: #007bff; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px;'>Leave a Review</a>" +
                        "</div>" +
                        "<br><p>Thank you for choosing AutoHub,<br>Mike & The Team</p>",
                order.getUser().getFirstName(),
                order.getId(),
                order.getId()
        );
        sendHtmlEmail(order.getUser().getEmail(), subject, content);
    }

    public void sendPasswordResetEmail(String email, String token) {
        String subject = "Password Reset Request";
        String content = "<p>Use this token: <strong>" + token + "</strong></p>";
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
            System.err.println("Email Error: " + e.getMessage());
        }
    }
}