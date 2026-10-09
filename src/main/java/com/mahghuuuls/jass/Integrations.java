package com.mahghuuuls.jass;

import com.mahghuuuls.jass.compat.baubles.BaublesSlots;
import com.mahghuuuls.jass.compat.elenai.ElenaiDodge;
import com.mahghuuuls.jass.compat.elenai.ElenaiWeight;
import com.mahghuuuls.jass.compat.rlcombat.RLCombatHands;
import com.mahghuuuls.jass.compat.inhibited.InhibitedGating;
import com.mahghuuuls.jass.config.ConfigModel;
import com.mahghuuuls.jass.gameplay.JassGameplay;
import com.mahghuuuls.jass.gameplay.hooks.MeleeHook;
import net.minecraftforge.common.MinecraftForge;
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

    static void wire(JassGameplay gameplay, MeleeHook melee) {
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
        // RLCombat (mod id "bettercombatmod") is read reflectively; no RLCombat class is linked.
        if (Loader.isModLoaded("bettercombatmod")) {
            RLCombatHands hands = RLCombatHands.resolve(() -> ConfigModel.server().rlcombatIntegration(), LOGGER);
            if (hands != null) {
                melee.useHandSource(hands);
                LOGGER.info("RLCombat found: off-hand attacks cost the off-hand item while rlcombat_integration is true");
            }
        }
        // Elenai Dodge 2 classes are loaded only when it is present.
        if (Loader.isModLoaded("elenaidodge2")) {
            try {
                MinecraftForge.EVENT_BUS.register(new ElenaiDodge(gameplay.gate(),
                        () -> ConfigModel.server().elenaiDodgeResourceIntegration(),
                        () -> ConfigModel.server().elenaiDodgeBaseCost(), LOGGER));
                gameplay.useWeightSource(new ElenaiWeight(() -> ConfigModel.server().elenaiWeightSourceIntegration(),
                        () -> ConfigModel.server().elenaiWeightConversion(), LOGGER));
                LOGGER.info("Elenai Dodge 2 found: dodges cost Stamina and weight follows Elenai while their options are true");
            } catch (LinkageError | RuntimeException e) {
                LOGGER.warn("Elenai Dodge 2 could not be used ({}); dodges and weight are unaffected by JASS", e.toString());
            }
        }
    }
}
