package com.autohub.api.service;

import com.autohub.api.model.Order;
import com.autohub.api.model.OrderItem;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private static final String FROM_EMAIL = "noreply@autohub.co.zw";

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /* ---------------- ORDER CREATION ---------------- */
    @Async
    public void sendOrderReceivedEmail(Order order) {
        String subject = "Order Received #" + order.getId();
        String body =
                "Hi " + order.getUser().getFirstName() + ",\n\n" +
                        "We have received your order #" + order.getId() + ".\n" +
                        "Total amount: " + order.getTotalAmount() + "\n\n" +
                        "Please proceed with payment to continue processing.\n\n" +
                        "AutoHub Team";

        sendPlainText(order.getUser().getEmail(), subject, body);
    }

    /* ---------------- PAYMENT ---------------- */
    @Async
    public void sendOrderConfirmation(Order order) {
        String items = order.getItems().stream()
                .map(i -> i.getQuantity() + " x " + i.getPart().getName())
                .collect(Collectors.joining(", "));

        String subject = "Payment Confirmed for Order #" + order.getId();
        String body =
                "Hi " + order.getUser().getFirstName() + ",\n\n" +
                        "Your payment has been confirmed.\n\n" +
                        "Order: #" + order.getId() + "\n" +
                        "Items: " + items + "\n\n" +
                        "We are now preparing your shipment.\n\n" +
                        "AutoHub Team";

        sendPlainText(order.getUser().getEmail(), subject, body);
    }

    /* ---------------- SHIPPING ---------------- */
    @Async
    public void sendShippingNotification(Order order) {
        String subject = "Order Shipped #" + order.getId();
        String body =
                "Hi " + order.getUser().getFirstName() + ",\n\n" +
                        "Your order has been shipped.\n\n" +
                        "Courier: " + order.getCourierName() + "\n" +
                        "Tracking Number: " + order.getTrackingNumber() + "\n\n" +
                        "AutoHub Team";

        sendPlainText(order.getUser().getEmail(), subject, body);
    }

    /* ---------------- DELIVERY ---------------- */
    @Async
    public void sendDeliveryConfirmation(Order order) {
        String subject = "Order Delivered #" + order.getId();
        String body =
                "Hi " + order.getUser().getFirstName() + ",\n\n" +
                        "Your order has been delivered successfully.\n\n" +
                        "Thank you for shopping with AutoHub.";

        sendPlainText(order.getUser().getEmail(), subject, body);
    }

    /* ---------------- REFUNDS ---------------- */
    @Async
    public void sendRefundConfirmation(Order order) {
        String subject = "Refund Processed for Order #" + order.getId();
        String body =
                "Hi " + order.getUser().getFirstName() + ",\n\n" +
                        "Your refund has been processed for order #" + order.getId() + ".\n\n" +
                        "AutoHub Team";

        sendPlainText(order.getUser().getEmail(), subject, body);
    }

    /* ---------------- AUTH ---------------- */
    @Async
    public void sendPasswordResetEmail(String email, String token) {
        String subject = "Password Reset Request";
        String body =
                "You requested a password reset.\n\n" +
                        "Use this token to reset your password:\n\n" +
                        token + "\n\n" +
                        "This token will expire shortly.";

        sendPlainText(email, subject, body);
    }

    /* ---------------- INTERNAL ---------------- */

    private void sendPlainText(String to, String subject, String content) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(FROM_EMAIL);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content, false);
            mailSender.send(message);
        } catch (MessagingException e) {
            System.err.println("Email send failed: " + e.getMessage());
        }
    }
}
