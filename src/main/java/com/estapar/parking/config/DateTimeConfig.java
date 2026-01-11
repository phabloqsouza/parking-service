package com.estapar.parking.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "parking.datetime")
@Getter
@Setter
public class DateTimeConfig {
    
    private String instantFormat = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
    private String dateFormat = "yyyy-MM-dd";
    private String timezone = "UTC";
}
