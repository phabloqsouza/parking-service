package com.estapar.parking.util;

import com.estapar.parking.config.DecimalConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class BigDecimalUtils {
    
    private final DecimalConfig decimalConfig;
    
    public BigDecimal setCurrencyScale(BigDecimal value) {
        return value.setScale(decimalConfig.getCurrencyScale(), decimalConfig.getRoundingMode());
    }
    
    public BigDecimal calculatePercentage(BigDecimal dividend, BigDecimal divisor) {
        return  divide(dividend, divisor, decimalConfig.getPercentageScale()*2)
                .multiply(BigDecimal.valueOf(100))
                .setScale(decimalConfig.getPercentageScale(), decimalConfig.getRoundingMode());
    }
    
    public BigDecimal divideWithCurrencyScale(BigDecimal dividend, BigDecimal divisor) {
        return dividend.divide(divisor, decimalConfig.getCurrencyScale(), decimalConfig.getRoundingMode());
    }
    
    public BigDecimal multiplyAndSetCurrencyScale(BigDecimal value1, BigDecimal value2) {
        return setCurrencyScale(value1.multiply(value2));
    }
    
    public BigDecimal zeroWithCurrencyScale() {
        return BigDecimal.ZERO.setScale(decimalConfig.getCurrencyScale(), decimalConfig.getRoundingMode());
    }
    
    public BigDecimal divide(BigDecimal dividend, BigDecimal divisor, int scale) {
        return dividend.divide(divisor, scale, decimalConfig.getRoundingMode());
    }
    
    public BigDecimal setScale(BigDecimal value, int scale) {
        return value.setScale(scale, decimalConfig.getRoundingMode());
    }
}
