package com.bank.account.application.service;

import com.bank.account.application.dto.AccountRequest;
import com.bank.account.application.dto.TransferRequest;
import com.bank.account.application.dto.TransferResponse;
import com.bank.account.application.usecase.TransferUseCase;
import com.bank.account.domain.exception.DatabaseException;
import com.bank.account.infrastructure.kafka.TransferEventProducer;
import com.bank.account.infrastructure.persistence.AccountEntity;
import com.bank.account.infrastructure.persistence.repository.AccountRepository;
import com.bank.account.infrastructure.persistence.repository.TransactionRepository;
import com.bank.account.infrastructure.persistence.TransactionEntity;
import jakarta.transaction.Transactional;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
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
    @Transactional
    public void transfer(TransferRequest request) {

        try {
            // verificar se a transacao ja foi feita
            validateIdempotency(request.idempotencyKey());

            // verificar se a conta possui saldo
            AccountEntity from = accountRepository.findById(UUID.fromString(request.fromAccountId())).orElseThrow();
            if (from.getBalance().compareTo(BigDecimal.ZERO) < 0) {
                throw new RuntimeException("Insufficient funds");
            }

            AccountEntity to = accountRepository.findById(UUID.fromString(request.toAccountId())).orElseThrow();

            //remover saldo conta - resp operacao
            from.setBalance(from.getBalance().subtract(request.amount()));

            // creditar saldo conta - receptor
            to.setBalance(to.getBalance().add(request.amount()));

            // atualizar contas
            accountRepository.save(from);
            accountRepository.save(to);

            // registrar transacoes
            saveRegistry(from.getId(), "TRANSFER_OUT", request);
            var transferRequestIn =
                    new TransferRequest(
                            request.fromAccountId(),request.toAccountId(),request.amount(),
                            UUID.randomUUID().toString());
            saveRegistry(to.getId(), "TRANSFER_IN", transferRequestIn);

            // envio de mensageria
            producer.send(request);
        }catch (DataIntegrityViolationException ex) {
            // violação de constraint (unique, not null, fk)
            throw new DatabaseException("Violação de integridade ao salvar transação", ex);
        } catch (DataAccessException ex) {
            // erro genérico de acesso ao banco
            throw new DatabaseException("Erro ao acessar o banco de dados", ex);
        }
    }

    @Override
    public void validateIdempotency(String key) {
        Optional<String> exists =
                transactionRepository.findByIdempotencyKey(key);

        if (exists.isPresent()) throw new RuntimeException(
                "Transaction already processed"
        );
    }

    @Override
    public List<TransferResponse> findByAccountId(UUID accountId) {
        try {
            List<TransactionEntity> entityList = transactionRepository.findByAccountId(accountId);
            List<TransferResponse> transferResponseList =
                    entityList.stream()
                            .map(entity -> new TransferResponse(
                                    entity.getId().toString(),
                                    entity.getAccountId().toString(),
                                    entity.getType(),
                                    entity.getAmount(),
                                    entity.getCreatedAt().toString()
                            ))
                            .toList();
            return transferResponseList;
        }catch (DataAccessException ex) {
            throw new DatabaseException("Erro ao acessar o banco de dados", ex);
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
