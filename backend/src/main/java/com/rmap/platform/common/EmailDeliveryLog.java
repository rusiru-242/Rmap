package com.rmap.platform.common;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "email_delivery_logs")
public class EmailDeliveryLog {
    @Id
    private String id;
    @Indexed
    private String recipient;
    private String subject;
    private String templateType;
    private String status;
    private String errorMessage;
    @Indexed
    private Instant sentAt;

    public EmailDeliveryLog() {}

    public EmailDeliveryLog(String recipient, String subject, String templateType, String status, String errorMessage, Instant sentAt) {
        this.recipient = recipient;
        this.subject = subject;
        this.templateType = templateType;
        this.status = status;
        this.errorMessage = errorMessage;
        this.sentAt = sentAt;
    }

    public String getId() { return id; }
    public String getRecipient() { return recipient; }
    public String getSubject() { return subject; }
    public String getTemplateType() { return templateType; }
    public String getStatus() { return status; }
    public String getErrorMessage() { return errorMessage; }
    public Instant getSentAt() { return sentAt; }
}
