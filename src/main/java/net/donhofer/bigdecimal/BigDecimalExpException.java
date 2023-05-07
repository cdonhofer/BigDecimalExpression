package net.donhofer.bigdecimal;

/**
 * Exception thrown by BigDecimalException to wrap underlying exceptions
 */
public class BigDecimalExpException extends RuntimeException {
    /**
     * default constructor
     * @param expression the mathematical expression causing the exception
     * @param cause the Throwable cause
     */
    public BigDecimalExpException(String expression, Throwable cause) {
        super(String.format("An exception occurred parsing or evaluating the following expression: %s", expression), cause);
    }
}
