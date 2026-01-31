package com.apokalypsix.chartx.chart.finance.indicator.dsl;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.apokalypsix.chartx.chart.finance.indicator.dsl.ast.*;

/**
 * Parser for indicator expressions.
 *
 * <p>Parses expressions like:
 * <ul>
 *   <li>{@code close} - just the close price</li>
 *   <li>{@code SMA(close, 20)} - 20-period SMA of close</li>
 *   <li>{@code EMA(close, 12) - EMA(close, 26)} - MACD line approximation</li>
 *   <li>{@code SMA(close, 20) + ATR(14) * 2} - custom bands</li>
 * </ul>
 *
 * <p>Grammar (simplified):
 * <pre>
 * expression := term (('+' | '-') term)*
 * term       := factor (('*' | '/') factor)*
 * factor     := base ('^' factor)?
 * base       := number | field | function | indicator | '(' expression ')'
 * function   := IDENTIFIER '(' expression (',' expression)* ')'
 * indicator  := INDICATOR '(' field ',' number ')' | INDICATOR '(' number ')'
 * field      := 'open' | 'high' | 'low' | 'close' | 'volume' | 'hl2' | 'hlc3' | 'ohlc4'
 * </pre>
 */
public class ExpressionParser {

    private static final Set<String> FIELDS = Set.of(
            "open", "high", "low", "close", "volume",
            "hl2", "hlc3", "ohlc4",
            "o", "h", "l", "c", "v"
    );

    private static final Set<String> INDICATORS = Set.of(
            "sma", "ema", "wma", "rsi", "atr", "stdev", "highest", "lowest"
    );

    private static final Set<String> FUNCTIONS = Set.of(
            "abs", "sqrt", "log", "ln", "exp", "min", "max",
            "sign", "floor", "ceil", "round", "pow"
    );

    private static final Pattern TOKEN_PATTERN = Pattern.compile(
            "\\s*(" +
                    "[0-9]+\\.?[0-9]*|" +  // Numbers
                    "[a-zA-Z_][a-zA-Z0-9_]*|" +  // Identifiers
                    "[+\\-*/^%()]|" +  // Operators
                    ",|" +  // Comma
                    "\\[|\\]" +  // Brackets for offset
                    ")\\s*"
    );

    private final String expression;
    private final List<String> tokens;
    private int pos;

    /**
     * Parses an expression string into an AST.
     *
     * @param expression the expression to parse
     * @return the root node of the AST
     * @throws IllegalArgumentException if the expression is invalid
     */
    public static ExpressionNode parse(String expression) {
        return new ExpressionParser(expression).parseExpression();
    }

    private ExpressionParser(String expression) {
        this.expression = expression;
        this.tokens = tokenize(expression);
        this.pos = 0;
    }

    private List<String> tokenize(String expr) {
        List<String> result = new ArrayList<>();
        Matcher matcher = TOKEN_PATTERN.matcher(expr);
        int lastEnd = 0;

        while (matcher.find()) {
            if (matcher.start() != lastEnd) {
                throw new IllegalArgumentException(
                        "Invalid character at position " + lastEnd + ": " + expr.charAt(lastEnd));
            }
            String token = matcher.group(1);
            if (!token.isEmpty()) {
                result.add(token);
            }
            lastEnd = matcher.end();
        }

        if (lastEnd != expr.length()) {
            throw new IllegalArgumentException(
                    "Invalid character at position " + lastEnd);
        }

        return result;
    }

    private String peek() {
        return pos < tokens.size() ? tokens.get(pos) : null;
    }

    private String consume() {
        return pos < tokens.size() ? tokens.get(pos++) : null;
    }

    private boolean match(String expected) {
        if (expected.equals(peek())) {
            consume();
            return true;
        }
        return false;
    }

    private void expect(String expected) {
        String token = consume();
        if (!expected.equals(token)) {
            throw new IllegalArgumentException(
                    "Expected '" + expected + "' but got '" + token + "'");
        }
    }

    private ExpressionNode parseExpression() {
        ExpressionNode left = parseTerm();

        while (true) {
            String op = peek();
            if ("+".equals(op) || "-".equals(op)) {
                consume();
                ExpressionNode right = parseTerm();
                left = new BinaryOpNode(
                        BinaryOpNode.Operator.fromSymbol(op), left, right);
            } else {
                break;
            }
        }

        return left;
    }

    private ExpressionNode parseTerm() {
        ExpressionNode left = parseFactor();

        while (true) {
            String op = peek();
            if ("*".equals(op) || "/".equals(op) || "%".equals(op)) {
                consume();
                ExpressionNode right = parseFactor();
                left = new BinaryOpNode(
                        BinaryOpNode.Operator.fromSymbol(op), left, right);
            } else {
                break;
            }
        }

        return left;
    }

    private ExpressionNode parseFactor() {
        ExpressionNode base = parseBase();

        if ("^".equals(peek())) {
            consume();
            ExpressionNode exponent = parseFactor(); // Right-associative
            return new BinaryOpNode(BinaryOpNode.Operator.POWER, base, exponent);
        }

        return base;
    }

    private ExpressionNode parseBase() {
        String token = peek();

        if (token == null) {
            throw new IllegalArgumentException("Unexpected end of expression");
        }

        // Unary minus
        if ("-".equals(token)) {
            consume();
            ExpressionNode operand = parseBase();
            return new BinaryOpNode(BinaryOpNode.Operator.SUBTRACT,
                    new NumberNode(0), operand);
        }

        // Parentheses
        if ("(".equals(token)) {
            consume();
            ExpressionNode inner = parseExpression();
            expect(")");
            return inner;
        }

        // Number
        if (isNumber(token)) {
            consume();
            return new NumberNode(Float.parseFloat(token));
        }

        // Identifier (field, function, or indicator)
        if (isIdentifier(token)) {
            consume();
            String lower = token.toLowerCase();

            // Check if followed by parentheses (function/indicator call)
            if ("(".equals(peek())) {
                consume(); // consume (

                if (INDICATORS.contains(lower)) {
                    return parseIndicatorCall(lower);
                } else if (FUNCTIONS.contains(lower)) {
                    return parseFunctionCall(lower);
                } else {
                    throw new IllegalArgumentException("Unknown function: " + token);
                }
            }

            // Must be a field
            if (FIELDS.contains(lower)) {
                int offset = 0;
                // Check for offset like close[1]
                if ("[".equals(peek())) {
                    consume();
                    String offsetStr = consume();
                    if (!isNumber(offsetStr)) {
                        throw new IllegalArgumentException("Expected number in offset");
                    }
                    offset = Integer.parseInt(offsetStr);
                    expect("]");
                }
                return new FieldNode(FieldNode.Field.fromName(lower), offset);
            }

            throw new IllegalArgumentException("Unknown identifier: " + token);
        }

        throw new IllegalArgumentException("Unexpected token: " + token);
    }

    private ExpressionNode parseIndicatorCall(String name) {
        List<ExpressionNode> args = parseArguments();

        ExpressionNode source;
        int period;

        if (args.size() == 1) {
            // ATR(14) style - single numeric argument
            if (args.get(0) instanceof NumberNode) {
                source = new FieldNode(FieldNode.Field.CLOSE);
                period = (int) ((NumberNode) args.get(0)).getValue();
            } else {
                throw new IllegalArgumentException(
                        name + " with single argument requires a number");
            }
        } else if (args.size() >= 2) {
            // SMA(close, 20) style
            source = args.get(0);
            if (args.get(1) instanceof NumberNode) {
                period = (int) ((NumberNode) args.get(1)).getValue();
            } else {
                throw new IllegalArgumentException(
                        name + " requires a numeric period");
            }
        } else {
            throw new IllegalArgumentException(
                    name + " requires at least one argument");
        }

        return new IndicatorNode(name, source, period);
    }

    private ExpressionNode parseFunctionCall(String name) {
        List<ExpressionNode> args = parseArguments();
        return new FunctionNode(name, args);
    }

    private List<ExpressionNode> parseArguments() {
        List<ExpressionNode> args = new ArrayList<>();

        if (!")".equals(peek())) {
            args.add(parseExpression());
            while (",".equals(peek())) {
                consume();
                args.add(parseExpression());
            }
        }

        expect(")");
        return args;
    }

    private boolean isNumber(String token) {
        if (token == null || token.isEmpty()) return false;
        try {
            Float.parseFloat(token);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean isIdentifier(String token) {
        if (token == null || token.isEmpty()) return false;
        char first = token.charAt(0);
        return Character.isLetter(first) || first == '_';
    }
}
