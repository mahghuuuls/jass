package com.mahghuuuls.jass;

import com.mahghuuuls.jass.compat.baubles.BaublesSlots;
import com.mahghuuuls.jass.compat.inhibited.InhibitedGating;
import com.mahghuuuls.jass.config.ConfigModel;
import com.mahghuuuls.jass.gameplay.JassGameplay;
import net.minecraftforge.fml.common.Loader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Wires optional integrations into the gameplay layer (ARC-008) once every mod has registered its
 * content. An absent mod leaves the default in place and logs nothing.
 */
final class Integrations {

    private static final Logger LOGGER = LogManager.getLogger(Tags.MOD_NAME);

    private Integrations() {
    }

    static void wire(JassGameplay gameplay) {
        InhibitedGating inhibited = InhibitedGating.resolve(() -> ConfigModel.server().inhibitedIntegration());
        if (inhibited != null) {
            gameplay.gate().useGating(inhibited);
            LOGGER.info("Inhibited found: Stamina costs apply only under its effect while inhibited_integration is true");
        }
        // The Baubles API (Baubles or Bubbles) is present only when a mod with the id "baubles" is
        // loaded; BaublesSlots is loaded only then.
        if (Loader.isModLoaded("baubles")) {
            try {
                gameplay.useExtraSlots(new BaublesSlots(LOGGER));
                LOGGER.info("Baubles API found: items in bauble slots count for Item Stamina Modifiers");
            } catch (LinkageError | RuntimeException e) {
                LOGGER.warn("Baubles API could not be used ({}); bauble slots do not count for Stamina", e.toString());
            }
        }
    }
}
