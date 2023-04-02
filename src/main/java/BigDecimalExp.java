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
    static final Character[] operators = new Character[]{POW, MULTIPLY, DIVIDE, ADD, SUBTRACT};

    /*
    * instance fields
    */
    RoundingMode roundingMode;
    int scale;
    Map<String, BigDecimal> vars = new HashMap<>();
    String exp;
    char[] chars;
    int currInd;

    // debug flag makes this very verbose
    boolean debug = false;

    public BigDecimalExp(int scale, RoundingMode roundingMode) {
        this.roundingMode = roundingMode;
        this.scale = scale;
    }

    public BigDecimalExp() {
        this.roundingMode = defaultRoundingMode;
        this.scale = defaultScale;
    }

    public BigDecimalExp debug() {
        debug = true;
        return this;
    }

    public BigDecimalExp parse(String exp, Map.Entry<String, BigDecimal>... vars) throws BigDecimalExpException {
        Map<String, BigDecimal> varsMap = new HashMap<>();
        for(Map.Entry<String, BigDecimal> var : vars) {
            varsMap.put(var.getKey(), var.getValue());
        }

        return parse(exp, varsMap);
    }

    public BigDecimalExp parse(String exp, Map<String, BigDecimal> vars) throws BigDecimalExpException {
        // remove whitespace from expression
        this.exp = exp.strip().replaceAll("\\s", "");
        this.chars = this.exp.toCharArray();
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
        // the index is maintained globally, due to the recursive nature of the evaluation
        currInd = 0;
        try {
            if(!validateParentheses(exp)) {
                throw new ArithmeticException("Different no. of opening and closing parentheses");
            }
            return evaluate();
        } catch (Exception e) {
            throw new BigDecimalExpException(exp, e);
        }
    }

    private BigDecimal evaluate() throws ArithmeticException, NumberFormatException {
        /*
         * parse and evaluate, each opening parenthesis creates a recursive call of this method
         * to immediately reduce the contained sub-expression to a single BigDecimal
         */
        // collect operations as a linked list with a dummy start node
        Node startNode = new Node(null, ' ');
        Node node = startNode;

        OperationsLists nodesPerOp = new OperationsLists();

        // iterate over the characters of this (sub-)expression
        int start = currInd;
        while(currInd < chars.length) {
            char c = chars[currInd];
            boolean isOp = isOperator(c);
            boolean isEnd = currInd == chars.length-1;
            boolean isStartOfSubExpr = c == '(';
            boolean isEndOfSubExpr = c == ')';

            // within a term: continue
            if(!isEnd && !isEndOfSubExpr && !isStartOfSubExpr && !isOp) {
                currInd++;
                continue;
            }

            // case where operator is the first sign encountered, which (legally) happens after a sub-expression has been parsed
            if(isOp && start == currInd) {
                if(node == startNode){
                    throw new ArithmeticException(String.format("An expression must not start with an operator: %s", exp));
                }
                // do not silently accept duplicate operators
                if(node.op != null) {
                    throw new ArithmeticException(String.format("duplicate operators (op. 1: %s, op. 2: %s): %s", node.op, c, exp));
                }

                node.op = c;
            }else if(isOp || isEnd || isEndOfSubExpr) {
                // get the term that ends here / at the last pos
                BigDecimal val = getCurrentVal(chars, start, currInd, isEnd, isEndOfSubExpr);
                node = node.appendAndReturn(new Node(val, isOp ? c : null));
            } else if(isStartOfSubExpr) { // start of sub-expression

                // handle empty parentheses sub-expression: ()
                if(chars[currInd+1] == ')') {
                    throw new ArithmeticException(String.format("Empty sub-expressions are not allowed: %s", "()"));
                }

                // implicit multiplication; terms from sub-expressions will be inside a node already, which is simply missing the operator
                // else (i.e. the node before has an operator), fetch the ongoing term and add a multiplication node
                if(currInd != 0 && !isOperator(chars[currInd-1]) && chars[currInd-1] != '(') {
                    if(node.op != null) {
                        BigDecimal val = getCurrentVal(chars, start, currInd, false, false);
                        node = node.appendAndReturn(new Node(val, MULTIPLY));
                    } else {
                        node.op = MULTIPLY;
                    }
                    nodesPerOp.multiply.add(node);
                }

                // parse sub-expression and add resulting value as a node
                currInd++;
                node = node.appendAndReturn(new Node(evaluate(), null));
            }

            if(isOp) {
                switch (c) {
                    case POW -> nodesPerOp.pow.add(node);
                    case MULTIPLY -> nodesPerOp.multiply.add(node);
                    case DIVIDE -> nodesPerOp.divide.add(node);
                    case ADD -> nodesPerOp.add.add(node);
                    case SUBTRACT -> nodesPerOp.subtract.add(node);
                }
            }

            // end loop if we've reached the end of a sub-expression
            if(isEndOfSubExpr) {
                break;
            }

            // else, continue at next char
            start = currInd + 1;
            currInd++;
        }

        // apply operations
        if(debug) printTerms("found these terms: ", startNode.next);
        BigDecimal result = applyOperations(startNode, nodesPerOp);
//        BigDecimal result = BigDecimal.TEN;
        if(debug) printTerms("final terms: ", startNode.next);

        return result;
    }

    private static int getOpIndex(char operator) {
        return operator;
    }

    private void printTerms(String msg, Node node) {
        System.out.println("----------------------------------");
        System.out.println(msg);
        while(node != null) {
            System.out.println(" "+node.val.toString()+" "+Optional.ofNullable(node.op).orElse(' '));
            node = node.next;
        }
        System.out.println("----------------------------------");
    }

    private BigDecimal applyOperations(Node startNode, OperationsLists nodesPerOp) {
        if(nodesPerOp.pow.size() > 0) {
            applyOp(getOpMethod(POW), nodesPerOp.pow);
        }
        if(nodesPerOp.multiply.size() > 0) {
            applyOp(getOpMethod(MULTIPLY), nodesPerOp.multiply);
        }
        if(nodesPerOp.divide.size() > 0) {
            applyOp(getOpMethod(DIVIDE), nodesPerOp.divide);
        }
        if(nodesPerOp.add.size() > 0) {
            applyOp(getOpMethod(ADD), nodesPerOp.add);
        }
        if(nodesPerOp.subtract.size() > 0) {
            applyOp(getOpMethod(SUBTRACT), nodesPerOp.subtract);
        }

        return startNode.next.val;
    }

    private void applyOp(BigDecimalOperation<BigDecimal, BigDecimal> operation, List<Node> opNodes) {
        for(Node n : opNodes) {
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

    private BigDecimalOperation<BigDecimal, BigDecimal> getOpMethod(char op) {
        return switch (op) {
            case POW -> (a, b, scale, rMode) -> a.pow(b.intValue());
            case MULTIPLY -> (a, b, scale, rMode) -> a.multiply(b);
            case ADD -> (a, b, scale, rMode) -> a.add(b);
            case SUBTRACT -> (a, b, scale, rMode) -> a.subtract(b);
            case DIVIDE -> (a, b, scale, rMode) -> a.divide(b, scale, rMode);
            default -> throw new IllegalStateException("Unexpected value: " + op);
        };
    }

    private BigDecimal getCurrentVal(char[] chars, int start, int i, boolean isEnd, boolean isEndOfSubExpr) {
        int expLastChar = isEnd && !isEndOfSubExpr ? i : i - 1;
        int length = expLastChar + 1 - start;
        char[] newChars = new char[length];
        System.arraycopy(chars,start,newChars, 0, length);
        String currentTerm = new String(newChars);
        // check if term is a variable and if so, get it; else, treat term as value and create BigDecimal
        return Optional
                .ofNullable(vars.get(currentTerm))
                .orElseGet(() -> new BigDecimal(newChars));
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
        return c == POW || c == MULTIPLY || c == DIVIDE || c == SUBTRACT || c == ADD ;
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

        Node appendAndReturn(Node n) {
            this.next = n;
            this.next.prev = this;
            return n;
        }
    }

    private static class OperationsLists {
        List<Node> pow = new LinkedList<>();
        List<Node> multiply = new LinkedList<>();
        List<Node> divide = new LinkedList<>();
        List<Node> subtract = new LinkedList<>();
        List<Node> add = new LinkedList<>();
    }
}
