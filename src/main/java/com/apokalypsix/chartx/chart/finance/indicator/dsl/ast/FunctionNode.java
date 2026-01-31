package com.apokalypsix.chartx.chart.finance.indicator.dsl.ast;

import java.util.List;

import com.apokalypsix.chartx.chart.finance.indicator.dsl.EvaluationContext;

/**
 * AST node representing a function call.
 *
 * <p>Supports mathematical functions: abs, sqrt, log, exp, min, max, sign
 */
public class FunctionNode implements ExpressionNode {

    private final String name;
    private final List<ExpressionNode> arguments;

    public FunctionNode(String name, List<ExpressionNode> arguments) {
        this.name = name.toLowerCase();
        this.arguments = arguments;
    }

    public String getName() {
        return name;
    }

    public List<ExpressionNode> getArguments() {
        return arguments;
    }

    @Override
    public float evaluate(EvaluationContext context, int index) {
        switch (name) {
            case "abs":
                return evaluateAbs(context, index);
            case "sqrt":
                return evaluateSqrt(context, index);
            case "log":
                return evaluateLog(context, index);
            case "ln":
                return evaluateLn(context, index);
            case "exp":
                return evaluateExp(context, index);
            case "min":
                return evaluateMin(context, index);
            case "max":
                return evaluateMax(context, index);
            case "sign":
                return evaluateSign(context, index);
            case "floor":
                return evaluateFloor(context, index);
            case "ceil":
                return evaluateCeil(context, index);
            case "round":
                return evaluateRound(context, index);
            case "pow":
                return evaluatePow(context, index);
            default:
                throw new IllegalArgumentException("Unknown function: " + name);
        }
    }

    private float evaluateAbs(EvaluationContext context, int index) {
        requireArgs(1);
        float val = arguments.get(0).evaluate(context, index);
        return Float.isNaN(val) ? Float.NaN : Math.abs(val);
    }

    private float evaluateSqrt(EvaluationContext context, int index) {
        requireArgs(1);
        float val = arguments.get(0).evaluate(context, index);
        if (Float.isNaN(val) || val < 0) return Float.NaN;
        return (float) Math.sqrt(val);
    }

    private float evaluateLog(EvaluationContext context, int index) {
        requireArgs(1);
        float val = arguments.get(0).evaluate(context, index);
        if (Float.isNaN(val) || val <= 0) return Float.NaN;
        return (float) Math.log10(val);
    }

    private float evaluateLn(EvaluationContext context, int index) {
        requireArgs(1);
        float val = arguments.get(0).evaluate(context, index);
        if (Float.isNaN(val) || val <= 0) return Float.NaN;
        return (float) Math.log(val);
    }

    private float evaluateExp(EvaluationContext context, int index) {
        requireArgs(1);
        float val = arguments.get(0).evaluate(context, index);
        return Float.isNaN(val) ? Float.NaN : (float) Math.exp(val);
    }

    private float evaluateMin(EvaluationContext context, int index) {
        if (arguments.isEmpty()) return Float.NaN;
        float result = Float.POSITIVE_INFINITY;
        for (ExpressionNode arg : arguments) {
            float val = arg.evaluate(context, index);
            if (!Float.isNaN(val) && val < result) {
                result = val;
            }
        }
        return result == Float.POSITIVE_INFINITY ? Float.NaN : result;
    }

    private float evaluateMax(EvaluationContext context, int index) {
        if (arguments.isEmpty()) return Float.NaN;
        float result = Float.NEGATIVE_INFINITY;
        for (ExpressionNode arg : arguments) {
            float val = arg.evaluate(context, index);
            if (!Float.isNaN(val) && val > result) {
                result = val;
            }
        }
        return result == Float.NEGATIVE_INFINITY ? Float.NaN : result;
    }

    private float evaluateSign(EvaluationContext context, int index) {
        requireArgs(1);
        float val = arguments.get(0).evaluate(context, index);
        if (Float.isNaN(val)) return Float.NaN;
        return Math.signum(val);
    }

    private float evaluateFloor(EvaluationContext context, int index) {
        requireArgs(1);
        float val = arguments.get(0).evaluate(context, index);
        return Float.isNaN(val) ? Float.NaN : (float) Math.floor(val);
    }

    private float evaluateCeil(EvaluationContext context, int index) {
        requireArgs(1);
        float val = arguments.get(0).evaluate(context, index);
        return Float.isNaN(val) ? Float.NaN : (float) Math.ceil(val);
    }

    private float evaluateRound(EvaluationContext context, int index) {
        requireArgs(1);
        float val = arguments.get(0).evaluate(context, index);
        return Float.isNaN(val) ? Float.NaN : Math.round(val);
    }

    private float evaluatePow(EvaluationContext context, int index) {
        requireArgs(2);
        float base = arguments.get(0).evaluate(context, index);
        float exp = arguments.get(1).evaluate(context, index);
        if (Float.isNaN(base) || Float.isNaN(exp)) return Float.NaN;
        return (float) Math.pow(base, exp);
    }

    private void requireArgs(int count) {
        if (arguments.size() < count) {
            throw new IllegalArgumentException(
                    name + " requires at least " + count + " argument(s)");
        }
    }

    @Override
    public String toExpressionString() {
        StringBuilder sb = new StringBuilder(name);
        sb.append("(");
        for (int i = 0; i < arguments.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(arguments.get(i).toExpressionString());
        }
        sb.append(")");
        return sb.toString();
    }

    @Override
    public int getMinimumBars() {
        int max = 1;
        for (ExpressionNode arg : arguments) {
            max = Math.max(max, arg.getMinimumBars());
        }
        return max;
    }
}
