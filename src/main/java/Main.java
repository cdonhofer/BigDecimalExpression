import java.math.BigDecimal;
import java.util.Map;

public class Main {
    public static void main(String[] args) {
        BigDecimal a = new BigDecimal("0.014000");
        BigDecimal b = new BigDecimal("2");
        BigDecimal c = new BigDecimal("13.73");


        BigDecimal res = a.pow(2).multiply(c.divide(BigDecimal.TEN).add(b.multiply(c).add(a)));
        try {
            BigDecimal easyRes = BigDecimalExp.create().eval("a ^ 2 *((c/10)+b*c+a)", Map.of("a", a, "b", b, "c", c));
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("target result: ");
        System.out.println(new BigDecimal("0.000196000000").multiply(new BigDecimal("28.833")));


//        BigDecimal easyRes2 = BigDecimalExp.create().eval("(a^2 *((c/10)+b*c+a))", Map.of("a", a, "b", b, "c", c));

//        System.out.println("^: "+(int)'^');
//        System.out.println("+: "+(int)'+');
//        System.out.println("-: "+(int)'-');
//        System.out.println("*: "+(int)'*');
//        System.out.println("/: "+(int)'/');


        /*
        stack:
        -----CALC END-----
        -----CALC END-----
        a
        +
        c
        *
        b
        +
        -----CALC END-----
        10
        /
        c
        -----CALC START-----
        -----CALC START-----
        *
        2
        ^
        a
        -----CALC START-----
        -----CALC START-----

        ??
        - whenever calc end is reached, calc block (from last calc start till end/current pos.)
        - how are blocks with more than two operands and 2+ operators solved? See top block: simply using pop order would
        apply addition before multiplication --> wrong! Find way to determine order / sequence!

        --

        * evaluate each separate term first into a big decimal, then create a linked list
        * for each operator, from highest precedence to lowest, go through the list
        * when an occurrence is found, merge the nodes (i.e. apply the operator)
        * it's done when there is only one node left, which is the result

        --
        leave pow for the end - it requires transforming a sub-expression to an int


         */


    }
}
