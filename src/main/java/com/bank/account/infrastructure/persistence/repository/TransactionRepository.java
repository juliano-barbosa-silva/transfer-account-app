package com.bank.account.infrastructure.persistence.repository;

import com.bank.account.infrastructure.persistence.TransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<TransactionEntity, UUID> {

    @Query("SELECT t FROM TransactionEntity t WHERE t.idempotencyKey = :idempotencyKey")
    Optional<String>findByIdempotencyKey(@Param("idempotencyKey") String idempotencyKey);
}
