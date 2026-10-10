package com.mahghuuuls.jass.gameplay;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WeightResolverChoiceTest {

    @Test
    void usablePreferredWeightWins() {
        WeightResolver.Resolved r = WeightResolver.choose(22.4, "elenai", () -> 23.0);
        assertEquals(22.4, r.weight, 1e-9);
        assertEquals("elenai", r.source);
    }

    @Test
    void notANumberFallsBackToStandalone() {
        WeightResolver.Resolved r = WeightResolver.choose(Double.NaN, "elenai", () -> 23.0);
        assertEquals(23.0, r.weight, 1e-9);
        assertEquals(WeightResolver.STANDALONE, r.source);
    }

    @Test
    void heldWeightAddsOnTopOfEitherSource() {
        WeightResolver.Resolved elenai = WeightResolver.choose(7.0, "elenai", () -> 6.0).withHeld(6.0);
        assertEquals(13.0, elenai.weight, 1e-9);
        assertEquals(6.0, elenai.held, 1e-9);
        assertEquals("elenai", elenai.source);
        WeightResolver.Resolved standalone = WeightResolver.choose(Double.NaN, "elenai", () -> 0.0).withHeld(4.0);
        assertEquals(4.0, standalone.weight, 1e-9);
        assertEquals(WeightResolver.STANDALONE, standalone.source);
    }

    @Test
    void bothHandsAddUpAndEmptyOrBadAnswersAddNothing() {
        net.minecraft.init.Bootstrap.register();
        net.minecraft.item.ItemStack sword = new net.minecraft.item.ItemStack(net.minecraft.init.Items.IRON_SWORD);
        net.minecraft.item.ItemStack shield = new net.minecraft.item.ItemStack(net.minecraft.init.Items.SHIELD, 3);
        java.util.function.ToDoubleFunction<net.minecraft.item.ItemStack> lookup =
                s -> s.getItem() == net.minecraft.init.Items.SHIELD ? 4.0 : 2.0;
        assertEquals(6.0, WeightResolver.heldWeight(sword, shield, lookup), 1e-9);
        assertEquals(4.0, WeightResolver.heldWeight(net.minecraft.item.ItemStack.EMPTY, shield, lookup), 1e-9);
        assertEquals(0.0, WeightResolver.heldWeight(sword, net.minecraft.item.ItemStack.EMPTY, s -> Double.NaN), 1e-9);
        assertEquals(0.0, WeightResolver.heldWeight(sword, net.minecraft.item.ItemStack.EMPTY, s -> -3.0), 1e-9);
    }

    @Test
    void negativeWeightFallsBackToStandalone() {
        WeightResolver.Resolved r = WeightResolver.choose(-1.0, "elenai", () -> 17.0);
        assertEquals(17.0, r.weight, 1e-9);
        assertEquals(WeightResolver.STANDALONE, r.source);
    }
}
