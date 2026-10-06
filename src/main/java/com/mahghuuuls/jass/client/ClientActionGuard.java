package com.mahghuuuls.jass.client;

import com.mahghuuuls.jass.network.AirSwingMessage;
import com.mahghuuuls.jass.network.JassNetwork;
import com.mahghuuuls.jass.network.RefusedAttackMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.RayTraceResult;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Client-side prediction of Stamina rules from the last snapshot, so the local player does not
 * see an action the server is about to refuse, and reporting of facts only the client sees.
 * The server stays authoritative.
 */
public final class ClientActionGuard {

    /** Mouse-button key codes are stored as {@code button - 100}. */
    private static final int MOUSE_KEY_OFFSET = 100;

    /**
     * At zero Stamina an attack click on an entity or on air does nothing at all (no swing), so
     * it is consumed before vanilla or another combat mod processes it. Clicks on blocks pass,
     * so mining is never blocked. Works when attack is bound to a mouse button; with a keyboard
     * binding the swing still plays and the server refuses the hit. Cancelling at HIGHEST also
     * hides the press from other listeners of that button, and a press consumed over air does not
     * start mining if the player then sweeps onto a block (a new click does).
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onMouse(MouseEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (!event.isButtonstate() || mc.player == null || mc.currentScreen != null || !mc.inGameHasFocus
                || ClientStaminaState.canSpend()) {
            return;
        }
        int attackKey = mc.gameSettings.keyBindAttack.getKeyCode();
        if (attackKey >= 0 || event.getButton() != attackKey + MOUSE_KEY_OFFSET) {
            return;
        }
        RayTraceResult target = mc.objectMouseOver;
        if (target != null && target.typeOfHit == RayTraceResult.Type.BLOCK) {
            return;
        }
        event.setCanceled(true);
        JassNetwork.channel().sendToServer(new RefusedAttackMessage());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onAttack(AttackEntityEvent event) {
        if (event.getEntityPlayer().world.isRemote && !ClientStaminaState.canSpend()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onLeftClickEmpty(PlayerInteractEvent.LeftClickEmpty event) {
        // The server cannot see misses; it validates the report and decides the cost.
        JassNetwork.channel().sendToServer(new AirSwingMessage());
    }
}
