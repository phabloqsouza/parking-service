package com.estapar.parking.api.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;

@Getter
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
public abstract class WebhookEventDto   {
    
    @NotNull(message = "Event type is required")
    @JsonProperty("event_type")
    private EventType eventType;
    
    @NotNull(message = "License plate is required")
    @Pattern(regexp = "[a-zA-Z0-9]{7}", message = "License plate must be exactly 7 alphanumeric characters")
    @JsonProperty("license_plate")
    private String licensePlate;

    public void setLicensePlate(String licensePlate) {
        this.licensePlate = licensePlate != null ? licensePlate.toUpperCase() : null;
    }

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }
}
