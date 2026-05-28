package com.bank.account.api.controller;

import com.bank.account.application.dto.TransferRequest;
import com.bank.account.application.dto.TransferResponse;
import com.bank.account.application.usecase.TransferUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/transfers")
public class TransferController {

    private final TransferUseCase useCase;

    public TransferController(TransferUseCase useCase) {
        this.useCase = useCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void transfer(@RequestBody @Validated TransferRequest request) {
        useCase.transfer(request);
    }

    @GetMapping("/{accountId}")
    public ResponseEntity<List<TransferResponse>> findByAccountId(
            @PathVariable UUID accountId) {
        List<TransferResponse> response = useCase.findByAccountId(accountId);

        if (response == null || response.isEmpty()) return ResponseEntity.notFound().build();

        return ResponseEntity.ok(response);
    }
}
