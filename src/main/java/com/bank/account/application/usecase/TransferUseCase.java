package com.bank.account.application.usecase;

import com.bank.account.application.dto.TransferRequest;
import com.bank.account.application.dto.TransferResponse;

import java.util.List;
import java.util.UUID;

public interface TransferUseCase {

    void transfer(TransferRequest request);
    void validateIdempotency(String key);
    List<TransferResponse> findByAccountId (UUID accountId);
}
