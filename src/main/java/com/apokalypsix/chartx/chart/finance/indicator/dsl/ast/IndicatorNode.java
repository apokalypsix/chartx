package com.apokalypsix.chartx.chart.finance.indicator.dsl.ast;

import java.util.Arrays;
import java.util.List;

import com.apokalypsix.chartx.chart.finance.indicator.dsl.EvaluationContext;

/**
 * AST node representing a built-in indicator function.
 *
 * <p>Supports common indicators: SMA, EMA, WMA, RSI, ATR, STDEV, HIGHEST, LOWEST
 *
 * <p>Example usage in expressions:
 * <ul>
 *   <li>SMA(close, 20) - 20-period SMA of close</li>
 *   <li>EMA(close, 12) - 12-period EMA of close</li>
 *   <li>ATR(14) - 14-period Average True Range</li>
 * </ul>
 */
public class IndicatorNode implements ExpressionNode {

    private final String name;
    private final ExpressionNode source;
    private final int period;

    public IndicatorNode(String name, ExpressionNode source, int period) {
        this.name = name.toUpperCase();
        this.source = source;
        this.period = period;
    }

    public String getName() {
        return name;
    }

    public ExpressionNode getSource() {
        return source;
    }

    public int getPeriod() {
        return period;
    }

    @Override
    public float evaluate(EvaluationContext context, int index) {
        // Try to use cached values first
        String cacheKey = name + "_" + period + "_" + source.toExpressionString();

        float[] cached = context.getCachedIndicator(cacheKey);
        if (cached != null && index < cached.length) {
            return cached[index];
        }

        // Calculate and cache the entire indicator
        int size = context.getSize();
        float[] values = new float[size];

        switch (name) {
            case "SMA":
                calculateSMA(context, values);
                break;
            case "EMA":
                calculateEMA(context, values);
                break;
            case "WMA":
                calculateWMA(context, values);
                break;
            case "STDEV":
                calculateStdDev(context, values);
                break;
            case "HIGHEST":
                calculateHighest(context, values);
                break;
            case "LOWEST":
                calculateLowest(context, values);
                break;
            case "ATR":
                calculateATR(context, values);
                break;
            case "RSI":
                calculateRSI(context, values);
                break;
            default:
                Arrays.fill(values, Float.NaN);
        }

        context.cacheIndicator(cacheKey, values);
        return index < values.length ? values[index] : Float.NaN;
    }

    private void calculateSMA(EvaluationContext context, float[] outValues) {
        int size = context.getSize();
        double sum = 0;

        for (int i = 0; i < size; i++) {
            float val = source.evaluate(context, i);
            if (Float.isNaN(val)) val = 0;
            sum += val;

            if (i < period - 1) {
                outValues[i] = Float.NaN;
            } else {
                if (i >= period) {
                    float oldVal = source.evaluate(context, i - period);
                    if (Float.isNaN(oldVal)) oldVal = 0;
                    sum -= oldVal;
                }
                outValues[i] = (float) (sum / period);
            }
        }
    }

    private void calculateEMA(EvaluationContext context, float[] outValues) {
        int size = context.getSize();
        double multiplier = 2.0 / (period + 1);
        double ema = 0;

        for (int i = 0; i < size; i++) {
            float val = source.evaluate(context, i);
            if (Float.isNaN(val)) val = 0;

            if (i < period - 1) {
                outValues[i] = Float.NaN;
                ema += val;
            } else if (i == period - 1) {
                ema = (ema + val) / period;
                outValues[i] = (float) ema;
            } else {
                ema = (val - ema) * multiplier + ema;
                outValues[i] = (float) ema;
            }
        }
    }

    private void calculateWMA(EvaluationContext context, float[] outValues) {
        int size = context.getSize();
        float weightSum = period * (period + 1) / 2.0f;

        for (int i = 0; i < size; i++) {
            if (i < period - 1) {
                outValues[i] = Float.NaN;
            } else {
                float sum = 0;
                int weight = 1;
                for (int j = i - period + 1; j <= i; j++) {
                    float val = source.evaluate(context, j);
                    if (!Float.isNaN(val)) {
                        sum += val * weight;
                    }
                    weight++;
                }
                outValues[i] = sum / weightSum;
            }
        }
    }

    private void calculateStdDev(EvaluationContext context, float[] outValues) {
        int size = context.getSize();

        for (int i = 0; i < size; i++) {
            if (i < period - 1) {
                outValues[i] = Float.NaN;
            } else {
                // Calculate mean
                double sum = 0;
                int count = 0;
                for (int j = i - period + 1; j <= i; j++) {
                    float val = source.evaluate(context, j);
                    if (!Float.isNaN(val)) {
                        sum += val;
                        count++;
                    }
                }

                if (count == 0) {
                    outValues[i] = Float.NaN;
                    continue;
                }

                double mean = sum / count;

                // Calculate variance
                double variance = 0;
                for (int j = i - period + 1; j <= i; j++) {
                    float val = source.evaluate(context, j);
                    if (!Float.isNaN(val)) {
                        double diff = val - mean;
                        variance += diff * diff;
                    }
                }

                outValues[i] = (float) Math.sqrt(variance / count);
            }
        }
    }

    private void calculateHighest(EvaluationContext context, float[] outValues) {
        int size = context.getSize();

        for (int i = 0; i < size; i++) {
            if (i < period - 1) {
                outValues[i] = Float.NaN;
            } else {
                float max = Float.NEGATIVE_INFINITY;
                for (int j = i - period + 1; j <= i; j++) {
                    float val = source.evaluate(context, j);
                    if (!Float.isNaN(val) && val > max) {
                        max = val;
                    }
                }
                outValues[i] = max == Float.NEGATIVE_INFINITY ? Float.NaN : max;
            }
        }
    }

    private void calculateLowest(EvaluationContext context, float[] outValues) {
        int size = context.getSize();

        for (int i = 0; i < size; i++) {
            if (i < period - 1) {
                outValues[i] = Float.NaN;
            } else {
                float min = Float.POSITIVE_INFINITY;
                for (int j = i - period + 1; j <= i; j++) {
                    float val = source.evaluate(context, j);
                    if (!Float.isNaN(val) && val < min) {
                        min = val;
                    }
                }
                outValues[i] = min == Float.POSITIVE_INFINITY ? Float.NaN : min;
            }
        }
    }

    private void calculateATR(EvaluationContext context, float[] outValues) {
        int size = context.getSize();
        float prevClose = 0;
        double atr = 0;

        for (int i = 0; i < size; i++) {
            float high = context.getHigh(i);
            float low = context.getLow(i);
            float close = context.getClose(i);

            float tr;
            if (i == 0) {
                tr = high - low;
            } else {
                tr = Math.max(high - low, Math.max(
                        Math.abs(high - prevClose),
                        Math.abs(low - prevClose)));
            }
            prevClose = close;

            if (i < period) {
                atr += tr;
                outValues[i] = Float.NaN;
            } else if (i == period) {
                atr = (atr + tr) / period;
                outValues[i] = (float) atr;
            } else {
                atr = (atr * (period - 1) + tr) / period;
                outValues[i] = (float) atr;
            }
        }
    }

    private void calculateRSI(EvaluationContext context, float[] outValues) {
        int size = context.getSize();
        double avgGain = 0, avgLoss = 0;

        outValues[0] = Float.NaN;

        for (int i = 1; i < size; i++) {
            float curr = source.evaluate(context, i);
            float prev = source.evaluate(context, i - 1);

            if (Float.isNaN(curr) || Float.isNaN(prev)) {
                outValues[i] = Float.NaN;
                continue;
            }

            float change = curr - prev;
            float gain = Math.max(0, change);
            float loss = Math.max(0, -change);

            if (i < period) {
                avgGain += gain;
                avgLoss += loss;
                outValues[i] = Float.NaN;
            } else if (i == period) {
                avgGain = (avgGain + gain) / period;
                avgLoss = (avgLoss + loss) / period;
                outValues[i] = avgLoss == 0 ? 100 : (float) (100 - 100 / (1 + avgGain / avgLoss));
            } else {
                avgGain = (avgGain * (period - 1) + gain) / period;
                avgLoss = (avgLoss * (period - 1) + loss) / period;
                outValues[i] = avgLoss == 0 ? 100 : (float) (100 - 100 / (1 + avgGain / avgLoss));
            }
        }
    }

    @Override
    public String toExpressionString() {
        if (source != null) {
            return name + "(" + source.toExpressionString() + ", " + period + ")";
        }
        return name + "(" + period + ")";
    }

    @Override
    public int getMinimumBars() {
        int sourceMin = source != null ? source.getMinimumBars() : 1;
        return Math.max(period, sourceMin);
    }
}
