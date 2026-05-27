package com.bank.account.application.dto;

import java.math.BigDecimal;

public record TransferRequest(String fromAccountId,
                              String toAccountId,
                              BigDecimal amount,
                              String idempotencyKey) {
}
