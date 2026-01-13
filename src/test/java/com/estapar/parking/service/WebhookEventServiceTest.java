package com.estapar.parking.service;

import com.estapar.parking.api.dto.EntryEventDto;
import com.estapar.parking.api.dto.EventType;
import com.estapar.parking.api.dto.ExitEventDto;
import com.estapar.parking.api.dto.WebhookEventDto;
import com.estapar.parking.infrastructure.persistence.entity.Garage;
import com.estapar.parking.service.event.EventHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebhookEventServiceTest {

    @Mock
    private GarageResolver garageResolver;

    @Mock
    private EventHandler entryEventHandler;

    @Mock
    private EventHandler exitEventHandler;

    private WebhookEventService webhookEventService;

    private Garage garage;
    private UUID garageId;
    private EntryEventDto entryEvent;
    private ExitEventDto exitEvent;
    private WebhookEventDto unsupportedEvent;

    @BeforeEach
    void setUp() {
        garageId = UUID.randomUUID();

        garage = new Garage();
        garage.setId(garageId);

        entryEvent = new EntryEventDto();
        entryEvent.setEventType(EventType.ENTRY);
        entryEvent.setLicensePlate("ABC1234");
        entryEvent.setEntryTime(Instant.now());

        exitEvent = new ExitEventDto();
        exitEvent.setEventType(EventType.EXIT);
        exitEvent.setLicensePlate("ABC1234");
        exitEvent.setExitTime(Instant.now());

        unsupportedEvent = new EntryEventDto();
        unsupportedEvent.setEventType(EventType.ENTRY);
        unsupportedEvent.setLicensePlate("XYZ9876");

        webhookEventService = new WebhookEventService(
                garageResolver,
                Arrays.asList(entryEventHandler, exitEventHandler));
    }

    @Test
    void processEvent_WithMatchingHandler_ShouldCallHandler() {
        when(garageResolver.getGarage(garageId)).thenReturn(garage);
        when(entryEventHandler.supports(entryEvent)).thenReturn(true);
        when(exitEventHandler.supports(entryEvent)).thenReturn(false);

        webhookEventService.processEvent(garageId, entryEvent);

        verify(garageResolver).getGarage(garageId);
        verify(entryEventHandler).supports(entryEvent);
        verify(entryEventHandler).handle(garage, entryEvent);
    }

    @Test
    void processEvent_WithNoMatchingHandler_ShouldNotCallAnyHandler() {
        when(garageResolver.getGarage(garageId)).thenReturn(garage);
        when(entryEventHandler.supports(unsupportedEvent)).thenReturn(false);
        when(exitEventHandler.supports(unsupportedEvent)).thenReturn(false);

        webhookEventService.processEvent(garageId, unsupportedEvent);

        verify(garageResolver).getGarage(garageId);
        verify(entryEventHandler).supports(unsupportedEvent);
        verify(exitEventHandler).supports(unsupportedEvent);
        verify(entryEventHandler, never()).handle(any(), any());
        verify(exitEventHandler, never()).handle(any(), any());
    }

    @Test
    void processEvent_WithNullGarageId_ShouldUseDefaultGarage() {
        when(garageResolver.getGarage(null)).thenReturn(garage);
        when(entryEventHandler.supports(entryEvent)).thenReturn(true);

        webhookEventService.processEvent(null, entryEvent);

        verify(garageResolver).getGarage(null);
        verify(entryEventHandler).handle(garage, entryEvent);
    }
}
