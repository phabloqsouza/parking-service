package com.estapar.parking.util;

import com.estapar.parking.config.DecimalConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BigDecimalUtilsTest {

    @Mock
    private DecimalConfig decimalConfig;

    @InjectMocks
    private BigDecimalUtils bigDecimalUtils;

    @BeforeEach
    void setUp() {
        when(decimalConfig.getCurrencyScale()).thenReturn(2);
        when(decimalConfig.getPercentageScale()).thenReturn(2);
        when(decimalConfig.getCoordinateScale()).thenReturn(8);
        when(decimalConfig.getRoundingMode()).thenReturn(RoundingMode.HALF_UP);
    }

    @Test
    void setCurrencyScale_ShouldSetScaleTo2() {
        BigDecimal value = new BigDecimal("10.555");
        BigDecimal result = bigDecimalUtils.setCurrencyScale(value);

        assertThat(result).isEqualByComparingTo(new BigDecimal("10.56"));
        assertThat(result.scale()).isEqualTo(2);
    }

    @Test
    void calculatePercentage_ShouldCalculatePercentageCorrectly() {
        BigDecimal dividend = new BigDecimal("25");
        BigDecimal divisor = new BigDecimal("100");

        BigDecimal result = bigDecimalUtils.calculatePercentage(dividend, divisor);

        assertThat(result).isEqualByComparingTo(new BigDecimal("25.00"));
        assertThat(result.scale()).isEqualTo(2);
    }

    @Test
    void calculatePercentage_WithDecimalResult_ShouldRoundCorrectly() {
        BigDecimal dividend = new BigDecimal("33");
        BigDecimal divisor = new BigDecimal("100");

        BigDecimal result = bigDecimalUtils.calculatePercentage(dividend, divisor);

        assertThat(result).isEqualByComparingTo(new BigDecimal("33.00"));
    }

    @Test
    void divideWithCurrencyScale_ShouldDivideAndSetCurrencyScale() {
        BigDecimal dividend = new BigDecimal("100");
        BigDecimal divisor = new BigDecimal("3");

        BigDecimal result = bigDecimalUtils.divideWithCurrencyScale(dividend, divisor);

        assertThat(result).isEqualByComparingTo(new BigDecimal("33.33"));
        assertThat(result.scale()).isEqualTo(2);
    }

    @Test
    void multiplyAndSetCurrencyScale_ShouldMultiplyAndSetCurrencyScale() {
        BigDecimal value1 = new BigDecimal("10.555");
        BigDecimal value2 = new BigDecimal("2");

        BigDecimal result = bigDecimalUtils.multiplyAndSetCurrencyScale(value1, value2);

        assertThat(result).isEqualByComparingTo(new BigDecimal("21.11"));
        assertThat(result.scale()).isEqualTo(2);
    }

    @Test
    void zeroWithCurrencyScale_ShouldReturnZeroWithCurrencyScale() {
        BigDecimal result = bigDecimalUtils.zeroWithCurrencyScale();

        assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.scale()).isEqualTo(2);
    }
}
