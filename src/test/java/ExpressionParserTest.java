import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.util.Map;
import java.util.stream.Stream;

public class ExpressionParserTest {

    /**
     * tests the parsing process only, i.e. if it succeeds or fails as expected
     * @param expression
     * @param params
     */
    @ParameterizedTest
    @MethodSource("getStandardExpressions")
    public void testStandardCases(String expression, Map<String, BigDecimal> params, boolean shouldSucceed){
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

    private static Stream<Arguments> getStandardExpressions() {
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
}
