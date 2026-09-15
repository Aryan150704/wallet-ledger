package com.aryan.ledger.dto;

import com.aryan.ledger.domain.TransactionRecord;
import com.aryan.ledger.domain.TransactionStatus;
import com.aryan.ledger.domain.TransactionType;

import java.math.BigDecimal;
import java.util.UUID;

public record TransactionResponse(
        UUID transactionId,
        UUID userId,
        BigDecimal amount,
        TransactionType type,
        TransactionStatus status,
        String message,
        BigDecimal balanceAfter) {

    public static TransactionResponse from(TransactionRecord record) {
        return new TransactionResponse(
                record.getTransactionId(),
                record.getUserId(),
                record.getAmount(),
                record.getType(),
                record.getStatus(),
                record.getMessage(),
                record.getBalanceAfter());
    }
}
