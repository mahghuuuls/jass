package com.mahghuuuls.jass.latemixin.elenai;

import com.elenai.elenaidodge2.event.TooltipEventListener;
import com.mahghuuuls.jass.client.ElenaiWeightTooltip;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Skips Elenai Dodge 2's armor weight tooltip (its steel feather icons) while JASS takes Effective
 * Weight from Elenai and shows the same weight in its own tooltip line (REQ-073, REV-012). Both of
 * Elenai's methods are skipped: the one that reserves the blank line and the one that draws the
 * icons. {@code require = 0}: a different Elenai version simply keeps its tooltip.
 */
@Mixin(value = TooltipEventListener.class, remap = false)
public abstract class MixinTooltipEventListener {

    @Inject(method = "makeTooltip", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void jass$skipWeightLine(ItemTooltipEvent event, CallbackInfo ci) {
        if (ElenaiWeightTooltip.hide()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderTooltip", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void jass$skipWeightIcons(RenderTooltipEvent.PostText event, CallbackInfo ci) {
        if (ElenaiWeightTooltip.hide()) {
            ci.cancel();
        }
    }
}
