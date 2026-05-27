package com.bank.account.infrastructure.kafka;

import com.bank.account.application.dto.TransferRequest;
import org.springframework.stereotype.Component;

@Component
public class TransferEventProducer {

    public void send(TransferRequest request) {
    }
}
