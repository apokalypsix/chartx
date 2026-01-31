package com.apokalypsix.chartx.chart.finance.indicator.dsl.ast;

import com.apokalypsix.chartx.chart.finance.indicator.dsl.EvaluationContext;

/**
 * AST node representing a constant number value.
 */
public class NumberNode implements ExpressionNode {

    private final float value;

    public NumberNode(float value) {
        this.value = value;
    }

    public float getValue() {
        return value;
    }

    @Override
    public float evaluate(EvaluationContext context, int index) {
        return value;
    }

    @Override
    public String toExpressionString() {
        if (value == (int) value) {
            return String.valueOf((int) value);
        }
        return String.valueOf(value);
    }
}
