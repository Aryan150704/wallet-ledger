package com.aryan.ledger.dto;

import com.aryan.ledger.domain.TransactionType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public record TransactionRequest(
        @NotNull UUID transactionId,
        @NotNull UUID userId,
        @NotNull @Positive BigDecimal amount,
        @NotNull TransactionType type) {
}
