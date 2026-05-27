package com.bank.account.application.usecase;

import com.bank.account.application.dto.AccountRequest;
import com.bank.account.application.dto.AccountResponse;
import com.bank.account.infrastructure.persistence.AccountEntity;

import java.util.List;

public interface AccountUseCase {

    AccountEntity create(AccountRequest request);
    List<AccountResponse> findAll();
}
