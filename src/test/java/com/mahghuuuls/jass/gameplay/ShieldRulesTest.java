package com.mahghuuuls.jass.gameplay;

import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Classification paths that need no live entity; the melee path is checked in campaign C. */
class ShieldRulesTest {

    @Test
    void projectilesAreProjectileBlocks() {
        assertEquals(ShieldRules.BlockKind.PROJECTILE,
                ShieldRules.classify(new EntityDamageSource("arrow", null).setProjectile()));
    }

    @Test
    void explosionsAreOtherBlocks() {
        assertEquals(ShieldRules.BlockKind.OTHER,
                ShieldRules.classify(new EntityDamageSource("explosion.player", null).setExplosion()));
    }

    @Test
    void thornsAreOtherBlocks() {
        assertEquals(ShieldRules.BlockKind.OTHER,
                ShieldRules.classify(new EntityDamageSource("thorns", null).setIsThornsDamage().setMagicDamage()));
    }

    @Test
    void sourcesWithoutAnEntityAreOtherBlocks() {
        assertEquals(ShieldRules.BlockKind.OTHER, ShieldRules.classify(new DamageSource("generic")));
    }

    @Test
    void entitySourcesWithoutALivingAttackerAreOtherBlocks() {
        assertEquals(ShieldRules.BlockKind.OTHER, ShieldRules.classify(new EntityDamageSource("mob", null)));
    }
}
