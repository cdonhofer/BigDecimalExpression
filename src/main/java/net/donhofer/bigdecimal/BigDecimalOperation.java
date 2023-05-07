package net.donhofer.bigdecimal;

import java.math.RoundingMode;

/**
 * functional interface describing a big decimal operation
 * @param <BigDecimal> operand typ is fixed to BigDecimal
 * @param <T> return type
 */
@FunctionalInterface
public interface BigDecimalOperation<BigDecimal, T> {
    /**
     * apply the operation
     * @param a left operand
     * @param b right operand
     * @param scale scale for division operations
     * @param roundingMode rounding mode for division operations
     * @return the result of type T
     */
    T apply(BigDecimal a, BigDecimal b, int scale, RoundingMode roundingMode);
}
