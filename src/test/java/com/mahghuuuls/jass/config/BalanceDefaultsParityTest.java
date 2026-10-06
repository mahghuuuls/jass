package com.mahghuuuls.jass.config;

import com.mahghuuuls.jass.client.JassClientConfig;
import net.minecraftforge.common.config.Config;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Every configuration key the mod defines must ship with the default recorded in
 * {@code balance-defaults.csv}, the project's single source of default values.
 */
class BalanceDefaultsParityTest {

    private static final Class<?>[] CONFIG_CLASSES = {JassConfig.class, JassClientConfig.class};

    @Test
    void everyDefinedKeyMatchesTheBalanceTable() throws Exception {
        Map<String, String> table = readTable();
        int checked = 0;
        for (Class<?> configClass : CONFIG_CLASSES) {
            for (Field field : configClass.getDeclaredFields()) {
                if (!Modifier.isStatic(field.getModifiers()) || field.isSynthetic()) {
                    continue;
                }
                Config.Name name = field.getAnnotation(Config.Name.class);
                assertNotNull(name, configClass.getSimpleName() + "." + field.getName()
                        + " has no @Config.Name, so its key cannot be checked against the balance table");
                String expected = table.get(name.value());
                assertNotNull(expected, "balance-defaults.csv has no row for " + name.value());
                assertDefault(name.value(), expected, field.get(null));
                checked++;
            }
        }
        assertTrue(checked > 0, "no configuration keys found");
    }

    @Test
    void invalidValueFallbacksUseTheBalanceTableDefault() throws Exception {
        Map<String, String> table = readTable();
        assertEquals(Double.parseDouble(table.get("max_debt_fraction")), ConfigValues.DEFAULT_MAX_DEBT_FRACTION, 1e-9);
    }

    private static void assertDefault(String key, String expected, Object actual) {
        if (actual instanceof Double) {
            assertEquals(Double.parseDouble(expected), (Double) actual, 1e-9, key);
        } else if (actual instanceof Integer) {
            assertEquals(Integer.parseInt(expected), ((Integer) actual).intValue(), key);
        } else if (actual instanceof Boolean) {
            assertEquals(Boolean.parseBoolean(expected), actual, key);
        } else if (actual instanceof String[]) {
            String[] expectedLines = expected.isEmpty() ? new String[0] : expected.split(";");
            assertEquals(java.util.Arrays.asList(expectedLines), java.util.Arrays.asList((String[]) actual), key);
        } else if (actual instanceof Enum) {
            assertEquals(expected.toLowerCase(Locale.ROOT), ((Enum<?>) actual).name().toLowerCase(Locale.ROOT), key);
        } else {
            assertEquals(expected, String.valueOf(actual), key);
        }
    }

    private static Map<String, String> readTable() throws IOException {
        Map<String, String> table = new HashMap<>();
        InputStream in = BalanceDefaultsParityTest.class.getResourceAsStream("/balance-defaults.csv");
        assertNotNull(in, "balance-defaults.csv missing from test resources");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line = reader.readLine(); // header
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }
                String[] columns = line.split(",", -1);
                table.put(columns[0], columns[1]);
            }
        }
        return table;
    }
}
