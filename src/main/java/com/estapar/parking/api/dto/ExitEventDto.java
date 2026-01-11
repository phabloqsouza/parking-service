package com.estapar.parking.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ExitEventDto extends WebhookEventDto {
    
    @NotNull(message = "Exit time is required")
    @JsonProperty("exit_time")
    private Instant exitTime;
}
