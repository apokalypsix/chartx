package com.apokalypsix.chartx.chart.finance.indicator.dsl.ast;

import com.apokalypsix.chartx.chart.finance.indicator.dsl.EvaluationContext;

/**
 * AST node representing a price field reference.
 *
 * <p>Supported fields: open, high, low, close, volume, hl2, hlc3, ohlc4
 */
public class FieldNode implements ExpressionNode {

    /**
     * Available price fields.
     */
    public enum Field {
        OPEN("open"),
        HIGH("high"),
        LOW("low"),
        CLOSE("close"),
        VOLUME("volume"),
        HL2("hl2"),
        HLC3("hlc3"),
        OHLC4("ohlc4");

        private final String name;

        Field(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        /**
         * Parses a field name to a Field enum.
         */
        public static Field fromName(String name) {
            String lower = name.toLowerCase();
            for (Field f : values()) {
                if (f.name.equals(lower)) {
                    return f;
                }
            }
            // Aliases
            switch (lower) {
                case "o": return OPEN;
                case "h": return HIGH;
                case "l": return LOW;
                case "c": return CLOSE;
                case "v": return VOLUME;
                default:
                    throw new IllegalArgumentException("Unknown field: " + name);
            }
        }
    }

    private final Field field;
    private final int offset; // For accessing previous values like close[1]

    public FieldNode(Field field) {
        this(field, 0);
    }

    public FieldNode(Field field, int offset) {
        this.field = field;
        this.offset = offset;
    }

    public Field getField() {
        return field;
    }

    public int getOffset() {
        return offset;
    }

    @Override
    public float evaluate(EvaluationContext context, int index) {
        int adjustedIndex = index - offset;
        if (adjustedIndex < 0 || adjustedIndex >= context.getSize()) {
            return Float.NaN;
        }

        switch (field) {
            case OPEN:   return context.getOpen(adjustedIndex);
            case HIGH:   return context.getHigh(adjustedIndex);
            case LOW:    return context.getLow(adjustedIndex);
            case CLOSE:  return context.getClose(adjustedIndex);
            case VOLUME: return context.getVolume(adjustedIndex);
            case HL2:    return context.getHl2(adjustedIndex);
            case HLC3:   return context.getHlc3(adjustedIndex);
            case OHLC4:  return context.getOhlc4(adjustedIndex);
            default:
                return Float.NaN;
        }
    }

    @Override
    public String toExpressionString() {
        if (offset == 0) {
            return field.getName();
        }
        return field.getName() + "[" + offset + "]";
    }

    @Override
    public int getMinimumBars() {
        return offset + 1;
    }
}
