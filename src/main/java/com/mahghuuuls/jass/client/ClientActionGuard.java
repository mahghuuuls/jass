package com.mahghuuuls.jass.client;

import com.mahghuuuls.jass.core.SprintRules;
import com.mahghuuuls.jass.network.AirSwingMessage;
import com.mahghuuuls.jass.network.JassNetwork;
import com.mahghuuuls.jass.network.JumpMessage;
import com.mahghuuuls.jass.network.RefusedAttackMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumAction;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.RayTraceResult;
import net.minecraftforge.client.event.InputUpdateEvent;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * Client-side prediction of Stamina rules from the last snapshot, so the local player does not
 * see an action the server is about to refuse, and reporting of facts only the client sees.
 * The server stays authoritative.
 */
public final class ClientActionGuard {

    /** Mouse-button key codes are stored as {@code button - 100}. */
    private static final int MOUSE_KEY_OFFSET = 100;

    /** A held refusal (sprint, jump) flashes the bar only after this many ticks without one, so holding the key flashes once. */
    private static final int HELD_REFUSAL_GAP_TICKS = 10;

    private static ClientActionGuard instance;

    private final FeedbackPlayer feedback;
    private long clientTicks;
    private long lastSprintRefusalTick = Long.MIN_VALUE / 2;
    private long lastJumpRefusalTick = Long.MIN_VALUE / 2;

    private ClientActionGuard(FeedbackPlayer feedback) {
        this.feedback = feedback;
    }

    /** Creates the guard the sprint Mixin talks to. Called once by the client proxy. */
    static ClientActionGuard install(FeedbackPlayer feedback) {
        instance = new ClientActionGuard(feedback);
        return instance;
    }

    /**
     * Called by the sprint Mixin for every attempt to start sprinting. Refuses while the last
     * snapshot says the player cannot spend and sprinting would cost Stamina here.
     */
    public static boolean refuseSprintStart(EntityPlayerSP player) {
        if (instance == null || ClientStaminaState.canSpend() || !wouldCostStamina(player, true)) {
            return false;
        }
        instance.onSprintRefused();
        return true;
    }

    private void onSprintRefused() {
        if (clientTicks - lastSprintRefusalTick > HELD_REFUSAL_GAP_TICKS) {
            feedback.onDenied();
        }
        lastSprintRefusalTick = clientTicks;
    }

    /**
     * With jump cost enabled and no Stamina to spend, a jump from the ground is refused before
     * vanilla reads the input. Swimming, lava, ladders, riding, and flight are not ground jumps and
     * are never touched.
     */
    @SubscribeEvent
    public void onInputUpdate(InputUpdateEvent event) {
        if (event.getMovementInput().jump && refusesGroundJump(event.getEntityPlayer())) {
            event.getMovementInput().jump = false;
            onJumpRefused();
        }
    }

    /**
     * Reports a jump the local player performed; the server validates it and charges it when jump
     * cost is on. A jump that starts anyway while refused (vanilla auto-jump sets the input after
     * the input event) loses its upward push and is not reported.
     */
    @SubscribeEvent
    public void onJump(LivingEvent.LivingJumpEvent event) {
        EntityPlayerSP player = Minecraft.getMinecraft().player;
        if (event.getEntityLiving() != player || !ClientStaminaState.jumpCostEnabled()) {
            return;
        }
        if (refusesGroundJump(player)) {
            player.motionY = 0.0;
            onJumpRefused();
            return;
        }
        JassNetwork.channel().sendToServer(new JumpMessage());
    }

    private static boolean refusesGroundJump(EntityPlayer player) {
        return ClientStaminaState.jumpCostEnabled() && !ClientStaminaState.canSpend() && player.onGround
                && !player.isInWater() && !player.isInLava() && !player.isOnLadder() && !player.isRiding()
                && !player.capabilities.isFlying;
    }

    private void onJumpRefused() {
        if (clientTicks - lastJumpRefusalTick > HELD_REFUSAL_GAP_TICKS) {
            feedback.onDenied();
        }
        lastJumpRefusalTick = clientTicks;
    }

    private static boolean wouldCostStamina(EntityPlayerSP player, boolean sprinting) {
        return SprintRules.costsStamina(sprinting, player.isInWater(), player.isRiding(), player.isElytraFlying(),
                player.capabilities.isFlying);
    }

    /** Stops a sprint already running when Stamina runs out, within one client tick of the snapshot. */
    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) {
            return;
        }
        clientTicks++;
        EntityPlayerSP player = Minecraft.getMinecraft().player;
        if (player != null && !ClientStaminaState.canSpend() && wouldCostStamina(player, player.isSprinting())) {
            player.setSprinting(false);
        }
    }

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

    /**
     * At zero Stamina a bow-draw item (use action bow drawing, as on the server) does not start a
     * draw locally. Vanilla has already sent the use packet; the server refuses it and records the
     * denial (once per held right click), which drives the flash. Without this the client would
     * show a draw the server never started. The cancelled event answers PASS, so vanilla then tries
     * the other hand, as it does for a bow without arrows: a shield there is raised.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        ItemStack stack = event.getItemStack();
        if (!event.getWorld().isRemote || stack.isEmpty()) {
            return;
        }
        boolean refusedDraw = !ClientStaminaState.canSpend() && stack.getItemUseAction() == EnumAction.BOW;
        // During Guard Break no blocking item may be raised; the server refuses it as well.
        boolean refusedShield = ClientStaminaState.guardBroken()
                && (stack.getItemUseAction() == EnumAction.BLOCK || stack.getItem().isShield(stack, event.getEntityPlayer()));
        if (refusedDraw || refusedShield) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onLeftClickEmpty(PlayerInteractEvent.LeftClickEmpty event) {
        // The server cannot see misses; it validates the report and decides the cost.
        JassNetwork.channel().sendToServer(new AirSwingMessage());
    }
}
