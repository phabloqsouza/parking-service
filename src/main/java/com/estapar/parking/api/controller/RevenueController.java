package com.estapar.parking.api.controller;

import com.estapar.parking.api.dto.RevenueRequestDto;
import com.estapar.parking.api.dto.RevenueResponseDto;
import com.estapar.parking.api.mapper.ParkingMapper;
import com.estapar.parking.service.RevenueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/revenue")
@Tag(name = "Revenue", description = "API for querying parking revenue by date and sector")
public class RevenueController {
    
    private static final Logger logger = LoggerFactory.getLogger(RevenueController.class);
    
    private final RevenueService revenueService;
    private final ParkingMapper parkingMapper;
    
    public RevenueController(RevenueService revenueService, ParkingMapper parkingMapper) {
        this.revenueService = revenueService;
        this.parkingMapper = parkingMapper;
    }
    
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
        
        String correlationId = MDC.get("correlationId");
        logger.info("Revenue query: correlationId={}, date={}, sector={}, garageId={}", 
                   correlationId, requestDto.getDate(), requestDto.getSector(), garageId);
        
        // Resolve garageId - if not provided, will use default garage
        UUID resolvedGarageId = garageId; // Will be resolved to default in service if null
        
        try {
            BigDecimal amount = revenueService.getRevenue(
                    resolvedGarageId,
                    requestDto.getDate(),
                    requestDto.getSector());
            
            RevenueResponseDto response = parkingMapper.toRevenueResponseDto(amount);
            logger.info("Revenue query completed: correlationId={}, amount={}, sector={}", 
                       correlationId, amount, requestDto.getSector());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error querying revenue: correlationId={}, date={}, sector={}, error={}", 
                        correlationId, requestDto.getDate(), requestDto.getSector(), e.getMessage(), e);
            throw e; // Let GlobalExceptionHandler handle it
        }
    }
}
