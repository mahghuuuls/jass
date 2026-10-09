package com.mahghuuuls.jass.compat.rlcombat;

import com.mahghuuuls.jass.gameplay.AttackHandSource;
import net.minecraft.util.EnumHand;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Method;
import java.util.function.BooleanSupplier;

/**
 * Optional RLCombat support (REQ-025, ARC-008): RLCombat posts its attacks as
 * {@code bettercombat.mod.event.RLCombatAttackEntityEvent}, a subclass of {@link AttackEntityEvent}
 * with {@code getOffhand()}. Read reflectively, resolved once, so JASS has no compile dependency on
 * RLCombat. Any failure logs one warning and falls back to the main hand (ERR-1).
 */
public final class RLCombatHands implements AttackHandSource {

    static final String EVENT_CLASS = "bettercombat.mod.event.RLCombatAttackEntityEvent";

    private final Class<?> eventClass;
    private final Method offhand;
    private final BooleanSupplier enabled;
    private final Logger logger;
    private volatile boolean failed;

    private RLCombatHands(Class<?> eventClass, Method offhand, BooleanSupplier enabled, Logger logger) {
        this.eventClass = eventClass;
        this.offhand = offhand;
        this.enabled = enabled;
        this.logger = logger;
    }

    /** The hand source, or {@code null} with one warning when RLCombat's event cannot be read. */
    public static RLCombatHands resolve(BooleanSupplier enabled, Logger logger) {
        try {
            Class<?> eventClass = Class.forName(EVENT_CLASS, false, RLCombatHands.class.getClassLoader());
            Method offhand = eventClass.getMethod("getOffhand");
            if (offhand.getReturnType() != boolean.class) {
                throw new NoSuchMethodException("getOffhand does not return boolean");
            }
            return new RLCombatHands(eventClass, offhand, enabled, logger);
        } catch (ReflectiveOperationException | LinkageError e) {
            logger.warn("RLCombat is installed but its attack event could not be read ({}); off-hand attacks use the "
                    + "main-hand cost", e.toString());
            return null;
        }
    }

    @Override
    public EnumHand handOf(AttackEntityEvent event) {
        if (failed || !enabled.getAsBoolean() || !eventClass.isInstance(event)) {
            return EnumHand.MAIN_HAND;
        }
        try {
            return (Boolean) offhand.invoke(event) ? EnumHand.OFF_HAND : EnumHand.MAIN_HAND;
        } catch (ReflectiveOperationException | RuntimeException e) {
            failed = true;
            logger.warn("RLCombat's attack event could not be read ({}); off-hand attacks now use the main-hand cost",
                    e.toString());
            return EnumHand.MAIN_HAND;
        }
    }
}
