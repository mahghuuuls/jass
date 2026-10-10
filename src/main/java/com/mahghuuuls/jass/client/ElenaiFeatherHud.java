package com.mahghuuuls.jass.client;

/** Decides whether Elenai Dodge 2's feather bar is hidden (REQ-072); asked by the optional Elenai HUD Mixin. */
public final class ElenaiFeatherHud {

    private ElenaiFeatherHud() {
    }

    /** True while the server makes dodges cost Stamina and the player keeps the default option. */
    public static boolean hide() {
        return JassClientConfig.hideElenaiFeatherHud && ClientStaminaState.dodgeUsesStamina();
    }
}
