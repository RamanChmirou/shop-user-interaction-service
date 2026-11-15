package com.kanapa4.shop.userinteraction.dto;

import java.time.Instant;
import java.util.UUID;

public record InvoiceResponse(
        UUID id,
        InvoiceStatus status,
        String number,
        String pdfUrl,
        Instant issuedAt) {
}
