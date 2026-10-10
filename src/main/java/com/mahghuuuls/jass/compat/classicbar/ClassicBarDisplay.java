package com.mahghuuuls.jass.compat.classicbar;

import com.mahghuuuls.jass.client.HudDisplayProvider;
import com.mahghuuuls.jass.client.HudPresenter;
import org.apache.logging.log4j.Logger;
import tfar.classicbar.EventHandler;

/**
 * What the presenter needs from Classic Bar Legacy, behind {@link HudDisplayProvider} (ARC-013).
 * All Classic Bar types stay in this package.
 *
 * <p>Exclusivity (REQ-055): the {@code jassstamina} bar is registered, and the presenter draws no
 * built-in display while Classic Bar has that bar in an order.
 *
 * <p>No row bookkeeping is needed for stacking (REQ-051): Classic Bar advances Forge's
 * {@code right_height} and {@code left_height} by 10 for every bar it draws and leaves them advanced,
 * so a built-in display drawn later already starts above Classic Bar's bars.
 *
 * <p>A failing Classic Bar call logs one warning and the built-in display draws again (ERR-1). The
 * registered bar may then also still draw: two displays, accepted over none.
 */
public final class ClassicBarDisplay implements HudDisplayProvider {

    private final Logger logger;
    private boolean failed;

    private ClassicBarDisplay(Logger logger) {
        this.logger = logger;
    }

    /**
     * Registers the {@code jassstamina} bar. Called during pre-initialization, before Classic Bar
     * builds its active bars from its orders in post-init. Returns {@link HudDisplayProvider#NONE},
     * after one warning, when Classic Bar cannot take the bar.
     */
    public static HudDisplayProvider install(HudPresenter presenter, Logger logger) {
        try {
            EventHandler.register(new StaminaBarOverlay(presenter, logger));
            return new ClassicBarDisplay(logger);
        } catch (LinkageError | RuntimeException e) {
            logger.warn("Could not register the {} bar with Classic Bar Legacy ({}); JASS draws its own display",
                    StaminaBarOverlay.NAME, e.toString());
            return HudDisplayProvider.NONE;
        }
    }

    @Override
    public boolean drawsStamina() {
        if (failed) {
            return false;
        }
        try {
            return EventHandler.isBarEnabled(StaminaBarOverlay.NAME);
        } catch (LinkageError | RuntimeException e) {
            failed = true;
            logger.warn("Could not ask Classic Bar Legacy about the {} bar ({}); JASS draws its own display",
                    StaminaBarOverlay.NAME, e.toString());
            return false;
        }
    }
}
