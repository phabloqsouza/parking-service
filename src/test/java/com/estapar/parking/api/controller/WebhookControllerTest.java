package com.estapar.parking.api.controller;

import com.estapar.parking.api.dto.WebhookEventDto;
import com.estapar.parking.exception.SectorFullException;
import com.estapar.parking.service.ParkingEventService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WebhookController.class)
@DisplayName("WebhookController Unit Tests")
class WebhookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ParkingEventService parkingEventService;

    @Test
    @DisplayName("Should handle ENTRY event successfully")
    void shouldHandleEntryEventSuccessfully() throws Exception {
        // Given
        WebhookEventDto eventDto = new WebhookEventDto();
        eventDto.setEvent("ENTRY");
        eventDto.setLicensePlate("ABC1234");
        eventDto.setEntryTime(Instant.now());
        eventDto.setSector("A");

        doNothing().when(parkingEventService).handleEntryEvent(any(), anyString(), any(Instant.class), anyString());

        // When/Then
        mockMvc.perform(post("/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(eventDto)))
                .andExpect(status().isOk());

        verify(parkingEventService).handleEntryEvent(any(), eq("ABC1234"), any(Instant.class), eq("A"));
    }

    @Test
    @DisplayName("Should handle PARKED event successfully")
    void shouldHandleParkedEventSuccessfully() throws Exception {
        // Given
        WebhookEventDto eventDto = new WebhookEventDto();
        eventDto.setEvent("PARKED");
        eventDto.setLicensePlate("ABC1234");
        eventDto.setLat(new BigDecimal("-23.561684"));
        eventDto.setLng(new BigDecimal("-46.655981"));

        doNothing().when(parkingEventService).handleParkedEvent(any(), anyString(), any(BigDecimal.class), any(BigDecimal.class));

        // When/Then
        mockMvc.perform(post("/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(eventDto)))
                .andExpect(status().isOk());

        verify(parkingEventService).handleParkedEvent(any(), eq("ABC1234"), any(BigDecimal.class), any(BigDecimal.class));
    }

    @Test
    @DisplayName("Should handle EXIT event successfully")
    void shouldHandleExitEventSuccessfully() throws Exception {
        // Given
        WebhookEventDto eventDto = new WebhookEventDto();
        eventDto.setEvent("EXIT");
        eventDto.setLicensePlate("ABC1234");
        eventDto.setExitTime(Instant.now());

        doNothing().when(parkingEventService).handleExitEvent(any(), anyString(), any(Instant.class));

        // When/Then
        mockMvc.perform(post("/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(eventDto)))
                .andExpect(status().isOk());

        verify(parkingEventService).handleExitEvent(any(), eq("ABC1234"), any(Instant.class));
    }

    @Test
    @DisplayName("Should return 400 when ENTRY event missing entry_time")
    void shouldReturn400WhenEntryEventMissingEntryTime() throws Exception {
        // Given
        WebhookEventDto eventDto = new WebhookEventDto();
        eventDto.setEvent("ENTRY");
        eventDto.setLicensePlate("ABC1234");
        // entryTime is null

        // When/Then
        mockMvc.perform(post("/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(eventDto)))
                .andExpect(status().isBadRequest());

        verify(parkingEventService, never()).handleEntryEvent(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Should return 400 when PARKED event missing coordinates")
    void shouldReturn400WhenParkedEventMissingCoordinates() throws Exception {
        // Given
        WebhookEventDto eventDto = new WebhookEventDto();
        eventDto.setEvent("PARKED");
        eventDto.setLicensePlate("ABC1234");
        // lat and lng are null

        // When/Then
        mockMvc.perform(post("/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(eventDto)))
                .andExpect(status().isBadRequest());

        verify(parkingEventService, never()).handleParkedEvent(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Should return 400 when EXIT event missing exit_time")
    void shouldReturn400WhenExitEventMissingExitTime() throws Exception {
        // Given
        WebhookEventDto eventDto = new WebhookEventDto();
        eventDto.setEvent("EXIT");
        eventDto.setLicensePlate("ABC1234");
        // exitTime is null

        // When/Then
        mockMvc.perform(post("/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(eventDto)))
                .andExpect(status().isBadRequest());

        verify(parkingEventService, never()).handleExitEvent(any(), any(), any());
    }

    @Test
    @DisplayName("Should return 400 when validation fails")
    void shouldReturn400WhenValidationFails() throws Exception {
        // Given - missing required fields
        String invalidJson = """
                {
                  "event_type": "ENTRY"
                  // Missing license_plate and entry_time
                }
                """;

        // When/Then
        mockMvc.perform(post("/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());

        verify(parkingEventService, never()).handleEntryEvent(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Should return 400 for unknown event type")
    void shouldReturn400ForUnknownEventType() throws Exception {
        // Given
        WebhookEventDto eventDto = new WebhookEventDto();
        eventDto.setEvent("UNKNOWN");
        eventDto.setLicensePlate("ABC1234");

        // When/Then
        mockMvc.perform(post("/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(eventDto)))
                .andExpect(status().isBadRequest());

        verify(parkingEventService, never()).handleEntryEvent(any(), any(), any(), any());
        verify(parkingEventService, never()).handleParkedEvent(any(), any(), any(), any());
        verify(parkingEventService, never()).handleExitEvent(any(), any(), any());
    }

    @Test
    @DisplayName("Should handle SectorFullException and return 409")
    void shouldHandleSectorFullExceptionAndReturn409() throws Exception {
        // Given
        WebhookEventDto eventDto = new WebhookEventDto();
        eventDto.setEvent("ENTRY");
        eventDto.setLicensePlate("ABC1234");
        eventDto.setEntryTime(Instant.now());
        eventDto.setSector("A");

        doThrow(new SectorFullException("Sector A is full"))
                .when(parkingEventService).handleEntryEvent(any(), anyString(), any(Instant.class), anyString());

        // When/Then
        mockMvc.perform(post("/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(eventDto)))
                .andExpect(status().isConflict());
    }
}
