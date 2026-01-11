package com.estapar.parking.api.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    property = "event_type",
    visible = true
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = EntryEventDto.class, name = "ENTRY"),
    @JsonSubTypes.Type(value = ParkedEventDto.class, name = "PARKED"),
    @JsonSubTypes.Type(value = ExitEventDto.class, name = "EXIT")
})
public abstract class WebhookEventDto implements ParkingEvent {
    
    @NotNull(message = "Event type is required")
    @Pattern(regexp = "ENTRY|PARKED|EXIT", message = "Event type must be ENTRY, PARKED, or EXIT")
    @JsonProperty("event_type")
    private String eventType;
    
    @NotNull(message = "License plate is required")
    @Pattern(regexp = "[A-Z]{3}[0-9][A-Z][0-9]{2}|[A-Z]{3}-?[0-9]{4}", 
             message = "License plate format is invalid (expected format: ABC1234 or ABC-1234)")
    @JsonProperty("license_plate")
    private String licensePlate;
    
    @Pattern(regexp = "^[A-Z]$", message = "Sector must be a single uppercase letter (A-Z)")
    private String sector;
    
    private UUID garageId;
    
    public EventType getEventTypeEnum() {
        if (eventType == null) {
            return null;
        }
        return EventType.valueOf(eventType.toUpperCase());
    }
    
    @Override
    public EventType getEventType() {
        return getEventTypeEnum();
    }
}
