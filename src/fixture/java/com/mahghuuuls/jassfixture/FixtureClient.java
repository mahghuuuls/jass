package com.mahghuuuls.jassfixture;

import com.mahghuuuls.jass.api.client.ClientStaminaApi;
import com.mahghuuuls.jass.api.client.ClientStaminaSnapshot;
import com.mahghuuuls.jass.api.client.ItemStaminaInfo;
import com.mahghuuuls.jass.api.client.StaminaHudRenderEvent;
import com.mahghuuuls.jass.api.client.StaminaTooltipEvent;
import net.minecraft.init.Items;
import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.client.GuiIngameForge;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Client half of the fixture: {@code /jassfixturehud on|off} replaces the JASS HUD with a text line, and
 * {@code /jassfixture leftmarker on|off} draws a marker where the next left-column HUD row would start.
 */
final class FixtureClient {

    private static volatile boolean replaceHud;

    private FixtureClient() {
    }

    static void init() {
        MinecraftForge.EVENT_BUS.register(new FixtureClient());
        ClientCommandHandler.instance.registerCommand(new CommandBase() {
            @Override
            public String getName() {
                return "jassfixturehud";
            }

            @Override
            public String getUsage(ICommandSender sender) {
                return "/jassfixturehud <on|off>";
            }

            @Override
            public int getRequiredPermissionLevel() {
                return 0;
            }

            @Override
            public void execute(MinecraftServer server, ICommandSender sender, String[] args) {
                replaceHud = args.length > 0 && "on".equalsIgnoreCase(args[0]);
                ClientStaminaSnapshot s = ClientStaminaApi.getSnapshot();
                String text = "JASSFIXTURE hud replace=" + replaceHud + " snapshot visible="
                        + JassFixtureMod.format(s.getVisibleStamina()) + " maximum=" + JassFixtureMod.format(s.getMaximumStamina());
                JassFixtureMod.LOGGER.info(text);
                sender.sendMessage(new TextComponentString(text));
            }
        });
    }

    @SubscribeEvent
    public void onHud(StaminaHudRenderEvent event) {
        if (!replaceHud) {
            return;
        }
        event.setCanceled(true);
        ClientStaminaSnapshot s = event.getSnapshot();
        String text = "FIXTURE HUD " + Math.round(s.getVisibleStamina()) + "/" + Math.round(s.getMaximumStamina());
        Minecraft mc = Minecraft.getMinecraft();
        mc.fontRenderer.drawStringWithShadow(text, event.getScaledWidth() / 2.0F + 10, event.getScaledHeight() - 60,
                0x55FFFF);
    }

    private static int lastMarkerTop = Integer.MIN_VALUE;
    private static int lastMarkerToggles = -1;

    /**
     * Stands in for a mod that draws the next row of the left bar column: a magenta 81 x 9 box where
     * that row would start, drawn after the JASS bar (which draws before the air row).
     */
    @SubscribeEvent
    public void onOverlayPost(RenderGameOverlayEvent.Post event) {
        if (!JassFixtureMod.leftMarker || event.getType() != RenderGameOverlayEvent.ElementType.AIR) {
            return;
        }
        int width = event.getResolution().getScaledWidth();
        int height = event.getResolution().getScaledHeight();
        int left = width / 2 - 91;
        int top = height - GuiIngameForge.left_height;
        Gui.drawRect(left, top, left + 81, top + 9, 0xFFFF00FF);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        int toggles = JassFixtureMod.leftMarkerToggles;
        if (top != lastMarkerTop || toggles != lastMarkerToggles) {
            lastMarkerTop = top;
            lastMarkerToggles = toggles;
            JassFixtureMod.LOGGER.info("JASSFIXTURE leftmarker leftHeight={} left={} top={}", GuiIngameForge.left_height,
                    left, top);
        }
    }

    private static double lastLoggedSwing = Double.NaN;

    /**
     * Add-on path of the tooltip API (REV-010 addendum): on golden swords, reads the item info and
     * adds one marker line through {@code StaminaTooltipEvent}; logs when the value changes.
     */
    @SubscribeEvent
    public void onStaminaTooltip(StaminaTooltipEvent event) {
        if (event.getStack().getItem() != Items.GOLDEN_SWORD) {
            return;
        }
        ItemStaminaInfo apiInfo = ClientStaminaApi.getItemInfo(event.getStack());
        double swing = apiInfo.getSwingCost();
        event.getLines().add("JASSFIXTURE tooltip swing=" + JassFixtureMod.format(swing)
                + " event=" + JassFixtureMod.format(event.getInfo().getSwingCost()));
        if (Double.compare(swing, lastLoggedSwing) != 0) {
            lastLoggedSwing = swing;
            JassFixtureMod.LOGGER.info("JASSFIXTURE tooltip golden_sword swing={} lines={}",
                    JassFixtureMod.format(swing), event.getLines().size());
        }
    }
}
