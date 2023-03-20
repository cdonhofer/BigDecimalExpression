public class BigDecimalExpException extends RuntimeException{
    public BigDecimalExpException(String expression, Throwable cause) {
        super(String.format("An exception occurred parsing or evaluating the following expression: %s", expression), cause);
    }
}
