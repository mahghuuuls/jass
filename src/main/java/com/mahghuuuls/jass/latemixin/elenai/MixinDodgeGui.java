package com.mahghuuuls.jass.latemixin.elenai;

import com.elenai.elenaidodge2.gui.DodgeGui;
import com.mahghuuuls.jass.client.ElenaiFeatherHud;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Skips Elenai Dodge 2's feather bar while JASS says feathers are replaced (REQ-072, REV-008). Its
 * own configuration is queued only when Elenai is present; {@code require = 0} so a different Elenai
 * version simply keeps its bar instead of failing. Elenai's names are not obfuscated, so no remap.
 */
@Mixin(value = DodgeGui.class, remap = false)
public abstract class MixinDodgeGui {

    @Inject(method = "onRenderDodgeGUIEvent", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void jass$hideFeathers(RenderGameOverlayEvent.Post event, CallbackInfo ci) {
        if (ElenaiFeatherHud.hide()) {
            ci.cancel();
        }
    }
}
