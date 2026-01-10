package com.estapar.parking.api.controller;

import com.estapar.parking.api.dto.RevenueRequestDto;
import com.estapar.parking.api.dto.RevenueResponseDto;
import com.estapar.parking.service.RevenueService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/revenue")
public class RevenueController {
    
    private final RevenueService revenueService;
    
    public RevenueController(RevenueService revenueService) {
        this.revenueService = revenueService;
    }
    
    @PostMapping
    public ResponseEntity<RevenueResponseDto> getRevenue(
            @RequestHeader(value = "X-Garage-Id", required = false) UUID garageId,
            @Valid @RequestBody RevenueRequestDto requestDto) {
        
        // Resolve garageId - if not provided, will use default garage
        UUID resolvedGarageId = garageId; // Will be resolved to default in service if null
        
        BigDecimal amount = revenueService.getRevenue(
                resolvedGarageId,
                requestDto.getDate(),
                requestDto.getSector());
        
        RevenueResponseDto response = new RevenueResponseDto(amount, "BRL", Instant.now());
        
        return ResponseEntity.ok(response);
    }
}
