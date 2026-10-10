package com.mahghuuuls.jass.client;

import com.mahghuuuls.jass.api.client.ItemStaminaInfo;
import com.mahghuuuls.jass.core.StatField;
import com.mahghuuuls.jass.network.ItemValuesMessage;
import com.mahghuuuls.jass.network.ItemValuesMessage.Lines;
import com.mahghuuuls.jass.network.ItemValuesMessage.Number;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.util.text.TextFormatting;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TooltipLinesTest {

    /** Shows the key and arguments, so tests check text without the game's language files. */
    private static final TooltipLines.Translator T = (key, args) -> key + Arrays.toString(args);

    private static final String GRAY = TextFormatting.GRAY.toString();
    private static final String BLUE = TextFormatting.BLUE.toString();
    private static final String RED = TextFormatting.RED.toString();

    @Test
    void weaponBowShieldAndArmorLines() {
        ItemStaminaInfo info = new ItemStaminaInfo.Builder()
                .swingCost(12).drawPerSecond(8).blocking(50, 1.0, 0.5, 100.0 / 150.0).weight(6, false).build();
        List<String> lines = TooltipLines.lines(info, T);
        assertEquals(Arrays.asList(
                GRAY + "tooltip.jass.swing[12]",
                GRAY + "tooltip.jass.draw[8]",
                GRAY + "tooltip.jass.stability[50]",
                GRAY + "tooltip.jass.blocks[100%, 50%]",
                GRAY + "tooltip.jass.block_cost[0.67]",
                GRAY + "tooltip.jass.weight[6]"), lines);
    }

    @Test
    void freeDrawAndElenaiWeightAsAPlainNumber() {
        ItemStaminaInfo info = new ItemStaminaInfo.Builder().drawPerSecond(0).weight(7.0, true).build();
        assertEquals(Arrays.asList(GRAY + "tooltip.jass.draw_free[]", GRAY + "tooltip.jass.weight[7]"),
                TooltipLines.lines(info, T));
    }

    @Test
    void modifierLinesUseJawmsColors() {
        ItemStaminaInfo info = new ItemStaminaInfo.Builder()
                .modifier("flat_maximum", 20, ItemInfoResolver.beneficial(StatField.FLAT_MAXIMUM, 20))
                .modifier("flat_delay", 0.5, ItemInfoResolver.beneficial(StatField.FLAT_DELAY, 0.5))
                .modifier("regeneration_increase", 0.2, ItemInfoResolver.beneficial(StatField.REGENERATION_INCREASE, 0.2))
                .modifier("maximum_reduction", 0.25, ItemInfoResolver.beneficial(StatField.MAXIMUM_REDUCTION, 0.25))
                .modifier("delay_reduction", 0.5, ItemInfoResolver.beneficial(StatField.DELAY_REDUCTION, 0.5))
                .modifier("efficiency", -10, ItemInfoResolver.beneficial(StatField.EFFICIENCY, -10))
                .build();
        assertEquals(Arrays.asList(
                BLUE + "tooltip.jass.stat.maximum[+20]",
                RED + "tooltip.jass.stat.delay[+0.5 s]",
                BLUE + "tooltip.jass.stat.regeneration[+20%]",
                RED + "tooltip.jass.stat.maximum[-25%]",
                BLUE + "tooltip.jass.stat.delay[-50%]",
                RED + "tooltip.jass.stat.efficiency[-10]"), TooltipLines.lines(info, T));
    }

    @Test
    void beneficialSides() {
        assertTrue(ItemInfoResolver.beneficial(StatField.FLAT_REGENERATION, 5));
        assertFalse(ItemInfoResolver.beneficial(StatField.REGENERATION_REDUCTION, 0.1));
        assertTrue(ItemInfoResolver.beneficial(StatField.FLAT_DELAY, -0.25));
        assertFalse(ItemInfoResolver.beneficial(StatField.DELAY_INCREASE, 0.3));
        assertFalse(ItemInfoResolver.beneficial(StatField.MAXIMUM_INCREASE, -0.1));
    }

    @Test
    void numbersHaveAtMostTwoDecimals() {
        assertEquals("12", TooltipLines.number(12.0));
        assertEquals("0.67", TooltipLines.number(2.0 / 3.0));
        assertEquals("22.4", TooltipLines.number(22.4));
        assertEquals("0", TooltipLines.number(0.0001));
        assertEquals("?", TooltipLines.number(Double.NaN));
        assertEquals("?", TooltipLines.number(Double.POSITIVE_INFINITY));
    }

    @Test
    void tinyNegativeValuesShowNoMinusZero() {
        ItemStaminaInfo.Modifier m = new ItemStaminaInfo.Modifier("flat_maximum", -0.0001, false);
        assertEquals("tooltip.jass.stat.maximum[+0]", TooltipLines.modifierText(m, T));
    }

    @Test
    void emptyInfoHasNoLines() {
        assertTrue(ItemStaminaInfo.NONE.isEmpty());
        assertTrue(TooltipLines.lines(ItemStaminaInfo.NONE, T).isEmpty());
    }

    @Test
    void messageRoundTripKeepsEveryValue() {
        ItemValuesMessage out = new ItemValuesMessage()
                .set(Number.DEFAULT_MELEE_COST, 20)
                .set(Number.ELENAI_WEIGHT_CONVERSION, 1.4)
                .set(Lines.MELEE_COST_OVERRIDES, new String[] {"minecraft:iron_sword=15", "spartanweaponry:dagger_iron=4"})
                .set(Lines.ITEM_STAMINA_MODIFIERS, new String[] {"minecraft:golden_chestplate=flat_maximum:20"})
                .elenaiWeightSource(true);
        ByteBuf buf = Unpooled.buffer();
        out.toBytes(buf);
        ItemValuesMessage in = new ItemValuesMessage();
        in.fromBytes(buf);
        assertEquals(20.0, in.get(Number.DEFAULT_MELEE_COST));
        assertEquals(1.4, in.get(Number.ELENAI_WEIGHT_CONVERSION));
        assertArrayEquals(out.get(Lines.MELEE_COST_OVERRIDES), in.get(Lines.MELEE_COST_OVERRIDES));
        assertArrayEquals(out.get(Lines.ITEM_STAMINA_MODIFIERS), in.get(Lines.ITEM_STAMINA_MODIFIERS));
        assertEquals(0, in.get(Lines.ARMOR_WEIGHT).length);
        assertTrue(in.elenaiWeightSource());
    }

    @Test
    void tablesUseTheServerValues() {
        ItemValueTables t = new ItemValueTables(new ItemValuesMessage()
                .set(Number.DEFAULT_MELEE_COST, 20)
                .set(Lines.MELEE_COST_OVERRIDES, new String[] {"minecraft:iron_sword=15"})
                .set(Number.DEFAULT_SHIELD_STABILITY, 0)
                .set(Lines.SHIELD_STABILITY, new String[] {"minecraft:shield=50"}));
        assertEquals(15.0, t.meleeCost("minecraft:iron_sword", -1));
        assertEquals(20.0, t.meleeCost("minecraft:stick", 0));
        assertTrue(t.hasMeleeOverride("minecraft:iron_sword", -1));
        assertFalse(t.hasMeleeOverride("minecraft:stick", 0));
        assertEquals(50.0, t.stability("minecraft:shield", -1));
        assertTrue(Double.isNaN(t.armorWeight("minecraft:iron_chestplate", -1)));
    }

    @Test
    void elenaiOverridesAddUpPerItem() {
        Map<String, Double> o = ItemValueTables.parseElenaiOverrides(new String[] {
                "minecraft:iron_chestplate=10", "iron_chestplate=2", "bad line", "minecraft:gold_ingot=x"});
        assertEquals(12.0, o.get("minecraft:iron_chestplate"));
        assertEquals(1, o.size());
    }
}
