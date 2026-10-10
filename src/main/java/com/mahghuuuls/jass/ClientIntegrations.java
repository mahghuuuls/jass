package com.mahghuuuls.jass;

import com.mahghuuuls.jass.client.HudPresenter;
import com.mahghuuuls.jass.client.JassClientConfig;
import com.mahghuuuls.jass.compat.classicbar.ClassicBarDisplay;
import net.minecraftforge.fml.common.Loader;

/**
 * Client-side composition root for optional HUD mods: compat classes are named only here, after
 * the mod is confirmed present and its integration is on, and the presenter sees them through
 * {@code HudDisplayProvider}.
 */
public final class ClientIntegrations {

    private ClientIntegrations() {
    }

    /** Called during pre-initialization, before Classic Bar reads its bar orders in post-init. */
    public static void wire(HudPresenter presenter) {
        if (Loader.isModLoaded("classicbar") && JassClientConfig.classicbarIntegration) {
            presenter.useProvider(ClassicBarDisplay.install(presenter, JustAnotherStaminaSystemMod.LOGGER));
            JustAnotherStaminaSystemMod.LOGGER.info(
                    "Classic Bar Legacy found; registered the jassstamina bar (add it to a Classic Bar order to use it)");
        }
    }
}
