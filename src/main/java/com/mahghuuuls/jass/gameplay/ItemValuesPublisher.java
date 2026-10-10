package com.mahghuuuls.jass.gameplay;

import com.mahghuuuls.jass.config.ConfigModel;
import com.mahghuuuls.jass.config.ServerSettings;
import com.mahghuuuls.jass.network.ItemValuesMessage;
import com.mahghuuuls.jass.network.ItemValuesMessage.Lines;
import com.mahghuuuls.jass.network.ItemValuesMessage.Number;
import com.mahghuuuls.jass.network.JassNetwork;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraft.server.MinecraftServer;

import java.util.Arrays;

/**
 * Sends the server's per-item values to clients for tooltips (REQ-058, REV-010): at login, and to
 * everyone when the configuration revision or the weight source's preview changes (checked once a
 * second on the server thread).
 */
final class ItemValuesPublisher {

    private static final int CHECK_INTERVAL_TICKS = 20;

    private volatile WeightPreviewSource weightPreview;
    private int ticks;
    private int sentRevision = Integer.MIN_VALUE;
    private boolean sentPreviewActive;
    private String[] sentPreviewLines = new String[0];

    void useWeightPreview(WeightPreviewSource source) {
        this.weightPreview = source;
    }

    @SubscribeEvent
    public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.player instanceof EntityPlayerMP) {
            JassNetwork.channel().sendTo(build(), (EntityPlayerMP) event.player);
        }
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || ++ticks % CHECK_INTERVAL_TICKS != 0) {
            return;
        }
        int revision = ConfigModel.server().revision();
        boolean previewActive = previewActive();
        String[] previewLines = previewLines(previewActive);
        if (revision == sentRevision && previewActive == sentPreviewActive && Arrays.equals(previewLines, sentPreviewLines)) {
            return;
        }
        sentRevision = revision;
        sentPreviewActive = previewActive;
        sentPreviewLines = previewLines;
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null) {
            return;
        }
        // Also on the first check: a change between a login and this check would otherwise be lost.
        ItemValuesMessage message = build();
        for (EntityPlayerMP player : server.getPlayerList().getPlayers()) {
            JassNetwork.channel().sendTo(message, player);
        }
    }

    /** The message for the current configuration. */
    ItemValuesMessage build() {
        ServerSettings s = ConfigModel.server();
        boolean previewActive = previewActive();
        return new ItemValuesMessage()
                .set(Number.DEFAULT_MELEE_COST, s.defaultMeleeCost())
                .set(Number.DEFAULT_BOW_DRAW_COST, s.defaultBowDrawCost())
                .set(Number.BLOCK_STAMINA_PER_DAMAGE, s.blockStaminaPerDamage())
                .set(Number.STABILITY_SCALE, s.stabilityScale())
                .set(Number.DEFAULT_SHIELD_STABILITY, s.defaultShieldStability())
                .set(Number.DEFAULT_SHIELD_MELEE_BLOCK, s.defaultShieldMeleeBlock())
                .set(Number.DEFAULT_SHIELD_PROJECTILE_BLOCK, s.defaultShieldProjectileBlock())
                .set(Number.FALLBACK_WEIGHT_PER_ARMOR_POINT, s.fallbackWeightPerArmorPoint())
                .set(Number.ELENAI_WEIGHT_CONVERSION, s.elenaiWeightConversion())
                .set(Lines.MELEE_COST_OVERRIDES, s.meleeCostLines())
                .set(Lines.BOW_DRAW_COST_OVERRIDES, s.bowDrawCostLines())
                .set(Lines.SHIELD_STABILITY, s.shieldStabilityLines())
                .set(Lines.SHIELD_MELEE_BLOCK, s.shieldMeleeBlockLines())
                .set(Lines.SHIELD_PROJECTILE_BLOCK, s.shieldProjectileBlockLines())
                .set(Lines.ARMOR_WEIGHT, s.armorWeightLines())
                .set(Lines.ITEM_STAMINA_MODIFIERS, s.itemModifierLines())
                .set(Lines.ELENAI_WEIGHT_OVERRIDES, previewLines(previewActive))
                .elenaiWeightSource(previewActive);
    }

    private boolean previewActive() {
        WeightPreviewSource source = weightPreview;
        return source != null && source.previewActive();
    }

    private String[] previewLines(boolean active) {
        WeightPreviewSource source = weightPreview;
        if (!active || source == null) {
            return new String[0];
        }
        String[] lines = source.previewOverrideLines();
        return lines == null ? new String[0] : lines;
    }
}
