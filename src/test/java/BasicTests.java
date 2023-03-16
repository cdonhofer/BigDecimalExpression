import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class BasicTests {
    // MathContext definitions for these tests
    static final int scale = 30;
    static final RoundingMode roundingMode = RoundingMode.HALF_UP;

    /**
     * tests the parsing process only, i.e. if it succeeds or fails as expected
     * @param expression the expression to be parsed by BigDecimalExp
     * @param params the Map of param to BigDecimal, for all parameters used in the expression
     */
    @ParameterizedTest
    @MethodSource("getParserExpressions")
    public void testParser(String expression, Map<String, BigDecimal> params, boolean shouldSucceed){
        try {
            BigDecimal parsedResult = BigDecimalExp.create().eval(expression, params);
        } catch (Exception e) {
            if(shouldSucceed) {
               throw e;
            } else {
                System.out.println("exception: "+e.getCause());
                e.printStackTrace();
            }
        }
    }

    private static Stream<Arguments> getParserExpressions() {
        return Stream.of(
                Arguments.of("a ^ 2 *((c/10)+b*c+a)", Map.of("a", new BigDecimal("0.014000"), "b", new BigDecimal("2"), "c", new BigDecimal("13.73")), true),
                Arguments.of("0.014000 ^ 2 *((13.73/10)+2*13.73+0.014000)", Map.of(), true),
                Arguments.of("0.014000 ^ 2*() *((13.73/10)+2*13.73+0.014000)", Map.of(), true), // ()
                Arguments.of("a ^ 2 *((c/10)+b*c+a)", Map.of("a", new BigDecimal("0.014000"), "b", new BigDecimal("2")), false), // missing parameter
                Arguments.of("0,014000 ^ 2 *((13.73/10)+2*13.73+0.014000)", Map.of(), false), // wrong decimal separator
                Arguments.of("0.014000 | 2 *((13.73/10)+2*13.73+0.014000)", Map.of(), false), // unknown operator
                Arguments.of("0.014000 ^^ 2 *((13.73/10)+2*13.73+0.014000)", Map.of(), false), // double operator
                Arguments.of("0.014000 ^ 2 *((13.73/10)+2*13.73+0.014000", Map.of(), false), // missing parenthesis
                Arguments.of("0.014000 ^ 2 ((13.73/10)+2*13.73+0.014000)", Map.of(), false), // implicit multiplication,
                Arguments.of("(100/10) * (3+2)", Map.of(), true) // // missing parenthesis
        );
    }

    /**
     * tests the parsing process only, i.e. if it succeeds or fails as expected
     * @param expression the expression to be parsed by BigDecimalExp
     * @param params the Map of param to BigDecimal, for all parameters used in the expression
     */
    @ParameterizedTest
    @MethodSource("getReducerExpressions")
    public void testCalculation(String expression, Map<String, BigDecimal> params, BigDecimal expectedResult, boolean shouldSucceed){
        BigDecimal parsedResult = BigDecimalExp.with(scale, roundingMode).eval(expression, params);
        if(shouldSucceed) {
            assertEquals(parsedResult.compareTo(expectedResult), 0);
        } else {
            assertNotEquals(parsedResult.compareTo(expectedResult), 0);
        }
    }

    private static Stream<Arguments> getReducerExpressions() {
        // test one
        BigDecimal a1 = new BigDecimal("0.014000");
        BigDecimal b1 = new BigDecimal("2");
        BigDecimal c1 = new BigDecimal("13.73");

        // tests two and three
        BigDecimal a2 = new BigDecimal("17000000000");
        BigDecimal b2 = new BigDecimal("13.5");
        BigDecimal c2 = new BigDecimal("18");
        BigDecimal d2 = new BigDecimal("12");
        BigDecimal e2 = new BigDecimal("13");

        // test four
        BigDecimal a4 = new BigDecimal("100000");
        BigDecimal b4 = new BigDecimal("5");
        BigDecimal c4 = new BigDecimal("18");
        BigDecimal d4 = new BigDecimal("12");
        BigDecimal e4 = new BigDecimal("13");
        return Stream.of(
                Arguments.of(
                        "a ^ 2 *((c/10)+b*c+a)",
                        Map.of("a", a1, "b", b1, "c", c1),
                        a1.pow(2).multiply(
                                c1.divide(BigDecimal.TEN, scale, roundingMode).add(b1.multiply(c1)).add(a1)
                        ),
                        true
                ),
                Arguments.of(
                        "(17000000000/13.5+1)*10+(18-10/12-13)",
                        Map.of(),
                        (a2.divide(b2, scale, roundingMode).add(BigDecimal.ONE)).multiply(BigDecimal.TEN).add(
                                c2.subtract(BigDecimal.TEN.divide(d2, scale, roundingMode)).subtract(e2)
                        ),
                        true
                ),
                // negative test by scale change
                Arguments.of(
                        "(17000000000/13.5+1)*10+(18-10/12-13)",
                        Map.of(),
                        (a2.divide(b2, 1, roundingMode).add(BigDecimal.ONE)).multiply(BigDecimal.TEN).add(
                                c2.subtract(BigDecimal.TEN.divide(d2, scale, roundingMode)).subtract(e2)
                        ),
                        false
                ),
                // negative test by value
                Arguments.of(
                        "(100000/5+1)*10.00001+(18-10/12-13)",
                        Map.of(),
                        (a4.divide(b4, scale, roundingMode).add(BigDecimal.ONE)).multiply(BigDecimal.TEN).add(
                                c4.subtract(BigDecimal.TEN.divide(d4, scale, roundingMode)).subtract(e4)
                        ),
                        false
                )
        );
    }

    @Test
    public void testOperatorOrder() {
        assertEquals(BigDecimalExp.operators.get(0), '^');
        assertEquals(BigDecimalExp.operators.get(1), '*');
        assertEquals(BigDecimalExp.operators.get(2), '/');
        assertEquals(BigDecimalExp.operators.get(3), '+');
        assertEquals(BigDecimalExp.operators.get(4), '-');
    }
}
