package com.bank.account.application.dto;

import java.math.BigDecimal;

public record AccountRequest(String ownerName, BigDecimal balance) {
}
