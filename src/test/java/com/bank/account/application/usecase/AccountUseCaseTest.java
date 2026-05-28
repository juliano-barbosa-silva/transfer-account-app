package com.bank.account.application.usecase;

import com.bank.account.application.dto.AccountRequest;
import com.bank.account.application.dto.AccountResponse;
import com.bank.account.application.service.AccountService;
import com.bank.account.infrastructure.persistence.AccountEntity;
import com.bank.account.infrastructure.persistence.repository.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AccountUseCaseTest {

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private AccountService accountService;

    private AccountRequest accountRequest;
    private List<AccountEntity> entityList;

    @BeforeEach
    void setUp(){
        accountRequest = new AccountRequest("Pedro", new BigDecimal("100.00"));
        entityList = List.of(
                new AccountEntity(UUID.randomUUID(), "João", new BigDecimal("500.00"), null),
                new AccountEntity(UUID.randomUUID(), "Maria", new BigDecimal("300.00"), null)
        );
    }

    @Test
    @DisplayName("Deve realizar transferência com sucesso")
    void createAccount(){
        accountService.create(accountRequest);
        verify(accountRepository, times(1)).save(any(AccountEntity.class));
        System.out.println("Teste passou: conta criada e salva no repositório com sucesso!");
    }

    @Test
    @DisplayName("Deve retornar lista de contas com sucesso")
    void shouldReturnAccountList() {

        when(accountRepository.findAll()).thenReturn(entityList);
        List<AccountResponse> result = accountService.findAll();

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("João", result.get(0).ownerName());
        assertEquals("Maria", result.get(1).ownerName());
        assertEquals(new BigDecimal("500.00"), result.get(0).balance());

        verify(accountRepository, times(1)).findAll();

        System.out.println("✅ Teste passou: lista de contas retornada com sucesso!");
    }

    @Test
    @DisplayName("Deve retornar lista vazia quando não houver contas")
    void shouldReturnEmptyList() {

        when(accountRepository.findAll()).thenReturn(List.of());
        List<AccountResponse> result = accountService.findAll();

        assertNotNull(result);
        assertTrue(result.isEmpty());

        System.out.println("✅ Teste passou: lista vazia retornada corretamente!");
    }

}
