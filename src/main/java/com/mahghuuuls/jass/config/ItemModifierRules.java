package com.mahghuuuls.jass.config;

import com.mahghuuuls.jass.core.ModifierSet;
import com.mahghuuuls.jass.core.StatField;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Parsed item Stamina modifier rules (REQ-040): {@code namespace:item[@metadata or @*]=field:value;...}.
 * A line with any problem (bad item, unknown field, bad number, a reduction outside 0 to 1, a field
 * given twice) is skipped whole with one warning naming it, never partly applied.
 */
public final class ItemModifierRules implements ItemList {

    private final Map<String, ModifierSet> rules;
    private final List<String> warnings;

    private ItemModifierRules(Map<String, ModifierSet> rules, List<String> warnings) {
        this.rules = rules;
        this.warnings = warnings;
    }

    public static ItemModifierRules parse(String listName, String[] lines) {
        Map<String, ModifierSet> rules = new HashMap<>();
        List<String> warnings = new ArrayList<>();
        for (String raw : lines) {
            String line = raw == null ? "" : raw.trim();
            if (line.isEmpty()) {
                continue;
            }
            String problem = null;
            int equals = line.indexOf('=');
            String key = equals < 0 ? null : ItemValueRules.itemKey(line.substring(0, equals).trim());
            Map<StatField, Double> values = new EnumMap<>(StatField.class);
            if (equals < 0) {
                problem = "missing '='";
            } else if (key == null) {
                problem = "item must be namespace:item with optional @metadata or @*";
            } else {
                problem = parseFields(line.substring(equals + 1), values);
            }
            if (problem == null && rules.containsKey(key)) {
                problem = "duplicate rule for this item; the first one is used";
            }
            if (problem != null) {
                warnings.add(listName + ": skipped \"" + line + "\": " + problem);
            } else {
                rules.put(key, new ModifierSet(values, line.substring(0, equals).trim()));
            }
        }
        return new ItemModifierRules(Collections.unmodifiableMap(rules), Collections.unmodifiableList(warnings));
    }

    private static String parseFields(String text, Map<StatField, Double> values) {
        for (String part : text.split(";")) {
            String entry = part.trim();
            if (entry.isEmpty()) {
                continue;
            }
            int colon = entry.indexOf(':');
            if (colon < 0) {
                return "\"" + entry + "\" must be field:value";
            }
            StatField field = StatField.byConfigName(entry.substring(0, colon).trim());
            if (field == null) {
                return "unknown field \"" + entry.substring(0, colon).trim() + "\"";
            }
            if (values.containsKey(field)) {
                return field.configName() + " is given twice";
            }
            double value;
            try {
                value = Double.parseDouble(entry.substring(colon + 1).trim());
            } catch (NumberFormatException e) {
                return field.configName() + " value is not a number";
            }
            if (Double.isNaN(value) || Double.isInfinite(value)) {
                return field.configName() + " value is not a finite number";
            }
            if (field.isReduction() && (value < 0.0 || value > 1.0)) {
                return field.configName() + " must be a fraction from 0 to 1";
            }
            values.put(field, value);
        }
        return values.isEmpty() ? "no field:value pairs" : null;
    }

    /** The rule for an item (exact metadata first, then the wildcard), or {@code null}. */
    public ModifierSet ruleFor(String registryName, int metadata) {
        if (metadata != ItemValueRules.ANY_METADATA) {
            ModifierSet exact = rules.get(registryName + "@" + metadata);
            if (exact != null) {
                return exact;
            }
        }
        return rules.get(registryName + "@" + ItemValueRules.ANY_METADATA);
    }

    public boolean isEmpty() {
        return rules.isEmpty();
    }

    @Override
    public List<String> registryNames() {
        return ItemValueRules.namesOf(rules.keySet(), false);
    }

    @Override
    public List<String> namesWithMetadataEntries() {
        return ItemValueRules.namesOf(rules.keySet(), true);
    }

    @Override
    public List<String> warnings() {
        return warnings;
    }
}
