package com.aryan.ledger.service;

import com.aryan.ledger.domain.TransactionRecord;
import com.aryan.ledger.domain.TransactionStatus;
import com.aryan.ledger.domain.TransactionType;
import com.aryan.ledger.domain.Wallet;
import com.aryan.ledger.dto.ProcessResult;
import com.aryan.ledger.dto.TransactionRequest;
import com.aryan.ledger.dto.TransactionResponse;
import com.aryan.ledger.repository.TransactionRecordRepository;
import com.aryan.ledger.repository.WalletRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@Service
public class TransactionService {

    private final WalletRepository walletRepository;
    private final TransactionRecordRepository transactionRepository;

    public TransactionService(WalletRepository walletRepository, TransactionRecordRepository transactionRepository) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public ProcessResult process(TransactionRequest request) {
        Wallet wallet = walletRepository.findByIdForUpdate(request.userId())
                .orElseThrow(() -> new WalletNotFoundException(request.userId()));

        Optional<TransactionRecord> existing = transactionRepository.findByTransactionId(request.transactionId());
        if (existing.isPresent()) {
            return new ProcessResult(HttpStatus.CONFLICT, TransactionResponse.from(existing.get()));
        }

        if (request.type() == TransactionType.DEBIT && wallet.getBalance().compareTo(request.amount()) < 0) {
            TransactionRecord failed = transactionRepository.save(new TransactionRecord(
                    request.transactionId(), request.userId(), request.amount(), request.type(),
                    TransactionStatus.FAILED, "Insufficient funds", wallet.getBalance()));
            return new ProcessResult(HttpStatus.UNPROCESSABLE_ENTITY, TransactionResponse.from(failed));
        }

        BigDecimal newBalance = request.type() == TransactionType.DEBIT
                ? wallet.getBalance().subtract(request.amount())
                : wallet.getBalance().add(request.amount());
        wallet.setBalance(newBalance);

        TransactionRecord success = transactionRepository.save(new TransactionRecord(
                request.transactionId(), request.userId(), request.amount(), request.type(),
                TransactionStatus.SUCCESS, "Processed", newBalance));
        return new ProcessResult(HttpStatus.OK, TransactionResponse.from(success));
    }
}
