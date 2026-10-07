package com.mahghuuuls.jass.mixin.client;

import com.mahghuuuls.jass.client.ClientActionGuard;
import net.minecraft.client.entity.EntityPlayerSP;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Every way the local player starts sprinting (key held, double tap, other mods) ends in
 * {@code setSprinting(true)}; refusing it here is the one place that stops all of them.
 */
@Mixin(EntityPlayerSP.class)
public abstract class MixinEntityPlayerSP {

    @Inject(method = "setSprinting", at = @At("HEAD"), cancellable = true)
    private void jass$refuseSprintStart(boolean sprinting, CallbackInfo ci) {
        if (sprinting && ClientActionGuard.refuseSprintStart((EntityPlayerSP) (Object) this)) {
            ci.cancel();
        }
    }
}
