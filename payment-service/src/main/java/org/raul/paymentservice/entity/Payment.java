package org.raul.paymentservice.entity;

import jakarta.persistence.*;

import org.raul.paymentservice.domain.PaymentStatus;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.hibernate.generator.EventType.INSERT;

@Entity
@Table(name = "payments", schema = "public", indexes = {@Index(name = "idx_payments_order_id",
        columnList = "order_id")})
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status", nullable = false, columnDefinition = "payment_status")
    private PaymentStatus status;

    @Column(name = "reason")
    private String reason;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    @Generated(event = INSERT)
    private OffsetDateTime createdAt;

    public Payment() {}
    public Payment(Long orderId, BigDecimal amount) {
        this.orderId = orderId;
        this.amount = amount;
    }

    public Payment(Long orderId, BigDecimal amount, PaymentStatus paymentStatus) {
        this.orderId = orderId;
        this.amount = amount;
        this.status = paymentStatus;
    }

    public Payment(Long orderId, BigDecimal amount, PaymentStatus paymentStatus, String reason) {
        this.orderId = orderId;
        this.amount = amount;
        this.status = paymentStatus;
        this.reason = reason.isBlank() ? null : reason;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

}