package com.aryan.ledger.dto;

import org.springframework.http.HttpStatus;

public record ProcessResult(HttpStatus status, TransactionResponse body) {
}
