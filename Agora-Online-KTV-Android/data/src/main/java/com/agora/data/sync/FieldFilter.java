package com.agora.data.sync;

/**
 * Represents a filter to be applied to query.
 */
public class FieldFilter {
    public enum Operator {
        LESS_THAN("<"),
        LESS_THAN_OR_EQUAL("<="),
        EQUAL("=="),
        NOT_EQUAL("!="),
        GREATER_THAN(">"),
        GREATER_THAN_OR_EQUAL(">="),
        ARRAY_CONTAINS("array_contains"),
        ARRAY_CONTAINS_ANY("array_contains_any"),
        IN("in"),
        NOT_IN("not_in");

        private final String text;

        Operator(String text) {
            this.text = text;
        }

        @Override
        public String toString() {
            return text;
        }
    }

    private final Operator operator;

    private final String field;
    private final Object value;

    /**
     * Creates a new filter that compares fields and values. Only intended to be called from
     * Filter.create().
     */
    protected FieldFilter(Operator operator, String field, Object value) {
        this.operator = operator;
        this.field = field;
        this.value = value;
    }

    public Operator getOperator() {
        return operator;
    }

    public Object getValue() {
        return value;
    }

    public String getField() {
        return field;
    }

    public static FieldFilter create(Operator operator, String field, Object value) {
        return new FieldFilter(operator, field, value);
    }
}
