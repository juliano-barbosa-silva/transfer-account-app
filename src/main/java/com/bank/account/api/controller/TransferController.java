package com.bank.account.api.controller;

import com.bank.account.application.dto.TransferRequest;
import com.bank.account.application.usecase.TransferUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/transfers")
public class TransferController {

    private final TransferUseCase useCase;

    public TransferController(TransferUseCase useCase) {
        this.useCase = useCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void transfer(@RequestBody TransferRequest request) {
        useCase.transfer(request);
    }
}
