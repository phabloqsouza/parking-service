package com.estapar.parking.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.math.RoundingMode;

@Configuration
@ConfigurationProperties(prefix = "parking.decimal")
public class DecimalConfig {
    
    private int currencyScale = 2;
    private int percentageScale = 2;
    private int coordinateScale = 8;
    private RoundingMode roundingMode = RoundingMode.HALF_UP;
    
    public int getCurrencyScale() {
        return currencyScale;
    }
    
    public void setCurrencyScale(int currencyScale) {
        this.currencyScale = currencyScale;
    }
    
    public int getPercentageScale() {
        return percentageScale;
    }
    
    public void setPercentageScale(int percentageScale) {
        this.percentageScale = percentageScale;
    }
    
    public int getCoordinateScale() {
        return coordinateScale;
    }
    
    public void setCoordinateScale(int coordinateScale) {
        this.coordinateScale = coordinateScale;
    }
    
    public RoundingMode getRoundingMode() {
        return roundingMode;
    }
    
    public void setRoundingMode(RoundingMode roundingMode) {
        this.roundingMode = roundingMode;
    }
}
