package com.mahghuuuls.jass.core;

import java.util.EnumMap;
import java.util.Map;

/**
 * One contribution to a Stamina Profile: a value for some of the {@link StatField}s, plus a
 * short description of where it came from for {@code /stamina inspect}. Immutable.
 */
public final class ModifierSet {

    private final Map<StatField, Double> values;
    private final String source;

    public ModifierSet(Map<StatField, Double> values, String source) {
        this.values = values.isEmpty() ? new EnumMap<>(StatField.class) : new EnumMap<>(values);
        this.source = source;
    }

    /** The value for a field, or 0 when this contribution does not set it. */
    public double get(StatField field) {
        Double value = values.get(field);
        return value == null ? 0.0 : value;
    }

    public boolean has(StatField field) {
        return values.containsKey(field);
    }

    /** Where the contribution came from, such as {@code item minecraft:golden_boots (feet)}. */
    public String source() {
        return source;
    }

    /** The same values with another description. */
    public ModifierSet withSource(String newSource) {
        return new ModifierSet(values, newSource);
    }

    /** {@code field=value} pairs in field order, for explanations. */
    public String describeValues() {
        StringBuilder text = new StringBuilder();
        for (Map.Entry<StatField, Double> entry : values.entrySet()) {
            if (text.length() > 0) {
                text.append(';');
            }
            text.append(entry.getKey().configName()).append(':').append(entry.getValue());
        }
        return text.toString();
    }
}
