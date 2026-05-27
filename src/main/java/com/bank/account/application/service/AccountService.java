package com.bank.account.application.service;

import com.bank.account.application.dto.AccountRequest;
import com.bank.account.application.dto.AccountResponse;
import com.bank.account.application.usecase.AccountUseCase;
import com.bank.account.domain.exception.DatabaseException;
import com.bank.account.infrastructure.persistence.AccountEntity;
import com.bank.account.infrastructure.persistence.repository.AccountRepository;
import jakarta.transaction.Transactional;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class AccountService implements AccountUseCase {

    private final AccountRepository accountRepository;

    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Override
    @Transactional
    public AccountEntity create(AccountRequest request) {
        try {
            AccountEntity account = new AccountEntity(
                    UUID.randomUUID(),
                    request.ownerName(),
                    request.balance(),
                    null   // <-- sempre null na criação, JPA inicializa como 0
            );
            accountRepository.save(account);
            return account;
        } catch (
                DataIntegrityViolationException ex) {
            // violação de constraint (unique, not null, fk)
            throw new DatabaseException("Violação de integridade ao salvar transação", ex);
        } catch (
                DataAccessException ex) {
            // erro genérico de acesso ao banco
            throw new DatabaseException("Erro ao acessar o banco de dados", ex);
        }
    }

    @Override
    public List<AccountResponse> findAll() {
        try {
            List<AccountEntity> entityList = accountRepository.findAll();
            List<AccountResponse> accountResponseList =
                    entityList.stream()
                            .map(entity -> new AccountResponse(
                                    entity.getId(),
                                    entity.getOwnerName(),
                                    entity.getBalance()
                            ))
                            .toList();
            return accountResponseList;
        }catch (Exception ex){
            throw new DatabaseException("Erro ao acessar o banco de dados", ex);
        }
    }
}
