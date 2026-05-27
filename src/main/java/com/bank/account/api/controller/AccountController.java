package com.bank.account.api.controller;

import com.bank.account.application.dto.AccountRequest;
import com.bank.account.application.dto.AccountResponse;
import com.bank.account.application.usecase.AccountUseCase;
import com.bank.account.infrastructure.persistence.AccountEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/account")
public class AccountController {

    private final AccountUseCase useCase;

    public AccountController(AccountUseCase useCase) {
        this.useCase = useCase;
    }

    @PostMapping
    public ResponseEntity<AccountEntity> create(@RequestBody @Validated AccountRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(useCase.create(request));
    }

    @GetMapping
    public List<AccountResponse> findAll(){
        return useCase.findAll();
    }

}
