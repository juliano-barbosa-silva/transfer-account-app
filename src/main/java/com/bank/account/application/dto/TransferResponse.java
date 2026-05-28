package com.bank.account.application.dto;

import java.math.BigDecimal;

public record TransferResponse(String id, String accountId, String type, BigDecimal amount, String createdAt){
}
