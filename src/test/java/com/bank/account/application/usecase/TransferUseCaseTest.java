package com.bank.account.application.usecase;

import com.bank.account.application.dto.TransferRequest;
import com.bank.account.application.dto.TransferResponse;
import com.bank.account.application.service.TransferService;
import com.bank.account.domain.exception.DatabaseException;
import com.bank.account.infrastructure.kafka.TransferEventProducer;
import com.bank.account.infrastructure.persistence.AccountEntity;
import com.bank.account.infrastructure.persistence.TransactionEntity;
import com.bank.account.infrastructure.persistence.repository.AccountRepository;
import com.bank.account.infrastructure.persistence.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private TransferEventProducer producer;

    @InjectMocks
    private TransferService transferService;

    private UUID fromAccountId;
    private UUID toAccountId;
    private AccountEntity fromAccount;
    private AccountEntity toAccount;
    private TransferRequest request;

    @BeforeEach
    void setUp() {
        fromAccountId = UUID.randomUUID();
        toAccountId = UUID.randomUUID();

        fromAccount = new AccountEntity(fromAccountId, "João", new BigDecimal("500.00"), null);
        toAccount = new AccountEntity(toAccountId, "Maria", new BigDecimal("100.00"), null);

        request = new TransferRequest(
                fromAccountId.toString(),
                toAccountId.toString(),
                new BigDecimal("200.00"),
                UUID.randomUUID().toString()
        );
    }

    // -------------------------------------------------------
    // transfer()
    // -------------------------------------------------------

    @Test
    @DisplayName("Deve realizar transferência com sucesso")
    void shouldTransferSuccessfully() {
        when(transactionRepository.findByIdempotencyKey(request.idempotencyKey()))
                .thenReturn(Optional.empty());
        when(accountRepository.findById(fromAccountId)).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findById(toAccountId)).thenReturn(Optional.of(toAccount));

        transferService.transfer(request);

        verify(accountRepository, times(2)).save(any(AccountEntity.class));
        verify(transactionRepository, times(2)).save(any(TransactionEntity.class));
        verify(producer).send(request);

        assertEquals(new BigDecimal("300.00"), fromAccount.getBalance());
        assertEquals(new BigDecimal("300.00"), toAccount.getBalance());
    }

    @Test
    @DisplayName("Deve lançar exceção quando saldo for insuficiente")
    void shouldThrowWhenInsufficientBalance() {
        fromAccount = new AccountEntity(fromAccountId, "João", new BigDecimal("-10.00"), null);

        when(transactionRepository.findByIdempotencyKey(request.idempotencyKey()))
                .thenReturn(Optional.empty());
        when(accountRepository.findById(fromAccountId)).thenReturn(Optional.of(fromAccount));

        assertThrows(RuntimeException.class, () -> transferService.transfer(request));

        verify(accountRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
        verify(producer, never()).send(any());
    }

    @Test
    @DisplayName("Deve lançar DatabaseException em violação de constraint")
    void shouldThrowDatabaseExceptionOnDataIntegrityViolation() {
        when(transactionRepository.findByIdempotencyKey(request.idempotencyKey()))
                .thenReturn(Optional.empty());
        when(accountRepository.findById(fromAccountId)).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findById(toAccountId)).thenReturn(Optional.of(toAccount));
        when(accountRepository.save(any())).thenThrow(DataIntegrityViolationException.class);

        assertThrows(DatabaseException.class, () -> transferService.transfer(request));
    }

    @Test
    @DisplayName("Deve lançar DatabaseException em erro genérico de banco")
    void shouldThrowDatabaseExceptionOnDataAccessException() {
        when(transactionRepository.findByIdempotencyKey(request.idempotencyKey()))
                .thenReturn(Optional.empty());
        when(accountRepository.findById(fromAccountId)).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findById(toAccountId)).thenReturn(Optional.of(toAccount));
        when(accountRepository.save(any())).thenThrow(new DataAccessException("db error") {});

        assertThrows(DatabaseException.class, () -> transferService.transfer(request));
    }

    // -------------------------------------------------------
    // validateIdempotency()
    // -------------------------------------------------------

    @Test
    @DisplayName("Deve lançar exceção quando transação já foi processada")
    void shouldThrowWhenTransactionAlreadyProcessed() {
        when(transactionRepository.findByIdempotencyKey(request.idempotencyKey()))
                .thenReturn(Optional.of("exists"));

        assertThrows(RuntimeException.class,
                () -> transferService.validateIdempotency(request.idempotencyKey()));
    }

    @Test
    @DisplayName("Não deve lançar exceção quando idempotencyKey for nova")
    void shouldNotThrowWhenIdempotencyKeyIsNew() {
        when(transactionRepository.findByIdempotencyKey(request.idempotencyKey()))
                .thenReturn(Optional.empty());

        assertDoesNotThrow(() -> transferService.validateIdempotency(request.idempotencyKey()));
    }

    // -------------------------------------------------------
    // findByAccountId()
    // -------------------------------------------------------

    @Test
    @DisplayName("Deve retornar lista de transações da conta")
    void shouldReturnTransactionList() {
        TransactionEntity entity = TransactionEntity.builder()
                .id(UUID.randomUUID())
                .accountId(fromAccountId)
                .type("TRANSFER_OUT")
                .amount(new BigDecimal("200.00"))
                .createdAt(Instant.now())
                .idempotencyKey(UUID.randomUUID().toString())
                .build();

        when(transactionRepository.findByAccountId(fromAccountId)).thenReturn(List.of(entity));

        List<TransferResponse> result = transferService.findByAccountId(fromAccountId);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("TRANSFER_OUT", result.get(0).type());
    }

    @Test
    @DisplayName("Deve retornar lista vazia quando não houver transações")
    void shouldReturnEmptyListWhenNoTransactions() {
        when(transactionRepository.findByAccountId(fromAccountId)).thenReturn(List.of());

        List<TransferResponse> result = transferService.findByAccountId(fromAccountId);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Deve lançar DatabaseException quando findByAccountId falhar")
    void shouldThrowDatabaseExceptionWhenFindFails() {
        when(transactionRepository.findByAccountId(fromAccountId))
                .thenThrow(new DataAccessException("db error") {});

        assertThrows(DatabaseException.class,
                () -> transferService.findByAccountId(fromAccountId));
    }
}