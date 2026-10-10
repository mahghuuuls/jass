package com.mahghuuuls.jass;

import net.minecraftforge.fml.common.Loader;
import zone.rong.mixinbooter.ILateMixinLoader;

import java.util.Collections;
import java.util.List;

/**
 * Mixins into other mods' classes, queued by MixinBooter once mods are known: the Elenai Dodge 2
 * feather-bar Mixin only when Elenai is present (REV-008). This class must live outside every Mixin
 * package: Mixin refuses to load ordinary classes from a package a Mixin configuration owns.
 */
public final class JassLateMixins implements ILateMixinLoader {

    static final String ELENAI_CONFIG = "mixins.jass.elenai.json";

    @Override
    public List<String> getMixinConfigs() {
        return Collections.singletonList(ELENAI_CONFIG);
    }

    @Override
    public boolean shouldMixinConfigQueue(String config) {
        return !ELENAI_CONFIG.equals(config) || Loader.isModLoaded("elenaidodge2");
    }
}
