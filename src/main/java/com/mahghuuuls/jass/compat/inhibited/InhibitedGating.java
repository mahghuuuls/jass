package com.mahghuuuls.jass.compat.inhibited;

import com.mahghuuuls.jass.gameplay.GatingSource;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.potion.Potion;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.function.BooleanSupplier;

/**
 * Inhibited gating (REQ-045): costs apply only while the player has Inhibited's effect. Inhibited is
 * found by its potion registry id, so no Inhibited class is ever referenced, and its own
 * {@code InhibitedLogic.isInhibited} (which has a side effect) is never called.
 */
public final class InhibitedGating implements GatingSource {

    /** Potion registry id read from Inhibited 1.2.0's registration code (DEPREF-001). */
    static final ResourceLocation EFFECT_ID = new ResourceLocation("inhibited", "inhibited");

    private final Potion effect;
    private final BooleanSupplier enabled;

    private InhibitedGating(Potion effect, BooleanSupplier enabled) {
        this.effect = effect;
        this.enabled = enabled;
    }

    /** The gating source, or {@code null} when Inhibited's effect is not registered (Inhibited absent). */
    public static InhibitedGating resolve(BooleanSupplier enabled) {
        Potion effect = ForgeRegistries.POTIONS.getValue(EFFECT_ID);
        return effect == null ? null : new InhibitedGating(effect, enabled);
    }

    @Override
    public boolean suspended(EntityPlayer player) {
        return enabled.getAsBoolean() && !player.isPotionActive(effect);
    }
}
