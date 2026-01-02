package com.autohub.api.service;

import com.autohub.api.model.Order;
import com.autohub.api.model.OrderItem;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendOrderConfirmation(Order order) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(order.getUser().getUsername());
        message.setSubject("AutoHub Order Confirmation - Order #" + order.getId());

        StringBuilder content = new StringBuilder();
        content.append("Hello ").append(order.getUser().getUsername()).append(",\n\n");
        content.append("Thank you for your order! Here are your order details:\n\n");
        content.append("Order ID: ").append(order.getId()).append("\n");
        content.append("Status: ").append(order.getStatus()).append("\n");
        content.append("Total Amount: $").append(order.getTotalAmount()).append("\n\n");
        content.append("Items Ordered:\n");

        for (OrderItem item : order.getItems()) {
            content.append("- ").append(item.getPart().getName())
                    .append(" (Qty: ").append(item.getQuantity()).append(")\n");
        }

        content.append("\nWe will notify you when your order status changes.");

        message.setText(content.toString());
        mailSender.send(message);
    }

    // NEW: Password Reset Email
    public void sendPasswordResetEmail(String email, String token) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("AutoHub Password Reset Request");

        String resetUrl = "http://localhost:5173/reset-password?token=" + token;

        String body = "Hello,\n\n" +
                "We received a request to reset your AutoHub password.\n" +
                "Please click the link below to set a new password. This link expires in 15 minutes.\n\n" +
                resetUrl + "\n\n" +
                "If you did not request this, please ignore this email.";

        message.setText(body);
        mailSender.send(message);
    }
}