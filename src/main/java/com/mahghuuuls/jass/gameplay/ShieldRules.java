package com.mahghuuuls.jass.gameplay;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.EnumAction;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSource;

/**
 * Shield block classification (REQ-035, REQ-036): which blocked hits are Stamina actions. Only a
 * direct attack by a living entity and a projectile (arrows, fireballs, shulker bullets, llama
 * spit) count; everything else a shield can stop (explosions, magic, burning, thorns) keeps
 * vanilla's full block and costs nothing. A wither skull reports the wither itself as the direct
 * attacker, so it counts as melee.
 */
public final class ShieldRules {

    public enum BlockKind {
        MELEE,
        PROJECTILE,
        OTHER
    }

    private ShieldRules() {
    }

    /** Damage a block lets through: the part the blocking item does not stop. */
    public static double leakedDamage(double incomingDamage, double blockFraction) {
        double stopped = Math.min(1.0, Math.max(0.0, blockFraction));
        return Math.max(0.0, incomingDamage) * (1.0 - stopped);
    }

    /**
     * True for any item raised to block: the vanilla block use action or Forge's shield flag. The
     * client guard uses the same test.
     */
    public static boolean isBlockingItem(ItemStack stack, EntityLivingBase holder) {
        return !stack.isEmpty()
                && (stack.getItemUseAction() == EnumAction.BLOCK || stack.getItem().isShield(stack, holder));
    }

    public static BlockKind classify(DamageSource source) {
        if (source.isProjectile()) {
            return BlockKind.PROJECTILE;
        }
        if (!(source instanceof EntityDamageSource) || source.isExplosion() || source.isMagicDamage()
                || source.isFireDamage() || ((EntityDamageSource) source).getIsThornsDamage()) {
            return BlockKind.OTHER;
        }
        Entity immediate = source.getImmediateSource();
        return immediate instanceof EntityLivingBase && immediate == source.getTrueSource()
                ? BlockKind.MELEE
                : BlockKind.OTHER;
    }
}
