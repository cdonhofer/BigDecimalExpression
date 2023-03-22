import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class BigDecimalExp {
    public static final int defaultScale = 5;
    public static final RoundingMode defaultRoundingMode = RoundingMode.HALF_UP;

    // operator symbols
    public static final char ADD = '+';
    public static final char SUBTRACT = '-';
    public static final char MULTIPLY = '*';
    public static final char DIVIDE = '/';
    public static final char POW = '^';

    public static final String VALID_VAR_REGEX = "[a-zA-Z_$][a-zA-Z_$0-9]*";
    public static final String ILLEGAL_CHARS_REGEX = "[^a-zA-Z0-9.\\-+*/^_\\s()]";

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

    /*
    * instance fields
    */
    RoundingMode roundingMode;
    int scale;
    Map<String, BigDecimal> vars = new HashMap<>();
    String exp;

    public BigDecimalExp(int scale, RoundingMode roundingMode) {
        this.roundingMode = roundingMode;
        this.scale = scale;
    }

    public BigDecimalExp() {
        this.roundingMode = defaultRoundingMode;
        this.scale = defaultScale;
    }

    public BigDecimalExp parse(String exp, Map.Entry<String, BigDecimal>... vars) throws BigDecimalExpException {
        Map<String, BigDecimal> varsMap = new HashMap<>();
        for(Map.Entry<String, BigDecimal> var : vars) {
            varsMap.put(var.getKey(), var.getValue());
        }

        return parse(exp, varsMap);
    }

    public BigDecimalExp parse(String exp, Map<String, BigDecimal> vars) throws BigDecimalExpException {
        this.exp = exp;
        this.vars = vars;

        return this;
    }

    /**
     * state-test method that can be used before calling the eval method
     * tests for: illegal characters in the expression, missing variables or null entries in the vars map
     * @return true if all mentioned checks succeed; else false
     */
    public boolean isValid() {
        // validate symbols
        boolean charsAreLegal = !containsIllegalChar(exp);

        // missing or null vars
        List<String> vars = extractVariables(exp);
        boolean allVarsProvided = this.vars.keySet().containsAll(vars);

        return charsAreLegal && allVarsProvided && validateParentheses(exp);
    }

    public BigDecimal eval() throws BigDecimalExpException {
        try {
            return evaluate(exp, vars);
        } catch (Exception e) {
            throw new BigDecimalExpException(exp, e);
        }
    }

    private BigDecimal evaluate(String exp, Map<String, BigDecimal> vars) throws ArithmeticException, NumberFormatException {
        /*
        validate input
         */
        if(!validateParentheses(exp)) {
            throw new ArithmeticException("Different no. of opening and closing parentheses");
        }

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
            }else if(isOp || isEnd) {
                // get the term that ends here / at the last pos
                BigDecimal val = getCurrentVal(chars, start, i, isEnd);

                node.next = new Node(val, isEnd ? null : c);
                node.next.prev = node;
                node = node.next;
            } else { // start of sub-expression

                // if symbol before this parenthesis was neither an operator, nor start of expression, nor another opening parenthesis
                // this is an implicit multiplication
                // terms from sub-expressions will be inside
                // a node already, which is simply missing the operator
                // else (i.e. the node before has an operator), fetch the ongoing term and add a multiplication node
                if(i != 0 && !isOperator(chars[i-1]) && chars[i-1] != '(') {
                    if(node.op != null) {
                        BigDecimal val = getCurrentVal(chars, start, i, false);
                        node.next = new Node(val, MULTIPLY);
                        node.next.prev = node;
                        node = node.next;
                    } else {
                        node.op = MULTIPLY;
                    }
                    //add to occ. map
                    nodesPerOp.get(MULTIPLY).add(node);
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
                    BigDecimal subExp = evaluate(new String(Arrays.copyOfRange(chars, i+1, pEnd)), vars);
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
//        System.out.println("----------------------------------");
//
//        System.out.println("found these terms: ");
//        node = startNode.next;
//        while(node != null) {
//            System.out.println(" "+node.val.toString()+" "+Optional.ofNullable(node.op).orElse(' '));
//            node = node.next;
//        }

        // apply operations
        for(char op : operators) {
            BigDecimalOperation<BigDecimal, BigDecimal> operation = opToMethod.get(op);
            for(Node n : nodesPerOp.get(op)) {
                // keep refs of adjacent nodes
                Node left = n.prev;
                Node secondOperand = n.next;

                if(secondOperand == null) {
                    throw new ArithmeticException(String.format("Illegal Expression: missing right-hand operand in expression %s", exp));
                }

                // write result to right operand, as it possible contains an operation with another node - left one is unlinked
                secondOperand.val = operation.apply(n.val, secondOperand.val, scale, roundingMode);

                // unlink processed node
                secondOperand.prev = left;
                left.next = secondOperand;
            }
        }
//        System.out.println("----------------------------------");
//
//        System.out.println("final terms: ");
//        node = startNode.next;
//        while(node != null) {
//            System.out.println(" "+node.val.toString()+" "+Optional.ofNullable(node.op).orElse(' '));
//            node = node.next;
//        }
//
//        System.out.println("----------------------------------");
        return startNode.next.val;
    }

    private BigDecimal getCurrentVal(char[] chars, int start, int i, boolean isEnd) {
        int expLastChar = isEnd ? i : i - 1;
        String currentTerm = new String(Arrays.copyOfRange(chars, start, expLastChar + 1)).trim();
        // check if term is a variable and if so, get it; else, treat term as value and create BigDecimal
        return Optional
                .ofNullable(vars.get(currentTerm))
                .orElseGet(() -> new BigDecimal(currentTerm));
    }

    public static List<String> extractVariables(String exp) {
         return Pattern.compile(VALID_VAR_REGEX)
                .matcher(exp)
                .results()
                .map(MatchResult::group)
                .collect(Collectors.toList());
    }

    public static boolean containsIllegalChar(String exp) {
        return Pattern.compile(ILLEGAL_CHARS_REGEX)
                .matcher(exp)
                .find();
    }

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
