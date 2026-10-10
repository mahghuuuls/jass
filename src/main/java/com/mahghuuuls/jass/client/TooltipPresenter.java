package com.mahghuuuls.jass.client;

import com.mahghuuuls.jass.JustAnotherStaminaSystemMod;
import com.mahghuuuls.jass.api.client.ItemStaminaInfo;
import com.mahghuuuls.jass.api.client.StaminaTooltipEvent;
import com.mahghuuuls.jass.client.JassClientConfig.ItemTooltips;
import com.mahghuuuls.jass.config.ConfigModel;
import com.mahghuuuls.jass.network.ItemValuesMessage;
import com.mahghuuuls.jass.network.JassNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Adds the Stamina section to item tooltips (REQ-058) from the server's values, which arrive in
 * {@link ItemValuesMessage} at login and after a server configuration reload. Before they arrive,
 * or on a server without JASS, nothing is added. Addons may edit or replace the lines through
 * {@link StaminaTooltipEvent}; it is not posted while {@code item_tooltips} is off, and in shift
 * mode without Shift held its lines are the hint only.
 */
public final class TooltipPresenter implements JassNetwork.ClientItemValuesSink {

    /** Client thread only. */
    private volatile ItemValueTables tables;

    /** The item info the client API hands out ({@code ClientStaminaApi.getItemInfo}). */
    public ItemStaminaInfo info(ItemStack stack) {
        return stack == null ? ItemStaminaInfo.NONE : ItemInfoResolver.resolve(stack, Minecraft.getMinecraft().player, tables);
    }

    @Override
    public void onItemValues(ItemValuesMessage message) {
        Minecraft.getMinecraft().addScheduledTask(() -> {
            tables = new ItemValueTables(message);
            ElenaiWeightTooltip.setElenaiWeightSource(message.elenaiWeightSource());
            if (ConfigModel.server().debugLogging()) {
                // Single player shares the server's settings; on a remote server this follows the local file.
                JustAnotherStaminaSystemMod.LOGGER.info("JASS item values received from the server (elenaiWeight={})",
                        message.elenaiWeightSource());
            }
        });
    }

    @SubscribeEvent
    public void onDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        Minecraft.getMinecraft().addScheduledTask(() -> {
            tables = null;
            ElenaiWeightTooltip.setElenaiWeightSource(false);
        });
    }

    @SubscribeEvent
    public void onTooltip(ItemTooltipEvent event) {
        ItemTooltips mode = JassClientConfig.itemTooltips;
        if (mode == ItemTooltips.OFF || tables == null) {
            return;
        }
        EntityPlayer player = event.getEntityPlayer();
        ItemStaminaInfo info = ItemInfoResolver.resolve(event.getItemStack(), player, tables);
        if (info.isEmpty()) {
            return;
        }
        List<String> lines = mode == ItemTooltips.SHIFT && !GuiScreen.isShiftKeyDown()
                ? new ArrayList<>(Collections.singletonList(TextFormatting.GRAY + I18n.format("tooltip.jass.hold_shift")))
                : TooltipLines.lines(info, I18n::format);
        StaminaTooltipEvent hook = new StaminaTooltipEvent(event.getItemStack(), info, lines);
        if (!MinecraftForge.EVENT_BUS.post(hook)) {
            event.getToolTip().addAll(hook.getLines());
        }
    }
}
