package com.estapar.parking.api.controller;

import com.estapar.parking.api.dto.RevenueRequestDto;
import com.estapar.parking.service.RevenueService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RevenueController.class)
@DisplayName("RevenueController Unit Tests")
class RevenueControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RevenueService revenueService;

    @Test
    @DisplayName("Should return revenue successfully")
    void shouldReturnRevenueSuccessfully() throws Exception {
        // Given
        RevenueRequestDto requestDto = new RevenueRequestDto();
        requestDto.setDate(LocalDate.of(2025, 1, 15));
        requestDto.setSector("A");

        BigDecimal expectedRevenue = new BigDecimal("150.50");
        when(revenueService.getRevenue(any(), any(LocalDate.class), anyString())).thenReturn(expectedRevenue);

        // When/Then
        mockMvc.perform(post("/revenue")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(150.50))
                .andExpect(jsonPath("$.currency").value("BRL"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(revenueService).getRevenue(any(), eq(LocalDate.of(2025, 1, 15)), eq("A"));
    }

    @Test
    @DisplayName("Should return 400 when validation fails")
    void shouldReturn400WhenValidationFails() throws Exception {
        // Given - missing required fields
        String invalidJson = """
                {
                  "date": null,
                  "sector": ""
                }
                """;

        // When/Then
        mockMvc.perform(post("/revenue")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());

        verify(revenueService, never()).getRevenue(any(), any(), any());
    }

    @Test
    @DisplayName("Should return 400 when date is in the future")
    void shouldReturn400WhenDateIsInTheFuture() throws Exception {
        // Given
        RevenueRequestDto requestDto = new RevenueRequestDto();
        requestDto.setDate(LocalDate.now().plusDays(1)); // Future date
        requestDto.setSector("A");

        // When/Then
        mockMvc.perform(post("/revenue")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isBadRequest());

        verify(revenueService, never()).getRevenue(any(), any(), any());
    }

    @Test
    @DisplayName("Should return 400 when sector format is invalid")
    void shouldReturn400WhenSectorFormatIsInvalid() throws Exception {
        // Given
        RevenueRequestDto requestDto = new RevenueRequestDto();
        requestDto.setDate(LocalDate.of(2025, 1, 15));
        requestDto.setSector("INVALID"); // Invalid format (not single uppercase letter)

        // When/Then
        mockMvc.perform(post("/revenue")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isBadRequest());

        verify(revenueService, never()).getRevenue(any(), any(), any());
    }

    @Test
    @DisplayName("Should return zero revenue when no sessions found")
    void shouldReturnZeroRevenueWhenNoSessionsFound() throws Exception {
        // Given
        RevenueRequestDto requestDto = new RevenueRequestDto();
        requestDto.setDate(LocalDate.of(2025, 1, 15));
        requestDto.setSector("A");

        when(revenueService.getRevenue(any(), any(LocalDate.class), anyString())).thenReturn(BigDecimal.ZERO);

        // When/Then
        mockMvc.perform(post("/revenue")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(0))
                .andExpect(jsonPath("$.currency").value("BRL"));
    }
}
