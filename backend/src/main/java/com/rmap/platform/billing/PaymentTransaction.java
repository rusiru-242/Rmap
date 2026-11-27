package com.rmap.platform.billing;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "payment_transactions")
public class PaymentTransaction {
    @Id
    private String id;
    @Indexed
    private String userId;
    private String provider;
    @Indexed(unique = true)
    private String providerEventId;
    private long amountMinor;
    private String currency;
    private String status;
    private String planCode;
    @Indexed
    private Instant createdAt;

    public PaymentTransaction() {}

    public PaymentTransaction(
            String userId,
            String provider,
            String providerEventId,
            long amountMinor,
            String currency,
            String status,
            String planCode,
            Instant createdAt
    ) {
        this.userId = userId;
        this.provider = provider;
        this.providerEventId = providerEventId;
        this.amountMinor = amountMinor;
        this.currency = currency;
        this.status = status;
        this.planCode = planCode;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public String getUserId() { return userId; }
    public String getProvider() { return provider; }
    public String getProviderEventId() { return providerEventId; }
    public long getAmountMinor() { return amountMinor; }
    public String getCurrency() { return currency; }
    public String getStatus() { return status; }
    public String getPlanCode() { return planCode; }
    public Instant getCreatedAt() { return createdAt; }
}
