package com.bank.account.application.dto;

import java.math.BigDecimal;

public record TransferResponse(String id, String accountId, BigDecimal amount, String createdAt){
}
