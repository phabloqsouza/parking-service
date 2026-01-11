package com.estapar.parking.api.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
public class RevenueResponseDto {
    
    private BigDecimal amount;
    private String currency;
    private Instant timestamp;
    
    public RevenueResponseDto(BigDecimal amount, String currency, Instant timestamp) {
        this.amount = amount;
        this.currency = currency;
        this.timestamp = timestamp;
    }
}
