package com.kanapa4.shop.userinteraction.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "checkout_drafts")
@Getter
@Setter
@NoArgsConstructor
public class CheckoutDraft {

    @Id
    private UUID userId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private CheckoutData data;

    @UpdateTimestamp
    private Instant updatedAt;

    public CheckoutDraft(UUID userId, CheckoutData data) {
        this.userId = userId;
        this.data = data;
    }
}

