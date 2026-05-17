package com.project.notificationservice.sender.impl;

import com.project.notificationservice.domain.entity.Notification;
import com.project.notificationservice.domain.enums.Channel;
import com.project.notificationservice.exception.BaseException;
import com.project.notificationservice.exception.ErrorCode;
import com.project.notificationservice.sender.NotificationSender;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationSender implements NotificationSender {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Override
    public void send(Notification notification) {
        try {
            //
            String subject = (String) notification.getPayload().getOrDefault("subject", "Thông báo mới");
            String title = (String) notification.getPayload().getOrDefault("title", "Thông báo");
            String content = (String) notification.getPayload().getOrDefault("content", "");

            Context context = new Context();
            context.setVariable("title", title);
            context.setVariable("content", content);

            String htmlContent = templateEngine.process("email/notification", context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(notification.getRecipientContact());
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Email sent successfully to: {}", notification.getRecipientContact());

        } catch (Exception exception) {
            log.error("Failed to send email to {}: {}", notification.getRecipientContact(), exception.getMessage());
            throw new BaseException(ErrorCode.EMAIL_SERVICE_ERROR);
        }
    }

    @Override
    public Channel getChannel() {
        return Channel.EMAIL;
    }
}
