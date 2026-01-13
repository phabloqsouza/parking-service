package com.estapar.parking.api.controller;

import com.estapar.parking.api.dto.RevenueRequestDto;
import com.estapar.parking.api.dto.RevenueResponseDto;
import com.estapar.parking.service.PricingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/revenue")
@Tag(name = "Revenue", description = "API for querying parking revenue by date and sector")
@RequiredArgsConstructor
public class RevenueController {
    
    private static final Logger logger = LoggerFactory.getLogger(RevenueController.class);
    
    private final PricingService pricingService;

    
    @PostMapping
    @Operation(
        summary = "Get revenue by date and sector",
        description = "Returns total revenue for completed parking sessions in a specific sector on a given date"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Revenue retrieved successfully",
            content = @Content(schema = @Schema(implementation = RevenueResponseDto.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "404", description = "Sector not found", content = @Content)
    })
    public ResponseEntity<RevenueResponseDto> getRevenue(
            @Parameter(description = "Optional garage ID. If not provided, uses default garage")
            @RequestHeader(value = "X-Garage-Id", required = false) UUID garageId,
            @Valid @RequestBody RevenueRequestDto requestDto) {
        
        logger.info("Revenue query: date={}, sector={}, garageId={}", 
                   requestDto.getDate(), requestDto.getSector(), garageId);
        
        RevenueResponseDto response = pricingService.getRevenue(
                garageId,
                requestDto.getDate(),
                requestDto.getSector());
        
        logger.info("Revenue query completed: amount={}, sector={}",
                   response.getAmount(), requestDto.getSector());
        
        return ResponseEntity.ok(response);
    }
}
