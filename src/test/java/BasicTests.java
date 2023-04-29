import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

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
    public void testParser(String expression, Map<String, BigDecimal> params, boolean shouldSucceed) {
        if(!shouldSucceed) {
            assertThrows(BigDecimalExpException.class, () -> {
                BigDecimal parsedResult = new BigDecimalExp().debug().parse(expression, params).eval();
            }, "The expression should have thrown an exception: "+expression);
        } else {
            BigDecimal parsedResult = new BigDecimalExp().debug().parse(expression, params).eval();
        }
    }

    private static Stream<Arguments> getParserExpressions() {
        return Stream.of(
                Arguments.of("2*(100/10)", Map.of(), true), // minimal term with sub expression
                Arguments.of("a ^ 2 *((c/10)+b*c+a)", Map.of("a", new BigDecimal("0.014000"), "b", new BigDecimal("2"), "c", new BigDecimal("13.73")), true),
                Arguments.of("0.014000 ^ 2 *((13.73/10)+2*13.73+0.014000)", Map.of(), true),
                Arguments.of("0.014000 ^ 2*() *((13.73/10)+2*13.73+0.014000)", Map.of(), false), // ()
                Arguments.of("a ^ 2 *((c/10)+b*c+a)", Map.of("a", new BigDecimal("0.014000"), "b", new BigDecimal("2")), false), // missing parameter
                Arguments.of("0,014000 ^ 2 *((13.73/10)+2*13.73+0.014000)", Map.of(), false), // wrong decimal separator
                Arguments.of("0.014000 | 2 *((13.73/10)+2*13.73+0.014000)", Map.of(), false), // unknown operator
                Arguments.of("0.014000 ^^ 2 *((13.73/10)+2*13.73+0.014000)", Map.of(), false), // double operator
                Arguments.of("0.014000 ^ 2 *((13.73/10)+2*13.73+0.014000", Map.of(), false), // missing parenthesis
                Arguments.of("0.014000 ^ 2 ((13.73/10)+2*13.73+0.014000)", Map.of(), true), // implicit multiplication
                Arguments.of("(100/10) * (3+2)", Map.of(), true),
                Arguments.of("(100/10) * (3+2)\n+2", Map.of(), false), // new line
                Arguments.of("(100/10) * (3+2)-", Map.of(), true) // operator as last symbol - makes no sense, but will work
        );
    }

    /**
     * tests the parsing process only, i.e. if it succeeds or fails as expected
     * @param expression the expression to be parsed by BigDecimalExp
     * @param params the Map of param to BigDecimal, for all parameters used in the expression
     */
    @ParameterizedTest
    @MethodSource("getReducerExpressions")
    public void testCalculation(String expression, Map<String, BigDecimal> params, BigDecimal expectedResult, boolean shouldSucceed) {
        BigDecimal parsedResult = new BigDecimalExp(scale, roundingMode).debug().parse(expression, params).eval();
        if(shouldSucceed) {
            assertEquals(0, parsedResult.compareTo(expectedResult), String.format("Expected: %s / Actual: %s", expectedResult, parsedResult));
        } else {
            assertNotEquals(0, parsedResult.compareTo(expectedResult));
        }
    }

    private static Stream<Arguments> getReducerExpressions() {
        // test one
        BigDecimal a1 = new BigDecimal("0.014000");
        BigDecimal b1 = new BigDecimal("2");
        BigDecimal c1 = new BigDecimal("13.73");

        // tests two and three
        BigDecimal a2 = new BigDecimal("17000000000");
        BigDecimal b2 = new BigDecimal("1000000");
        BigDecimal c2 = new BigDecimal("18");
        BigDecimal d2 = new BigDecimal("5");
        BigDecimal e2 = new BigDecimal("13");

        // test four
        BigDecimal a4 = new BigDecimal("100000");
        BigDecimal b4 = new BigDecimal("5");
        BigDecimal c4 = new BigDecimal("18");
        BigDecimal d4 = new BigDecimal("12");
        BigDecimal e4 = new BigDecimal("13");
        return Stream.of(
                Arguments.of(
                        "10*2.5",
                        Map.of(),
                        BigDecimal.TEN.multiply(new BigDecimal("2.5")),
                        true
                ),
                Arguments.of(
                        "a ^ 2 *((c/10)+b*c+a)",
                        Map.of("a", a1, "b", b1, "c", c1),
                        a1.pow(2).multiply(
                                c1.divide(BigDecimal.TEN, scale, roundingMode).add(b1.multiply(c1)).add(a1)
                        ),
                        true
                ),
                Arguments.of(
                        "(17000000000/1000000+1)*10+(18-10/5-13)",
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
                ),
                // implicit multiplication with parentheses
                Arguments.of(
                        "(100/10)(100)",
                        Map.of(),
                        new BigDecimal("1000"),
                        true
                ),
                Arguments.of(
                        "10 (100)",
                        Map.of(),
                        new BigDecimal("1000"),
                        true
                ),
                Arguments.of(
                        "(5-2)(5+2)",
                        Map.of(),
                        new BigDecimal("21"),
                        true
                ),
                Arguments.of(
                        "(5-2)2",
                        Map.of(),
                        new BigDecimal("6"),
                        true
                ),
                Arguments.of(
                        "(5-2)-2",
                        Map.of(),
                        new BigDecimal("-6"),
                        true
                ),
                // implicit multiplication with just one set of parentheses
                Arguments.of(
                        "3 (5+2)",
                        Map.of(),
                        new BigDecimal("21"),
                        true
                ),
                // avoid false implicit multiplication
                Arguments.of(
                        "3+(5+2)",
                        Map.of(),
                        new BigDecimal("10"),
                        true
                ),
                // negative values
                Arguments.of(
                        "-3+(5+2)",
                        Map.of(),
                        new BigDecimal("4"),
                        true
                ),
                Arguments.of(
                        "3+(-5)+2",
                        Map.of(),
                        new BigDecimal("0"),
                        true
                ),
                Arguments.of(
                        "3(-5)",
                        Map.of(),
                        new BigDecimal("-15"),
                        true
                )
        );
    }

    @Test
    public void testOperatorOrder() {
        assertEquals(BigDecimalExp.operators[0], '^');
        assertEquals(BigDecimalExp.operators[1], '*');
        assertEquals(BigDecimalExp.operators[2], '/');
        assertEquals(BigDecimalExp.operators[3], '+');
        assertEquals(BigDecimalExp.operators[4], '-');
    }

    // TODO make this test more exhaustive
    @Test
    public void testVariableExtraction() {
        List<String> params = BigDecimalExp.extractVariables("a ^ 2 *myVar+SOMETHING_BIG((c/10)+b*c+a)");
        List<String> expected = List.of("a", "myVar", "SOMETHING_BIG", "c", "b");

        assertTrue(params.containsAll(expected));
    }

    /**
     * tests the illegal char detection, but minimalistically (i.e. for errors expected to be common)
     */
    @Test
    public void testIllegalCharDetection() {
        assertFalse(BigDecimalExp.containsIllegalChar("0.014000 ^ 2 *((13.73/10)+2*13.73+0.014000) - 1"));
        assertTrue(BigDecimalExp.containsIllegalChar("0.014000 ^ 2: *((13.73/10)+2*13.73+0.014000)")); // colon
        assertTrue(BigDecimalExp.containsIllegalChar("0,014000 ^ 2 *((13.73/10)+2*13.73+0.014000)")); // comma
        assertTrue(BigDecimalExp.containsIllegalChar("0.014000 ^ 2 *([13.73/10]+2*13.73+0.014000)")); // square brackets
        assertTrue(BigDecimalExp.containsIllegalChar("0.014000 ^ 2 *((13.73/10)+2*13.7>3+0.014000)<")); // less/larger than signs
        assertTrue(BigDecimalExp.containsIllegalChar("0.014000 ^ 2%1 *((13.73/10)+2*13.73+0.014000)")); // percent sign/modulo operator
        assertTrue(BigDecimalExp.containsIllegalChar("{0.014000} ^ 2 *((13.73/10)+2*13.73+0.014000)")); // curly braces
        assertTrue(BigDecimalExp.containsIllegalChar("{0.014000} ^ 2 *\\((13.73/10)+2*13.73+0.014000)")); // backslash
    }

    @ParameterizedTest
    @MethodSource("getSpeedTestArgs")
    public void testSpeedDifference(String expression, Map<String, BigDecimal> params, Supplier<BigDecimal> nativeBD, BigDecimal expectedResult) {

        int runs = 1_500_000;

        long expStart = System.nanoTime();
        BigDecimalExp bde = new BigDecimalExp(scale, roundingMode).parse(expression, params);
        BigDecimal resultExp = null;
        for(int i = 0; i < runs; i++) {
            resultExp = bde.eval();
        }
        assertTrue(expectedResult.compareTo(resultExp) == 0, String.format("BDExpression results differ! exp: %s; actual: %s", expectedResult, resultExp));
        long expEnd = System.nanoTime();
        long expDiff = expEnd - expStart;

        long bdStart = System.nanoTime();
        BigDecimal resultBd = null;
        for(int i = 0; i < runs; i++) {
            resultBd = nativeBD.get();
        }
        assertTrue(expectedResult.compareTo(resultBd) == 0, String.format("Native BD results differ! exp: %s; actual: %s", expectedResult, resultBd));
        long bdEnd = System.nanoTime();
        long bdDiff = bdEnd - bdStart;

        System.out.println("Native duration seconds: "+String.format(java.util.Locale.US,"%.10f", (bdDiff/1_000_000_000.0)));
        System.out.println("BigDecimalExp duration seconds: "+String.format(java.util.Locale.US,"%.10f", (expDiff/1_000_000_000.0)));

        // must not take more than 2.2 times the native time
        double bound = bdDiff*2.5;
        assertTrue(expDiff < bound, String.format("Failed to calculate within the given runtime limit of %.10f!", bound));
    }

    private static Stream<Arguments> getSpeedTestArgs() {
        BigDecimal a1 = new BigDecimal("0.014000");
        BigDecimal b1 = new BigDecimal("2");
        BigDecimal c1 = new BigDecimal("13.73");

        BigDecimal a2 = new BigDecimal("17000000000");
        BigDecimal b2 = new BigDecimal("1000000");
        BigDecimal c2 = new BigDecimal("18");
        BigDecimal d2 = new BigDecimal("5");
        BigDecimal e2 = new BigDecimal("13");
        BigDecimal f2 = new BigDecimal("1");
        BigDecimal g2 = new BigDecimal("10");
        BigDecimal h2 = new BigDecimal("2");

        return Stream.of(
                Arguments.of(
                        "a^b*((c/d)+b*c+a)",
                        Map.of("a", a1, "b", b1, "c", c1, "d", BigDecimal.TEN),
                        (Supplier<BigDecimal>)() -> {
                            return new BigDecimal("0.014000").pow(new BigDecimal("2").intValue()).multiply(
                                    new BigDecimal("13.73").divide(new BigDecimal("10"), scale, roundingMode).add(new BigDecimal("2").multiply(new BigDecimal("13.73"))).add(new BigDecimal("0.014000"))
                            );
                        },
                        new BigDecimal("0.005654012")
                ),
                Arguments.of(
                        "(a/b+f)*g+(c-g/d-e)/h",
                        Map.of("a", a2, "b", b2, "c", c2, "d", d2, "e", e2, "f", f2, "g", g2, "h", h2),
                        (Supplier<BigDecimal>) () -> {
                            return new BigDecimal("17000000000")
                                    .divide(new BigDecimal("1000000"), scale, roundingMode)
                                    .add(new BigDecimal("1"))
                                    .multiply(new BigDecimal("10"))
                                    .add(
                                            new BigDecimal("18")
                                                    .subtract(
                                                            new BigDecimal("10")
                                                                    .divide(new BigDecimal("5"), scale, roundingMode)
                                                    )
                                                    .subtract(new BigDecimal("13"))
                                                    .divide(new BigDecimal("2"), scale, roundingMode)
                                    );
                        },
                        new BigDecimal("170011.5")
                ),
                Arguments.of(
                        "(17000000000/1000000+1)*10+(18-10/5-13)/2",
                        Map.of(),
                        (Supplier<BigDecimal>) () -> {
                            return new BigDecimal("17000000000")
                                    .divide(new BigDecimal("1000000"), scale, roundingMode)
                                    .add(new BigDecimal("1"))
                                    .multiply(new BigDecimal("10"))
                                    .add(
                                            new BigDecimal("18")
                                                    .subtract(
                                                            new BigDecimal("10")
                                                                    .divide(new BigDecimal("5"), scale, roundingMode)
                                                    )
                                                    .subtract(new BigDecimal("13"))
                                                    .divide(new BigDecimal("2"), scale, roundingMode)
                                    );
                        },
                        new BigDecimal("170011.5")
                )
        );
    }
}
