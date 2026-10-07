package com.mahghuuuls.jass.gameplay;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * Wires the server-side Stamina components together and connects them to the player lifecycle
 * and server tick. One instance exists per game session.
 */
public final class JassGameplay {

    private static final double SECONDS_PER_TICK = 1.0 / 20.0;
    private static JassGameplay instance;

    private final ProfileService profiles;
    private final StaminaSessionManager sessions;
    private final ActionGate gate;
    private final SyncService sync;

    private JassGameplay() {
        this.profiles = new ProfileService();
        this.sessions = new StaminaSessionManager(profiles);
        this.gate = new ActionGate(sessions, profiles);
        this.sync = new SyncService(sessions, gate);
    }

    /** Creates the gameplay layer and registers its lifecycle and tick listeners. */
    public static JassGameplay create() {
        instance = new JassGameplay();
        MinecraftForge.EVENT_BUS.register(instance);
        return instance;
    }

    public static JassGameplay get() {
        return instance;
    }

    public ActionGate gate() {
        return gate;
    }

    /** Drops every session, for server shutdown. */
    public void clear() {
        sessions.clear();
    }

    @SubscribeEvent
    public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        sessions.onLogin(event.player);
    }

    @SubscribeEvent
    public void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        // Leaving the End through its exit portal is reported as a respawn; it is a dimension
        // change, so Stamina is kept.
        if (event.isEndConquered()) {
            sessions.onDimensionChanged(event.player);
        } else {
            sessions.onRespawn(event.player);
        }
    }

    @SubscribeEvent
    public void onEquipmentChange(LivingEquipmentChangeEvent event) {
        if (event.getEntityLiving() instanceof EntityPlayer && !event.getEntityLiving().world.isRemote) {
            profiles.invalidate((EntityPlayer) event.getEntityLiving());
        }
    }

    @SubscribeEvent
    public void onDimensionChanged(PlayerEvent.PlayerChangedDimensionEvent event) {
        sessions.onDimensionChanged(event.player);
    }

    @SubscribeEvent
    public void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        sessions.onLogout(event.player);
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.world.isRemote
                || !(event.player instanceof EntityPlayerMP)) {
            return;
        }
        EntityPlayerMP player = (EntityPlayerMP) event.player;
        gate.tick(player, SECONDS_PER_TICK);
        sync.maybeSend(player, player.world.getTotalWorldTime());
    }
}
