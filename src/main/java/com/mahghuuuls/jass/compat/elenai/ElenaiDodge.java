package com.mahghuuuls.jass.compat.elenai;

import com.elenai.elenaidodge2.api.DodgeEvent;
import com.elenai.elenaidodge2.api.FeathersHelper;
import com.elenai.elenaidodge2.api.SpendFeatherEvent;
import com.elenai.elenaidodge2.capability.dodges.DodgesProvider;
import com.elenai.elenaidodge2.capability.dodges.IDodges;
import com.elenai.elenaidodge2.network.PacketHandler;
import com.elenai.elenaidodge2.network.message.CUpdateDodgeMessage;
import com.mahghuuuls.jass.api.JassActions;
import com.mahghuuuls.jass.config.ConfigModel;
import com.mahghuuuls.jass.gameplay.ActionGate;
import com.mahghuuuls.jass.gameplay.CostKind;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.BooleanSupplier;
import java.util.function.DoubleSupplier;

/**
 * Elenai dodges cost Stamina instead of feathers (REQ-070). Both Elenai events JASS uses are posted
 * on the server inside one dodge request: {@code ServerDodgeEvent} (Elenai's own gates, including its
 * feather and weight checks, run at normal priority; JASS decides at LOWEST, after them) and
 * {@code SpendFeatherEvent} (cancelled for a dodge JASS allowed, which is then paid once). Feathers
 * are never spent, so Elenai's feather check always passes; its weight limit still applies, as one of
 * Elenai's own conditions.
 *
 * <p>Feathers are topped up to full at HIGHEST priority before Elenai's own checks: Elenai does not
 * copy the feather count when a player is recreated (death, leaving the End), so without the top-up
 * its feather and weight checks would refuse dodges for a while after every respawn.
 *
 * <p>JASS must not listen to {@code CheckFeatherEvent}: Elenai 1.1.0 posts the same event object
 * twice, and Forge throws on the second post of an event that has any listener, which breaks every
 * dodge. Never touches the client-only {@code RequestDodgeEvent}. With the integration off, nothing
 * is changed and Elenai uses feathers.
 */
public final class ElenaiDodge {

    private final ActionGate gate;
    private final BooleanSupplier enabled;
    private final DoubleSupplier baseCost;
    private final Logger logger;
    private volatile boolean failed;
    /** Server thread only: player -> world tick of a dodge JASS allowed and has not paid yet. */
    private final Map<EntityPlayer, Long> allowed = new WeakHashMap<>();

    public ElenaiDodge(ActionGate gate, BooleanSupplier enabled, DoubleSupplier baseCost, Logger logger) {
        this.gate = gate;
        this.enabled = enabled;
        this.baseCost = baseCost;
        this.logger = logger;
    }

    /** Elenai's full feather count (20 half feathers), as its own helpers cap it. */
    private static final int FULL_FEATHERS = 20;

    /** True while dodges cost Stamina: the option is on and the integration has not failed. */
    public boolean active() {
        return !failed && enabled.getAsBoolean();
    }

    /** An incompatible Elenai turns the integration off with one warning instead of failing every dodge (ERR-1). */
    private void disable(Throwable e) {
        if (!failed) {
            failed = true;
            logger.warn("Elenai Dodge 2 dodges could not be handled ({}); dodges use feathers again", e.toString());
        }
    }

    /**
     * Before Elenai's own checks: feathers are never spent while dodges cost Stamina, so keep them full
     * (Elenai resets them to 0 on respawn). The client is told, as Elenai's own helpers do.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onDodgeRequested(DodgeEvent.ServerDodgeEvent event) {
        try {
            EntityPlayer player = event.getPlayer();
            if (!active() || player.world.isRemote || !(player instanceof EntityPlayerMP)) {
                return;
            }
            IDodges feathers = player.getCapability(DodgesProvider.DODGES_CAP, null);
            if (feathers != null && feathers.getDodges() < FULL_FEATHERS) {
                feathers.set(FULL_FEATHERS);
                PacketHandler.instance.sendTo(new CUpdateDodgeMessage(FULL_FEATHERS), (EntityPlayerMP) player);
            }
        } catch (LinkageError | RuntimeException e) {
            disable(e);
        }
    }

    /** LOWEST and not for cancelled events: only dodges Elenai itself would perform reach JASS. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onServerDodge(DodgeEvent.ServerDodgeEvent event) {
        try {
            EntityPlayer player = event.getPlayer();
            if (!active() || player.world.isRemote) {
                return;
            }
            if (!gate.canStart(player)) {
                event.setCanceled(true);
                gate.reportRefusedAttempt(player, JassActions.DODGE);
                return;
            }
            allowed.put(player, player.world.getTotalWorldTime());
        } catch (LinkageError | RuntimeException e) {
            disable(e);
        }
    }

    @SubscribeEvent
    public void onSpendFeathers(SpendFeatherEvent event) {
        try {
            EntityPlayer player = event.getPlayer();
            if (!active() || player.world.isRemote) {
                return;
            }
            Long tick = allowed.remove(player);
            if (tick == null || tick != player.world.getTotalWorldTime()) {
                return;
            }
            event.setCanceled(true);
            gate.tryDiscrete(player, JassActions.DODGE, baseCost.getAsDouble(), CostKind.MOVEMENT);
            if (ConfigModel.server().debugLogging() && player instanceof EntityPlayerMP) {
                logger.info("JASS dodge player={} feathers={} (feathers kept; Stamina paid)", player.getName(),
                        FeathersHelper.getFeatherLevel((EntityPlayerMP) player));
            }
        } catch (LinkageError | RuntimeException e) {
            disable(e);
        }
    }
}
