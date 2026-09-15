package com.aryan.ledger.web;

import com.aryan.ledger.dto.ProcessResult;
import com.aryan.ledger.dto.TransactionRequest;
import com.aryan.ledger.dto.TransactionResponse;
import com.aryan.ledger.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping("/process")
    public ResponseEntity<TransactionResponse> process(@Valid @RequestBody TransactionRequest request) {
        ProcessResult result = transactionService.process(request);
        return ResponseEntity.status(result.status()).body(result.body());
    }
}
