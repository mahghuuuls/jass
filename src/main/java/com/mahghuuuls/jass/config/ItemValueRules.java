package com.mahghuuuls.jass.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * A per-item value list parsed from lines of the form {@code namespace:item[@metadata|*]=value}.
 * An entry with a metadata value wins over the same item's wildcard entry. Invalid lines are
 * skipped and reported; they never become a different value.
 */
public final class ItemValueRules {

    /** Metadata value that matches only an item's wildcard entry. */
    public static final int ANY_METADATA = -1;

    private final Map<String, Double> values;
    private final List<String> warnings;

    private ItemValueRules(Map<String, Double> values, List<String> warnings) {
        this.values = values;
        this.warnings = warnings;
    }

    public static ItemValueRules parse(String listName, String[] lines, double minimum) {
        Map<String, Double> values = new HashMap<>();
        List<String> warnings = new ArrayList<>();
        for (String raw : lines) {
            String line = raw == null ? "" : raw.trim();
            if (line.isEmpty()) {
                continue;
            }
            String problem = null;
            int equals = line.lastIndexOf('=');
            String itemPart = equals < 0 ? "" : line.substring(0, equals).trim();
            String valuePart = equals < 0 ? "" : line.substring(equals + 1).trim();
            String key = null;
            Double value = null;
            if (equals < 0) {
                problem = "missing '='";
            } else {
                key = itemKey(itemPart);
                if (key == null) {
                    problem = "item must be namespace:item with optional @metadata or @*";
                } else {
                    try {
                        value = Double.valueOf(valuePart);
                        if (value.isNaN() || value.isInfinite() || value < minimum) {
                            problem = "value must be a number of at least " + minimum;
                        }
                    } catch (NumberFormatException e) {
                        problem = "value is not a number";
                    }
                }
            }
            if (problem == null && values.containsKey(key)) {
                problem = "duplicate entry for " + itemPart + "; the first one is used";
            }
            if (problem != null) {
                warnings.add(listName + ": skipped \"" + line + "\": " + problem);
            } else {
                values.put(key, value);
            }
        }
        return new ItemValueRules(Collections.unmodifiableMap(values), Collections.unmodifiableList(warnings));
    }

    /** The configured value for an item, or {@code fallback} when no entry matches. */
    public double valueFor(String registryName, int metadata, double fallback) {
        if (metadata != ANY_METADATA) {
            Double exact = values.get(registryName + "@" + metadata);
            if (exact != null) {
                return exact;
            }
        }
        Double any = values.get(registryName + "@" + ANY_METADATA);
        return any != null ? any : fallback;
    }

    /** Registry names mentioned by the list, for unknown-item checks once items are registered. */
    public List<String> registryNames() {
        List<String> names = new ArrayList<>();
        for (String key : values.keySet()) {
            String name = key.substring(0, key.lastIndexOf('@'));
            if (!names.contains(name)) {
                names.add(name);
            }
        }
        return names;
    }

    /** Registry names that have an entry for one specific metadata value. */
    public List<String> namesWithMetadataEntries() {
        List<String> names = new ArrayList<>();
        for (String key : values.keySet()) {
            int at = key.lastIndexOf('@');
            String name = key.substring(0, at);
            if (!key.endsWith("@" + ANY_METADATA) && !names.contains(name)) {
                names.add(name);
            }
        }
        return names;
    }

    public List<String> warnings() {
        return warnings;
    }

    private static String itemKey(String itemPart) {
        String name = itemPart;
        int meta = ANY_METADATA;
        int at = itemPart.indexOf('@');
        if (at >= 0) {
            name = itemPart.substring(0, at).trim();
            String metaPart = itemPart.substring(at + 1).trim();
            if (!metaPart.equals("*")) {
                try {
                    meta = Integer.parseInt(metaPart);
                } catch (NumberFormatException e) {
                    return null;
                }
                if (meta < 0) {
                    return null;
                }
            }
        }
        name = name.toLowerCase(Locale.ROOT);
        int colon = name.indexOf(':');
        if (colon <= 0 || colon == name.length() - 1 || name.indexOf(':', colon + 1) >= 0) {
            return null;
        }
        return name + "@" + meta;
    }
}
