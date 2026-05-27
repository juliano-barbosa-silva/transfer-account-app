package com.bank.account.application.usecase;

import com.bank.account.application.dto.TransferRequest;

public interface TransferUseCase {

    void transfer(TransferRequest request);
    void validateIdempotency(String key);
}
