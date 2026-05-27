package com.bank.account.application.service;

import com.bank.account.application.dto.TransferRequest;
import com.bank.account.application.usecase.TransferUseCase;
import com.bank.account.infrastructure.kafka.TransferEventProducer;
import com.bank.account.infrastructure.persistence.AccountEntity;
import com.bank.account.infrastructure.persistence.repository.AccountRepository;
import com.bank.account.infrastructure.persistence.repository.TransactionRepository;
import com.bank.account.infrastructure.persistence.TransactionEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
public class TransferService implements TransferUseCase {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final TransferEventProducer producer;

    public TransferService(AccountRepository accountRepository, TransactionRepository transactionRepository, TransferEventProducer producer) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.producer = producer;
    }

    @Override
    public void transfer(TransferRequest request) {

        // verificar se a transacao ja foi feita
        validateIdempotency(request.idempotencyKey());

        // verificar se conta possui saldo
        AccountEntity from = accountRepository.findById(request.fromAccountId()).orElseThrow();
        if (from.getBalance().compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException("Insufficient funds");
        }

        AccountEntity to = accountRepository.findById(request.toAccountId()).orElseThrow();

        //remover saldo conta - resp operacao
        from.setBalance(from.getBalance().subtract(request.amount()));

        // creditar saldo conta - receptor
        to.setBalance(to.getBalance().add(request.amount()));

        // atualizar contas
        accountRepository.save(from);
        accountRepository.save(to);

        // registrar transacoes
        saveRegistry(from.getId(), "TRANSFER_OUT", request);
        saveRegistry(to.getId(), "TRANSFER_IN", request);

        // envio de mensageria
        producer.send(request);
    }

    @Override
    public void validateIdempotency(String key) {
        boolean exists =
                transactionRepository.findByIdempotencyKey(key);

        if (exists) {
            throw new RuntimeException(
                    "Transaction already processed"
            );
        }
    }

    private void saveRegistry(UUID accountId, String type, TransferRequest request) {
        transactionRepository.save(
                TransactionEntity.builder()
                        .id(UUID.randomUUID())
                        .accountId(accountId)
                        .amount(request.amount())
                        .type(type)
                        .createdAt(Instant.now())
                        .idempotencyKey(
                                request.idempotencyKey()
                        )
                        .build()
        );
    }
}
