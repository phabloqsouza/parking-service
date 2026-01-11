package com.estapar.parking.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.math.RoundingMode;

@Configuration
@ConfigurationProperties(prefix = "parking.decimal")
@Getter
@Setter
public class DecimalConfig {
    
    private int currencyScale = 2;
    private int percentageScale = 2;
    private int coordinateScale = 8;
    private RoundingMode roundingMode = RoundingMode.HALF_UP;
}
