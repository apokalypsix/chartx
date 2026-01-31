package com.apokalypsix.chartx.chart.finance.indicator.dsl;

import com.apokalypsix.chartx.chart.finance.indicator.Indicator;
import com.apokalypsix.chartx.chart.finance.indicator.dsl.ast.ExpressionNode;
import com.apokalypsix.chartx.chart.data.OhlcData;
import com.apokalypsix.chartx.chart.data.XyData;

/**
 * Indicator implementation that evaluates a parsed expression.
 *
 * <p>This allows creating indicators from string expressions without writing Java code.
 *
 * <p>Example usage:
 * <pre>
 * // Create from expression string
 * ExpressionIndicator ind = new ExpressionIndicator("SMA(close, 20) + ATR(14) * 2");
 * XyData result = ind.calculate(ohlcData);
 *
 * // Or parse first for reuse
 * ExpressionNode ast = ExpressionParser.parse("EMA(close, 12) - EMA(close, 26)");
 * ExpressionIndicator macdLine = new ExpressionIndicator("MACD Line", ast);
 * </pre>
 *
 * <p>Supported elements:
 * <ul>
 *   <li><b>Price fields:</b> open, high, low, close, volume, hl2, hlc3, ohlc4</li>
 *   <li><b>Operators:</b> +, -, *, /, ^, %</li>
 *   <li><b>Functions:</b> abs(), sqrt(), log(), exp(), min(), max(), sign(), floor(), ceil(), round()</li>
 *   <li><b>Indicators:</b> SMA(), EMA(), WMA(), RSI(), ATR(), STDEV(), HIGHEST(), LOWEST()</li>
 *   <li><b>Offsets:</b> close[1] for previous bar</li>
 * </ul>
 */
public class ExpressionIndicator implements Indicator<OhlcData, XyData> {

    private final String name;
    private final String expressionString;
    private final ExpressionNode ast;

    /**
     * Creates an indicator from an expression string.
     *
     * @param expression the expression to evaluate
     */
    public ExpressionIndicator(String expression) {
        this(expression, expression, ExpressionParser.parse(expression));
    }

    /**
     * Creates an indicator with a custom name.
     *
     * @param name custom display name
     * @param expression the expression to evaluate
     */
    public ExpressionIndicator(String name, String expression) {
        this(name, expression, ExpressionParser.parse(expression));
    }

    /**
     * Creates an indicator from a pre-parsed AST.
     *
     * @param name display name
     * @param ast the parsed expression
     */
    public ExpressionIndicator(String name, ExpressionNode ast) {
        this(name, ast.toExpressionString(), ast);
    }

    private ExpressionIndicator(String name, String expressionString, ExpressionNode ast) {
        this.name = name;
        this.expressionString = expressionString;
        this.ast = ast;
    }

    /**
     * Returns the expression string.
     */
    public String getExpressionString() {
        return expressionString;
    }

    /**
     * Returns the parsed AST.
     */
    public ExpressionNode getAst() {
        return ast;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int getMinimumBars() {
        return ast.getMinimumBars();
    }

    @Override
    public XyData calculate(OhlcData source) {
        int size = source.size();
        String id = "expr_" + Integer.toHexString(expressionString.hashCode());
        XyData result = new XyData(id, name, size);

        if (size == 0) {
            return result;
        }

        EvaluationContext context = new EvaluationContext(source);
        long[] timestamps = source.getTimestampsArray();

        for (int i = 0; i < size; i++) {
            float value = ast.evaluate(context, i);
            result.append(timestamps[i], value);
        }

        return result;
    }

    @Override
    public void update(XyData result, OhlcData source, int fromIndex) {
        int resultSize = result.size();
        int sourceSize = source.size();

        if (sourceSize <= resultSize) {
            return;
        }

        // For efficiency, create a fresh context and evaluate only new bars
        // Note: This may not work correctly for expressions with lookbacks
        // that span across the boundary. For full correctness, recalculate all.
        EvaluationContext context = new EvaluationContext(source);
        long[] timestamps = source.getTimestampsArray();

        for (int i = resultSize; i < sourceSize; i++) {
            float value = ast.evaluate(context, i);
            result.append(timestamps[i], value);
        }
    }

    @Override
    public String toString() {
        return "ExpressionIndicator[" + expressionString + "]";
    }

    /**
     * Validates an expression without creating an indicator.
     *
     * @param expression the expression to validate
     * @return true if valid
     */
    public static boolean isValid(String expression) {
        try {
            ExpressionParser.parse(expression);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Validates an expression and returns an error message if invalid.
     *
     * @param expression the expression to validate
     * @return null if valid, error message if invalid
     */
    public static String validate(String expression) {
        try {
            ExpressionParser.parse(expression);
            return null;
        } catch (Exception e) {
            return e.getMessage();
        }
    }
}
