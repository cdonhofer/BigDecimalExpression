public class BigDecimalExpException extends Exception{
    public BigDecimalExpException(String expression, Throwable cause) {
        super(String.format("An exception occurred parsing or evaluating the following expression: %s", expression), cause);
    }
}
