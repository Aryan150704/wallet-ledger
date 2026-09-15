package com.aryan.ledger;

import com.aryan.ledger.domain.TransactionStatus;
import com.aryan.ledger.domain.TransactionType;
import com.aryan.ledger.domain.Wallet;
import com.aryan.ledger.dto.TransactionRequest;
import com.aryan.ledger.dto.TransactionResponse;
import com.aryan.ledger.repository.TransactionRecordRepository;
import com.aryan.ledger.repository.WalletRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("Idempotent Wallet Transaction Processor")
class TransactionProcessorIntegrationTest {

    private static final String URL = "/api/v1/transactions/process";

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private TransactionRecordRepository transactionRepository;

    @Test
    @DisplayName("Processes a single valid debit transaction successfully")
    void processesSingleValidDebit() {
        UUID userId = createWallet("1000.00");
        TransactionRequest request = debit(UUID.randomUUID(), userId, "250.00");

        ResponseEntity<TransactionResponse> response = send(request);
        BigDecimal finalBalance = balanceOf(userId);

        report("Single debit of 250.00 on a wallet with 1000.00",
                "HTTP " + response.getStatusCode().value() + ", final balance " + finalBalance);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(TransactionStatus.SUCCESS);
        assertThat(finalBalance).isEqualByComparingTo("750.00");
        assertThat(transactionRepository.findByTransactionId(request.transactionId())).isPresent();
    }

    @Test
    @DisplayName("Sends 3 identical transactionIDs simultaneously. Ensures the balance is only deducted once.")
    void duplicateTransactionIdsDeductOnce() throws Exception {
        UUID userId = createWallet("1000.00");
        UUID transactionId = UUID.randomUUID();
        List<TransactionRequest> requests = List.of(
                debit(transactionId, userId, "250.00"),
                debit(transactionId, userId, "250.00"),
                debit(transactionId, userId, "250.00"));

        List<ResponseEntity<TransactionResponse>> responses = sendConcurrently(requests);
        long succeeded = countStatus(responses, 200);
        long conflicts = countStatus(responses, 409);
        BigDecimal finalBalance = balanceOf(userId);

        report("3 simultaneous debits of 250.00 with the same transactionId on a wallet with 1000.00",
                succeeded + " succeeded, " + conflicts + " returned 409, final balance " + finalBalance);

        assertThat(succeeded).isEqualTo(1);
        assertThat(conflicts).isEqualTo(2);
        assertThat(finalBalance).isEqualByComparingTo("750.00");
    }

    @Test
    @DisplayName("Sends 10 concurrent debit requests of ₹100 for a wallet with a ₹500 balance. Ensures the final balance is exactly ₹0 and 5 requests fail with insufficient funds.")
    void concurrentDebitsNeverOverdraw() throws Exception {
        UUID userId = createWallet("500.00");
        List<TransactionRequest> requests = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            requests.add(debit(UUID.randomUUID(), userId, "100.00"));
        }

        List<ResponseEntity<TransactionResponse>> responses = sendConcurrently(requests);
        long succeeded = countStatus(responses, 200);
        long insufficient = responses.stream()
                .filter(r -> r.getStatusCode().value() == 422)
                .filter(r -> r.getBody() != null && r.getBody().status() == TransactionStatus.FAILED)
                .count();
        BigDecimal finalBalance = balanceOf(userId);

        report("10 concurrent debits of 100.00 on a wallet with 500.00",
                succeeded + " succeeded, " + insufficient + " failed with insufficient funds, final balance " + finalBalance);

        assertThat(succeeded).isEqualTo(5);
        assertThat(insufficient).isEqualTo(5);
        assertThat(finalBalance).isEqualByComparingTo("0.00");
    }

    private UUID createWallet(String balance) {
        UUID userId = UUID.randomUUID();
        walletRepository.save(new Wallet(userId, new BigDecimal(balance)));
        return userId;
    }

    private TransactionRequest debit(UUID transactionId, UUID userId, String amount) {
        return new TransactionRequest(transactionId, userId, new BigDecimal(amount), TransactionType.DEBIT);
    }

    private ResponseEntity<TransactionResponse> send(TransactionRequest request) {
        return restTemplate.postForEntity(URL, request, TransactionResponse.class);
    }

    private BigDecimal balanceOf(UUID userId) {
        return walletRepository.findById(userId).orElseThrow().getBalance();
    }

    private long countStatus(List<ResponseEntity<TransactionResponse>> responses, int status) {
        return responses.stream().filter(r -> r.getStatusCode().value() == status).count();
    }

    private List<ResponseEntity<TransactionResponse>> sendConcurrently(List<TransactionRequest> requests) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(requests.size());
        CountDownLatch startGate = new CountDownLatch(1);
        List<Future<ResponseEntity<TransactionResponse>>> futures = new ArrayList<>();
        for (TransactionRequest request : requests) {
            futures.add(executor.submit(() -> {
                startGate.await();
                return send(request);
            }));
        }
        startGate.countDown();
        List<ResponseEntity<TransactionResponse>> responses = new ArrayList<>();
        for (Future<ResponseEntity<TransactionResponse>> future : futures) {
            responses.add(future.get(30, TimeUnit.SECONDS));
        }
        executor.shutdown();
        return responses;
    }

    private void report(String intent, String result) {
        System.out.println();
        System.out.println("TEST   : " + intent);
        System.out.println("RESULT : " + result);
        System.out.println();
    }
}
