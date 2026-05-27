package com.bank.account.infrastructure.persistence.repository;

import com.bank.account.infrastructure.persistence.AccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AccountRepository extends JpaRepository<AccountEntity, UUID> {
}
