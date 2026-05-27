package com.bank.account.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record AccountResponse(UUID id, String ownerName, BigDecimal balance) {
}
