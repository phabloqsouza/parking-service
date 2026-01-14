package com.estapar.parking.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

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
public abstract class WebhookEventDto {

    @Setter
    @NotNull(message = "Event type is required")
    @JsonProperty("event_type")
    private EventType eventType;

    @NotEmpty(message = "License plate is required")
    @JsonProperty("license_plate")
    private String licensePlate;

    public void setLicensePlate(String licensePlate) {
        this.licensePlate = licensePlate != null ? licensePlate.toUpperCase() : null;
    }

}
