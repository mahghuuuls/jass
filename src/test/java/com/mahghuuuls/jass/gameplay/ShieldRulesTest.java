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


    @Test
    void blockStabilityAddsTheBonusToTheItemStability() {
        // REQ-095: vanilla shield 50 plus a bonus of 50 gives 100, so a 9-damage hit costs 9 x 100/200 = 4.5.
        assertEquals(100.0, ShieldRules.blockStability(50.0, 50.0), 1e-9);
        assertEquals(4.5, com.mahghuuuls.jass.core.CostCalculator.blockBaseCost(9.0, 1.0,
                com.mahghuuuls.jass.core.CostCalculator.stabilityFactor(100.0, ShieldRules.blockStability(50.0, 50.0))), 1e-9);
    }

    @Test
    void aNegativeBonusCanTakeTheTotalBelowZero() {
        // REQ-095: 50 - 80 = -30; the Stability factor then counts it as 0 (factor 1).
        assertEquals(-30.0, ShieldRules.blockStability(50.0, -80.0), 1e-9);
        assertEquals(1.0, com.mahghuuuls.jass.core.CostCalculator.stabilityFactor(100.0, ShieldRules.blockStability(50.0, -80.0)), 1e-9);
    }

    @Test
    void noBonusKeepsTheItemStability() {
        assertEquals(50.0, ShieldRules.blockStability(50.0, 0.0), 1e-9);
    }
}
