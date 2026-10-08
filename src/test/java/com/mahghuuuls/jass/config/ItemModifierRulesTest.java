package com.mahghuuuls.jass.config;

import com.mahghuuuls.jass.core.ModifierSet;
import com.mahghuuuls.jass.core.StatField;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemModifierRulesTest {

    private static final double EPS = 1e-9;

    @Test
    void parsesSeveralFields() {
        ItemModifierRules rules = ItemModifierRules.parse("item_stamina_modifiers",
                new String[]{"minecraft:golden_boots=flat_regeneration:25;delay_reduction:0.5"});
        ModifierSet set = rules.ruleFor("minecraft:golden_boots", ItemValueRules.ANY_METADATA);
        assertNotNull(set);
        assertEquals(25.0, set.get(StatField.FLAT_REGENERATION), EPS);
        assertEquals(0.5, set.get(StatField.DELAY_REDUCTION), EPS);
        assertTrue(rules.warnings().isEmpty());
    }

    @Test
    void exactMetadataWinsOverTheWildcard() {
        ItemModifierRules rules = ItemModifierRules.parse("item_stamina_modifiers",
                new String[]{"minecraft:wool@*=efficiency:10", "minecraft:wool@14=efficiency:50"});
        assertEquals(50.0, rules.ruleFor("minecraft:wool", 14).get(StatField.EFFICIENCY), EPS);
        assertEquals(10.0, rules.ruleFor("minecraft:wool", 3).get(StatField.EFFICIENCY), EPS);
    }

    @Test
    void malformedLinesAreSkippedWholeWithOneWarningEach() {
        ItemModifierRules rules = ItemModifierRules.parse("item_stamina_modifiers", new String[]{
                "minecraft:stick=flat_maximum:20;speed:3",
                "minecraft:bone=maximum_reduction:1.5",
                "minecraft:apple flat_maximum:5",
                "minecraft:carrot=efficiency:abc",
                "notanitem=efficiency:5"});
        assertEquals(5, rules.warnings().size());
        assertNull(rules.ruleFor("minecraft:stick", ItemValueRules.ANY_METADATA));
        assertTrue(rules.isEmpty());
        assertTrue(rules.warnings().get(0).contains("minecraft:stick=flat_maximum:20;speed:3"));
    }

    @Test
    void itemsWithoutARuleHaveNone() {
        ItemModifierRules rules = ItemModifierRules.parse("item_stamina_modifiers", new String[0]);
        assertNull(rules.ruleFor("minecraft:stick", 0));
    }
}
