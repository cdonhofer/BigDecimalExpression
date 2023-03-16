import java.math.RoundingMode;

@FunctionalInterface
public interface BigDecimalOperation<BigDecimal, T> {
    T apply(BigDecimal a, BigDecimal b, int scale, RoundingMode roundingMode);
}
