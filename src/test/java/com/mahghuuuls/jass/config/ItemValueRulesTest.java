package com.mahghuuuls.jass.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemValueRulesTest {

    @Test
    void overrideAppliesAndOthersFallBack() {
        ItemValueRules rules = ItemValueRules.parse("melee", new String[]{"minecraft:iron_sword=20"}, 0.0);
        assertEquals(20.0, rules.valueFor("minecraft:iron_sword", 0, 12.0), 0.0);
        assertEquals(12.0, rules.valueFor("minecraft:diamond_sword", 0, 12.0), 0.0);
        assertTrue(rules.warnings().isEmpty());
    }

    @Test
    void zeroMeansFree() {
        ItemValueRules rules = ItemValueRules.parse("melee", new String[]{"minecraft:iron_sword=0"}, 0.0);
        assertEquals(0.0, rules.valueFor("minecraft:iron_sword", 0, 12.0), 0.0);
    }

    @Test
    void exactMetadataWinsOverWildcard() {
        ItemValueRules rules = ItemValueRules.parse("melee",
                new String[]{"mod:club@*=5", "mod:club@2=9"}, 0.0);
        assertEquals(9.0, rules.valueFor("mod:club", 2, 12.0), 0.0);
        assertEquals(5.0, rules.valueFor("mod:club", 1, 12.0), 0.0);
    }

    @Test
    void wildcardOnlyLookupIgnoresMetadataEntries() {
        ItemValueRules rules = ItemValueRules.parse("melee",
                new String[]{"minecraft:iron_sword@0=20", "minecraft:gold_sword=7"}, 0.0);
        assertEquals(12.0, rules.valueFor("minecraft:iron_sword", ItemValueRules.ANY_METADATA, 12.0), 0.0);
        assertEquals(7.0, rules.valueFor("minecraft:gold_sword", ItemValueRules.ANY_METADATA, 12.0), 0.0);
    }

    @Test
    void invalidLinesAreSkippedWithOneWarningEach() {
        ItemValueRules rules = ItemValueRules.parse("melee", new String[]{
                "iron_sword=5", "minecraft:iron_sword", "minecraft:iron_sword=abc",
                "minecraft:iron_sword=-1", "minecraft:iron_sword@x=3", "", "  "}, 0.0);
        assertEquals(5, rules.warnings().size());
        assertEquals(12.0, rules.valueFor("minecraft:iron_sword", 0, 12.0), 0.0);
    }

    @Test
    void duplicatesKeepTheFirstEntry() {
        ItemValueRules rules = ItemValueRules.parse("melee",
                new String[]{"minecraft:stick=3", "minecraft:stick=4"}, 0.0);
        assertEquals(3.0, rules.valueFor("minecraft:stick", 0, 12.0), 0.0);
        assertEquals(1, rules.warnings().size());
    }

    @Test
    void registryNamesAreCaseInsensitive() {
        ItemValueRules rules = ItemValueRules.parse("melee", new String[]{"Minecraft:Stick=3"}, 0.0);
        assertEquals(3.0, rules.valueFor("minecraft:stick", 0, 12.0), 0.0);
    }
}
