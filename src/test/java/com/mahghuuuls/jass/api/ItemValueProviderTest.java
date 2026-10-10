package com.mahghuuuls.jass.api;

import net.minecraft.util.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemValueProviderTest {

    private static RegisteredProvider<IItemValueProvider> provider(String id, IItemValueProvider p) {
        return new RegisteredProvider<>(new ResourceLocation("test", id), p);
    }

    @Test
    void firstUsableAnswerWinsAndBadOnesAreSkipped() {
        List<RegisteredProvider<IItemValueProvider>> providers = Arrays.asList(
                provider("silent", new IItemValueProvider() { }),
                provider("throws", new IItemValueProvider() {
                    @Override
                    public double meleeCost(net.minecraft.item.ItemStack stack) {
                        throw new IllegalStateException("broken");
                    }
                }),
                provider("negative", new IItemValueProvider() {
                    @Override
                    public double meleeCost(net.minecraft.item.ItemStack stack) {
                        return -1.0;
                    }
                }),
                provider("good", new IItemValueProvider() {
                    @Override
                    public double meleeCost(net.minecraft.item.ItemStack stack) {
                        return 7.0;
                    }
                }),
                provider("later", new IItemValueProvider() {
                    @Override
                    public double meleeCost(net.minecraft.item.ItemStack stack) {
                        return 99.0;
                    }
                }));
        assertEquals(7.0, JassApi.firstUsable(providers, ItemValueKind.MELEE_COST, null));
        assertTrue(Double.isNaN(JassApi.firstUsable(providers, ItemValueKind.BOW_DRAW_COST, null)));
    }

    @Test
    void heldAndArmorWeightAreSeparateAnswers() {
        List<RegisteredProvider<IItemValueProvider>> providers = Arrays.asList(
                provider("held", new IItemValueProvider() {
                    @Override
                    public double heldWeight(net.minecraft.item.ItemStack stack) {
                        return 5.0;
                    }
                }));
        assertEquals(5.0, JassApi.firstUsable(providers, ItemValueKind.HELD_WEIGHT, null));
        assertTrue(Double.isNaN(JassApi.firstUsable(providers, ItemValueKind.ARMOR_WEIGHT, null)));
    }

    @Test
    void blockSharesMustBeFractions() {
        List<RegisteredProvider<IItemValueProvider>> providers = Arrays.asList(
                provider("too-much", new IItemValueProvider() {
                    @Override
                    public double shieldMeleeBlock(net.minecraft.item.ItemStack stack) {
                        return 1.5;
                    }
                }),
                provider("ok", new IItemValueProvider() {
                    @Override
                    public double shieldMeleeBlock(net.minecraft.item.ItemStack stack) {
                        return 0.5;
                    }

                    @Override
                    public double armorWeight(net.minecraft.item.ItemStack stack) {
                        return Double.POSITIVE_INFINITY;
                    }
                }));
        assertEquals(0.5, JassApi.firstUsable(providers, ItemValueKind.SHIELD_MELEE_BLOCK, null));
        assertTrue(Double.isNaN(JassApi.firstUsable(providers, ItemValueKind.ARMOR_WEIGHT, null)));
    }

    @Test
    void aProviderWithALinkageFailureIsSwitchedOff() {
        int[] calls = {0};
        List<RegisteredProvider<IItemValueProvider>> providers = Arrays.asList(
                provider("missing-class", new IItemValueProvider() {
                    @Override
                    public double meleeCost(net.minecraft.item.ItemStack stack) {
                        calls[0]++;
                        throw new NoClassDefFoundError("some/optional/Mod");
                    }
                }),
                provider("fallback", new IItemValueProvider() {
                    @Override
                    public double meleeCost(net.minecraft.item.ItemStack stack) {
                        return 4.0;
                    }
                }));
        assertEquals(4.0, JassApi.firstUsable(providers, ItemValueKind.MELEE_COST, null));
        assertEquals(4.0, JassApi.firstUsable(providers, ItemValueKind.MELEE_COST, null));
        assertEquals(1, calls[0]);
    }
}
