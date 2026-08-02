package com.project.notificationservice.sender.impl;

import com.project.notificationservice.domain.entity.Notification;
import com.project.notificationservice.domain.enums.Channel;
import com.project.notificationservice.exception.BaseException;
import com.project.notificationservice.exception.ErrorCode;
import com.project.notificationservice.repository.NotificationTemplateRepository;
import com.project.notificationservice.sender.NotificationSender;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

@Component
@Slf4j
public class EmailNotificationSender implements NotificationSender {

    private final NotificationTemplateRepository notificationTemplateRepository;
    private final JavaMailSender mailSender;
    private final SpringTemplateEngine stringTemplateEngine;

    private final String fromEmail;

    // IDE báo lỗi "Could not autowire. No beans of 'JavaMailSender' type found." là sai ---> ẩn
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    public EmailNotificationSender(
            NotificationTemplateRepository notificationTemplateRepository,
            JavaMailSender mailSender,
            @Qualifier("stringTemplateEngine") SpringTemplateEngine stringTemplateEngine,
            @Value("${spring.mail.username}") String fromEmail) {

        this.notificationTemplateRepository = notificationTemplateRepository;
        this.mailSender = mailSender;
        this.stringTemplateEngine = stringTemplateEngine;
        this.fromEmail = fromEmail;
    }

    @Override
    public void send(Notification notification) {
        try {
            // tìm template tương ứng với eventType trong notification_entity (== template_code trong template_entity)
            var template = notificationTemplateRepository
                    .findByTemplateCodeAndChannelAndIsActiveTrue(notification.getEventType(), notification.getChannel())
                    .orElseThrow(() -> new BaseException(ErrorCode.TEMPLATE_NOT_FOUND));

            // tạo thymeleaf object
            Context context = new Context();
            context.setVariables(notification.getPayload()); // set payload vào

            // lấy subject + map variable tương ứng từ context
            String subject = stringTemplateEngine.process(template.getSubjectTemplate(), context);
            String htmlContent = stringTemplateEngine.process(template.getBodyTemplate(), context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(notification.getRecipientContact());
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Email sent successfully to: {}", notification.getRecipientContact());

        } catch (BaseException exception) {

            log.error("Failed to send email with error: {}", exception.getMessage());
            throw exception;
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
