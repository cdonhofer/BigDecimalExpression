import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.BinaryOperator;

public class BigDecimalExp {
    public static final int defaultScale = 5;
    public static final RoundingMode defaultRoundingMode = RoundingMode.HALF_UP;

    // operator symbols
    public static final char ADD = '+';
    public static final char SUBTRACT = '-';
    public static final char MULTIPLY = '*';
    public static final char DIVIDE = '/';
    public static final char POW = '^';

    // list of operators for easier handling - the order is vital!
    static final List<Character> operators = List.of(POW, MULTIPLY, DIVIDE, ADD, SUBTRACT);

    // map operators to methods
    static final Map<Character, BigDecimalOperation<BigDecimal, BigDecimal>> opToMethod = Map.of(
            POW, (a, b, scale, rMode) -> a.pow(b.intValue()),
            MULTIPLY, (a, b, scale, rMode) -> a.multiply(b),
            ADD, (a, b, scale, rMode) -> a.add(b),
            SUBTRACT, (a, b, scale, rMode) -> a.subtract(b),
            DIVIDE, (a, b, scale, rMode) -> a.divide(b, scale, rMode)
    );

    RoundingMode roundingMode;
    int scale;

    public BigDecimalExp(int scale, RoundingMode roundingMode) {
        this.roundingMode = roundingMode;
        this.scale = scale;
    }

    public static BigDecimalExp create() {
        // TODO should this perhaps set nothing at all? might cause trouble or bad code building the divisions
        return new BigDecimalExp(defaultScale, defaultRoundingMode);
    }

    public static BigDecimalExp with(int scale, RoundingMode roundingMode) {
        return new BigDecimalExp(scale, roundingMode);
    }

    public BigDecimal eval(String exp, Map.Entry<String, BigDecimal>... params) {
        Map<String, BigDecimal> paramsMap = new HashMap<>();
        for(Map.Entry<String, BigDecimal> param : params) {
            paramsMap.put(param.getKey(), param.getValue());
        }

        return eval(exp, paramsMap);
    }

    public BigDecimal eval(String exp, Map<String, BigDecimal> params) throws ArithmeticException {
        /*
        validate input
         */

        // no. of parentheses
        if(!validateParentheses(exp)) {
            throw new ArithmeticException("Different no. of opening and closing parentheses");
        }

        // TODO disallow multiline strings

        // remove spaces
        exp = exp.strip().replaceAll("\\s", "");

        char[] chars = exp.toCharArray();

        /*
         * parse, each opening parenthesis creates a recursive call of this method
         */
        // collect operations as a linked list with a dummy start node
        Node startNode = new Node(null, ' ');
        Node node = startNode;

        // keep track of operators to allow for an efficient in-order application once parsing is done
        Map<Character, List<Node>> nodesPerOp = new HashMap<>();
        operators.forEach(op -> nodesPerOp.put(op, new ArrayList<>()));

        int start = 0;
        for(int i = 0; i < chars.length; i++) {
            char c = chars[i];
            boolean isOp = isOperator(c);
            boolean isEnd = i == chars.length-1;

            // within a term / expression: continue
            if(!isEnd && c != '(' && !isOp) {
                continue;
            }

            // case where operator is the first sign encountered, which happens after a sub-expression has been parsed
            if(isOp && start == i) {
                if(node == startNode){
                    throw new ArithmeticException(String.format("An expression must not start with an operator: %s", exp));
                }
                node.op = c;

                // continue at next char
                start = i + 1;
            }else if(isOp || isEnd) {
                // get the term that ends here / at the last pos
                int expLastChar = isEnd ? i : i - 1;
                String currentTerm = new String(Arrays.copyOfRange(chars, start, expLastChar + 1)).trim();
                // check if term is a parameter and if so, get it; else, treat term as value and create BigDecimal
                BigDecimal val = Optional
                        .ofNullable(params.get(currentTerm))
                        .orElseGet(() -> new BigDecimal(currentTerm));

                node.next = new Node(val, isEnd ? null : c);
                node.next.prev = node;
                node = node.next;
            } else { // start of sub-expression

                // if symbol before this parenthesis was neither an operator, nor start of expression, nor another parenthesis
                // this is an implicit multiplication, which isn't supported by now
                // when(if ever) implementing this, take into account all cases: terms from sub-expressions will be inside
                // a node already, which is simply missing the operator
                // else (i.e. the node before has an operator), fetch the ongoing term and add a multiplication node
                if(i != 0 && !isOperator(chars[i-1]) && chars[i-1] != '(') {
                    throw new ArithmeticException("Implicit multiplication is currently not supported. Please use the multiplication operator");
                }


                // find matching closing parenthesis
                int pCnt = 1;
                int pEnd = i;
                while(pCnt > 0) {
                    pEnd++;
                    if(chars[pEnd] == '(') pCnt++;
                    if(chars[pEnd] == ')') pCnt--;
                }
                // parse sub-expression
                // but ignore empty parentheses: ()
                if(pEnd - i > 1) {
                    // add sub expression and continue
                    BigDecimal subExp = eval(new String(Arrays.copyOfRange(chars, i+1, pEnd)), params);
                    node.next = new Node(subExp, null);
                    node.next.prev = node;
                    node = node.next;
                }
                i = pEnd;
            }

            if(isOp) {
                //add to occ. map
                nodesPerOp.get(c).add(node);
            }

            // continue at next char
            start = i + 1;

        }
        /*
         * possible problems
         * operator in the last position!?
         */

        System.out.println("----------------------------------");

        System.out.println("found these terms: ");
        node = startNode.next;
        while(node != null) {
            System.out.println(" "+node.val.toString()+" "+Optional.ofNullable(node.op).orElse(' '));
            node = node.next;
        }
//
//        System.out.println("----------------------------------");
//
//        System.out.println("found these operator nodes: ");
//        operators.forEach(op -> {
//            System.out.println("Operator "+op+"("+nodesPerOp.get(op).size()+")");
//            System.out.println(nodesPerOp.get(op).stream().map(n -> ""+n.val+n.op+n.next.val).collect(Collectors.joining(";")));
//        });

        // apply operations
        for(char op : operators) {
            BigDecimalOperation<BigDecimal, BigDecimal> operation = opToMethod.get(op);
            for(Node n : nodesPerOp.get(op)) {
                // keep refs of adjacent nodes
                Node left = n.prev;
                Node secondOperand = n.next;

                // write result to right operand, as it possible contains an operation with another node - left one is unlinked
                secondOperand.val = operation.apply(n.val, secondOperand.val, scale, roundingMode);

                // unlink processed node
                secondOperand.prev = left;
                left.next = secondOperand;
            }
        }
        System.out.println("----------------------------------");

        System.out.println("final terms: ");
        node = startNode.next;
        while(node != null) {
            System.out.println(" "+node.val.toString()+" "+Optional.ofNullable(node.op).orElse(' '));
            node = node.next;
        }

        System.out.println("----------------------------------");
        return startNode.next.val;
    }

    // utility methods
    private boolean isOperator(Character c) {
        return operators.contains(c);
    }

    private boolean validateParentheses(String haystack) {
        int opening = 0;
        int closing = 0;
        for(char c : haystack.toCharArray()) {
            if(c == '(') opening++;
            if(c == ')') closing++;
        }

        return opening == closing;
    }

    private static class Node {
        BigDecimal val;
        Character op;
        Node prev;
        Node next;

        Node(BigDecimal val, Character op) {
            this.val = val;
            this.op = op;
        }
    }
}
