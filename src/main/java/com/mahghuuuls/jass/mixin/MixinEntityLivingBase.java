package com.mahghuuuls.jass.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mahghuuuls.jass.gameplay.hooks.ShieldHook;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Lets JASS see, and veto, vanilla's own shield decision inside {@code attackEntityFrom} (ARC-005).
 * The wrapper keeps vanilla's private block test and chains with other mods' wrappers of the same
 * call. {@code amount} is the damage at that point: difficulty-scaled, before armor.
 */
@Mixin(EntityLivingBase.class)
public abstract class MixinEntityLivingBase {

    @WrapOperation(method = "attackEntityFrom", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/entity/EntityLivingBase;canBlockDamageSource(Lnet/minecraft/util/DamageSource;)Z"))
    private boolean jass$decideShieldBlock(EntityLivingBase self, DamageSource source, Operation<Boolean> original,
            @Local(argsOnly = true) float amount) {
        return original.call(self, source) && ShieldHook.allowBlock(self, source, amount);
    }
}
