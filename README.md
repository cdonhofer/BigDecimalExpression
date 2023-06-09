# BigDecimalExpression
Simple, readable and reasonably fast BigDecimal usage in Java

Gradle:
```
implementation 'net.donhofer:big-decimal-expression:1.0.0'
```

Maven:
```
<dependency>
    <groupId>net.donhofer</groupId>
    <artifactId>big-decimal-expression</artifactId>
    <version>1.0.0</version>
</dependency>
```

# What it is
Allows you to write your BigDecimal code like this

```Java
    BigDecimal a = new BigDecimal("17000000000");
    BigDecimal b = new BigDecimal("1000000");
    BigDecimal c = new BigDecimal("18");
    BigDecimal d = new BigDecimal("5");
    BigDecimal e = new BigDecimal("13");
    BigDecimal f = new BigDecimal("1");
    BigDecimal g = new BigDecimal("10");
    BigDecimal h = new BigDecimal("2");
    Map<String, BigDecimal> params = Map.of(
        "a", a, "b", b, "c", c, "d", d, 
        "e", e, "f", f, "g", g, "h", h
    );
    
    BigDecimal result = new BigDecimalExpression(scale, roundingMode)
        .parse("(a/b+f)*g+(c-g/d-e)/h", params)
        .eval();
```


... instead of this
```Java
BigDecimal result = new BigDecimal("17000000000")
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
```

## Usage Examples
```Java
    // create reusable expression parser
    BigDecimalExpression bde = new BigDecimalExpression(scale, roundingMode);
    
    // call using a map
    BigDecimal result = bde.parse(
        "(a^2 + b^2) / c^2", 
        Map.of("a", new BigDecimal("3"), "b", new BigDecimal("2"),"c", new BigDecimal("4.5")
    )).eval();
    
    // call using map entries
    BigDecimal result = bde.parse(
        "(a^2 + b^2) / c^2",
        Map.entry("a", new BigDecimal("3")),
        Map.entry("b", new BigDecimal("2")),
        Map.entry("c", new BigDecimal("4.5"))
    ).eval();
    
    // call using raw values
    BigDecimal result = bde.parse("(3^2 + 2^2) / 4.5^2").eval();
```

## Validation and Error Handling

BigDecimalExpression, like BigDecimal, throws only unchecked exceptions. In situations that allow you
 to recover from errors, you can use the following mechanisms.

```Java
// use the state-testing method
BigDecimalExpression bde = new BigDecimalExpression(scale, roundingMode).parse(expression, params);
if(!bde.isValid()){
    // ...
}

// there are also more fine-grained checks
if(bde.containsIllegalChar(exp)) ... 
if(!bde.validateParentheses(exp)) ...  

etc.; See the test cases or the class itself for more possibilities.

// catch possible exceptions
try {
    result = bde.eval();
} catch (BigDecimalExpressionException bde) {
    // ...
}
```


## Operators (highest precedence first)
### PARENTHESES
* work as you would expect from any mathematical expression;
* terms/sub-expressions in parentheses are evaluated first, before outer operations are applied
### POW
* symbol: ^
* usage: a ^ b
* automatically uses the intValue of b, when b is a BigDecimal
### MULTIPLICATION
* symbol: * or parentheses for implicit multiplication
* usage: a * b OR a (b) or (c+d)(e-f) ...
### DIVISION
* symbol: /
* usage: a / b
### ADDITION
* symbol: +
* usage: a + b
### SUBTRACTION
* symbol: -
* usage: a - b

## Expressions
Expressions follow the usual mathematical rules(e.g. left-to-right evaluation). Allowed symbols are:
* variables (following the java rules for valid names): "[a-zA-Z_$][a-zA-Z_$0-9]*"
* operators, numbers, decimal point(.) and spaces: "[^a-zA-Z0-9.\\-+*/^_ ()]"
* formatting underscores are currently not supported (e.g. "a * 2_500_000")

## Speed
Currently, the test suite validates the runtime of BDE expressions to be no more than 2.5 times 
that of native BigDecimal. Usually, they are a lot closer, i.e. BDE is only 1.6 times the native implementation's duration, which
is quite a bit faster than other, even commercial, expression parsers.

Still, improving the speed of BigDecimalExpressionression is one of the main goals for future versions.

## Future improvements / ongoing work
* improve runtime
* publish to repos
* debug mode and tests: use logger instead of sysout
