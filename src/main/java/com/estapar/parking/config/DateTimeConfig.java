package com.estapar.parking.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "parking.datetime")
public class DateTimeConfig {
    
    private String instantFormat = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
    private String dateFormat = "yyyy-MM-dd";
    private String timezone = "UTC";
    
    public String getInstantFormat() {
        return instantFormat;
    }
    
    public void setInstantFormat(String instantFormat) {
        this.instantFormat = instantFormat;
    }
    
    public String getDateFormat() {
        return dateFormat;
    }
    
    public void setDateFormat(String dateFormat) {
        this.dateFormat = dateFormat;
    }
    
    public String getTimezone() {
        return timezone;
    }
    
    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }
}
