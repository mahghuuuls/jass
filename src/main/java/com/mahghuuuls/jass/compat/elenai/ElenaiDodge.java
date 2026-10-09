package com.mahghuuuls.jass.compat.elenai;

import com.elenai.elenaidodge2.api.CheckFeatherEvent;
import com.elenai.elenaidodge2.api.DodgeEvent;
import com.elenai.elenaidodge2.api.FeathersHelper;
import com.elenai.elenaidodge2.api.SpendFeatherEvent;
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
 * Elenai dodges cost Stamina instead of feathers (REQ-070). All three Elenai events are posted on
 * the server inside one dodge request: {@code ServerDodgeEvent} (Elenai's own gates run at normal
 * priority; JASS decides at LOWEST, after them), {@code CheckFeatherEvent} (cancelled so feathers
 * never block a dodge), and {@code SpendFeatherEvent} (cancelled for a dodge JASS allowed, which is
 * then paid once). Never touches the client-only {@code RequestDodgeEvent}. With the integration
 * off, nothing is changed and Elenai uses feathers.
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

    private boolean active() {
        return !failed && enabled.getAsBoolean();
    }

    /** An incompatible Elenai turns the integration off with one warning instead of failing every dodge (ERR-1). */
    private void disable(Throwable e) {
        if (!failed) {
            failed = true;
            logger.warn("Elenai Dodge 2 dodges could not be handled ({}); dodges use feathers again", e.toString());
        }
    }

    @SubscribeEvent
    public void onCheckFeathers(CheckFeatherEvent event) {
        try {
            if (active() && !event.getPlayer().world.isRemote) {
                event.setCanceled(true);
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
