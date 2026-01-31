package com.apokalypsix.chartx.chart.finance.indicator.dsl.ast;

import com.apokalypsix.chartx.chart.finance.indicator.dsl.EvaluationContext;

/**
 * AST node representing a binary operation.
 *
 * <p>Supports: +, -, *, /, ^, %, min, max
 */
public class BinaryOpNode implements ExpressionNode {

    /**
     * Available binary operators.
     */
    public enum Operator {
        ADD("+", 1),
        SUBTRACT("-", 1),
        MULTIPLY("*", 2),
        DIVIDE("/", 2),
        MODULO("%", 2),
        POWER("^", 3);

        private final String symbol;
        private final int precedence;

        Operator(String symbol, int precedence) {
            this.symbol = symbol;
            this.precedence = precedence;
        }

        public String getSymbol() {
            return symbol;
        }

        public int getPrecedence() {
            return precedence;
        }

        public static Operator fromSymbol(String symbol) {
            for (Operator op : values()) {
                if (op.symbol.equals(symbol)) {
                    return op;
                }
            }
            throw new IllegalArgumentException("Unknown operator: " + symbol);
        }
    }

    private final Operator operator;
    private final ExpressionNode left;
    private final ExpressionNode right;

    public BinaryOpNode(Operator operator, ExpressionNode left, ExpressionNode right) {
        this.operator = operator;
        this.left = left;
        this.right = right;
    }

    public Operator getOperator() {
        return operator;
    }

    public ExpressionNode getLeft() {
        return left;
    }

    public ExpressionNode getRight() {
        return right;
    }

    @Override
    public float evaluate(EvaluationContext context, int index) {
        float leftVal = left.evaluate(context, index);
        float rightVal = right.evaluate(context, index);

        if (Float.isNaN(leftVal) || Float.isNaN(rightVal)) {
            return Float.NaN;
        }

        switch (operator) {
            case ADD:      return leftVal + rightVal;
            case SUBTRACT: return leftVal - rightVal;
            case MULTIPLY: return leftVal * rightVal;
            case DIVIDE:
                if (rightVal == 0) return Float.NaN;
                return leftVal / rightVal;
            case MODULO:
                if (rightVal == 0) return Float.NaN;
                return leftVal % rightVal;
            case POWER:    return (float) Math.pow(leftVal, rightVal);
            default:
                return Float.NaN;
        }
    }

    @Override
    public String toExpressionString() {
        return "(" + left.toExpressionString() + " " + operator.getSymbol() +
                " " + right.toExpressionString() + ")";
    }

    @Override
    public int getMinimumBars() {
        return Math.max(left.getMinimumBars(), right.getMinimumBars());
    }
}
