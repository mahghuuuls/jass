package com.mahghuuuls.jass.api;

import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JassBowDrawTest {

    private static final String OWNED_NAME = "jass-test-owned-bow";

    @BeforeAll
    static void bootstrap() {
        Bootstrap.register();
    }

    private static List<RegisteredProvider<IItemValueProvider>> ownerOfTheNamedBow() {
        return Collections.singletonList(new RegisteredProvider<>(new ResourceLocation("jasstest", "bow_owner"),
                new IItemValueProvider() {
                    @Override
                    public boolean ownsBowUse(ItemStack stack) {
                        return stack.hasDisplayName() && OWNED_NAME.equals(stack.getDisplayName());
                    }
                }));
    }

    @Test
    void aVanillaBowIsABowDraw() {
        ItemStack bow = new ItemStack(Items.BOW);
        assertTrue(JassApi.hasBowUseAction(bow));
        assertFalse(JassApi.anyOwnsBowUse(ownerOfTheNamedBow(), bow));
    }

    @Test
    void anItemWithoutTheBowUseActionIsNot() {
        assertFalse(JassApi.hasBowUseAction(new ItemStack(Items.IRON_SWORD)));
    }

    @Test
    void anOwnedBowStillHasTheBowUseActionButIsClaimed() {
        ItemStack owned = new ItemStack(Items.BOW);
        owned.setStackDisplayName(OWNED_NAME);
        assertTrue(JassApi.hasBowUseAction(owned));
        assertTrue(JassApi.anyOwnsBowUse(ownerOfTheNamedBow(), owned));
    }
}
