package com.pricecompare.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.email.from:noreply@pricecompare.ro}")
    private String fromEmail;

    @Value("${app.email.from-name:PriceCompare}")
    private String fromName;

    @Async
    public void sendPriceDropAlert(String toEmail, String username,
                                    String productName, String productUrl,
                                    BigDecimal oldPrice, BigDecimal newPrice,
                                    String storeName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject("Price Drop Alert: " + productName);

            String htmlContent = buildPriceDropEmailHtml(
                    username, productName, productUrl, oldPrice, newPrice, storeName);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Price drop alert sent to {} for product: {}", toEmail, productName);
        } catch (MessagingException | java.io.UnsupportedEncodingException e) {
            log.error("Failed to send price drop alert to {}: {}", toEmail, e.getMessage());
        }
    }

    private String buildPriceDropEmailHtml(String username, String productName,
                                            String productUrl, BigDecimal oldPrice,
                                            BigDecimal newPrice, String storeName) {
        double savings = oldPrice.subtract(newPrice).doubleValue();
        double percentOff = (savings / oldPrice.doubleValue()) * 100;

        return """
                <!DOCTYPE html>
                <html>
                <head><meta charset="UTF-8"></head>
                <body style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;">
                    <div style="background: #2563eb; color: white; padding: 20px; border-radius: 8px 8px 0 0;">
                        <h1 style="margin: 0;">📉 Price Drop Alert!</h1>
                        <p style="margin: 5px 0 0;">PriceCompare</p>
                    </div>
                    <div style="background: #f8fafc; padding: 20px; border: 1px solid #e2e8f0;">
                        <p>Hi <strong>%s</strong>,</p>
                        <p>Great news! A product on your watchlist has dropped in price.</p>
                        <div style="background: white; border: 1px solid #e2e8f0; border-radius: 8px; padding: 16px; margin: 16px 0;">
                            <h2 style="color: #1e293b; margin-top: 0;">%s</h2>
                            <p style="color: #64748b;">Store: <strong>%s</strong></p>
                            <div style="display: flex; gap: 20px; align-items: center;">
                                <span style="text-decoration: line-through; color: #94a3b8; font-size: 18px;">%.2f RON</span>
                                <span style="color: #16a34a; font-size: 24px; font-weight: bold;">%.2f RON</span>
                                <span style="background: #dcfce7; color: #16a34a; padding: 4px 8px; border-radius: 4px;">-%.0f%%</span>
                            </div>
                            <p style="color: #16a34a;">You save: <strong>%.2f RON</strong></p>
                        </div>
                        <a href="%s" style="display: inline-block; background: #2563eb; color: white; padding: 12px 24px; border-radius: 6px; text-decoration: none; font-weight: bold;">
                            View Product →
                        </a>
                    </div>
                    <div style="background: #f1f5f9; padding: 12px; border-radius: 0 0 8px 8px; text-align: center; color: #64748b; font-size: 12px;">
                        You received this email because you set a price alert on PriceCompare.
                    </div>
                </body>
                </html>
                """.formatted(username, productName, storeName, oldPrice, newPrice,
                percentOff, savings, productUrl);
    }
}
