package com.apokalypsix.chartx.chart.finance.indicator.dsl.ast;

import com.apokalypsix.chartx.chart.finance.indicator.dsl.EvaluationContext;

/**
 * Base interface for expression AST nodes.
 *
 * <p>Each node represents part of an indicator expression and can be
 * evaluated to produce a value at a specific bar index.
 */
public interface ExpressionNode {

    /**
     * Evaluates this expression at the specified bar index.
     *
     * @param context the evaluation context containing price data
     * @param index the bar index to evaluate at
     * @return the computed value, or Float.NaN if not available
     */
    float evaluate(EvaluationContext context, int index);

    /**
     * Returns a string representation of this expression.
     */
    String toExpressionString();

    /**
     * Returns the minimum number of bars needed before this expression
     * can produce valid values.
     */
    default int getMinimumBars() {
        return 1;
    }
}
