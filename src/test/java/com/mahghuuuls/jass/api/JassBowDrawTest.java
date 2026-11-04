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
        assertTrue(JassApi.isJassBowDraw(new ItemStack(Items.BOW), ownerOfTheNamedBow()));
    }

    @Test
    void anItemWithoutTheBowUseActionIsNot() {
        assertFalse(JassApi.isJassBowDraw(new ItemStack(Items.IRON_SWORD), ownerOfTheNamedBow()));
    }

    @Test
    void anOwnedBowIsNotABowDrawForJass() {
        ItemStack owned = new ItemStack(Items.BOW);
        owned.setStackDisplayName(OWNED_NAME);
        assertTrue(JassApi.hasBowUseAction(owned));
        assertFalse(JassApi.isJassBowDraw(owned, ownerOfTheNamedBow()));
    }

    @Test
    void withNoItemValueProviderInstalledABowIsABowDrawAndNotOwned() {
        // The test runtime registers no provider in JassApi, as in a pack without such an addon. No
        // test may register one: JassApi registration is global to the test JVM and cannot be undone.
        ItemStack bow = new ItemStack(Items.BOW);
        assertFalse(JassApi.isBowUseOwnedByAddon(bow));
        assertTrue(JassApi.isJassBowDraw(bow));
    }
}
