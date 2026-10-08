package com.mahghuuuls.jass.config;

import com.mahghuuuls.jass.Tags;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Owns the current settings snapshot. Gameplay code reads {@link #server()} and never the
 * annotated config fields directly, so a reload replaces every value at once.
 */
public final class ConfigModel {

    private static final Logger LOGGER = LogManager.getLogger(Tags.MOD_NAME);
    private static volatile ServerSettings server = new ServerSettings(0);

    private ConfigModel() {
    }

    public static ServerSettings server() {
        return server;
    }

    /** Rebuilds the snapshot from the annotated config classes. */
    public static void reload() {
        if (!ConfigValues.isValidDebtFraction(JassConfig.maxDebtFraction)) {
            LOGGER.warn("max_debt_fraction={} is not valid (use 0 or more, or -1 for no limit); using {}",
                    JassConfig.maxDebtFraction, ConfigValues.DEFAULT_MAX_DEBT_FRACTION);
        }
        server = new ServerSettings(server.revision() + 1);
        for (ItemList list : server.itemLists()) {
            for (String warning : list.warnings()) {
                LOGGER.warn(warning);
            }
        }
    }

    /** Warns about list entries naming items that are not registered. Call once items exist. */
    public static void reportUnknownItems() {
        for (ItemList list : server.itemLists()) {
            for (String name : list.registryNames()) {
                if (!ForgeRegistries.ITEMS.containsKey(new ResourceLocation(name))) {
                    LOGGER.warn("No item is registered as {}; its configuration entry has no effect", name);
                }
            }
            for (String name : list.namesWithMetadataEntries()) {
                Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(name));
                if (item != null && item.isDamageable()) {
                    LOGGER.warn("{} has durability, so its metadata is its damage; entries with a metadata value for it "
                            + "have no effect (use {} or {}@*)", name, name, name);
                }
            }
        }
    }

    public static final class ChangeListener {

        @SubscribeEvent
        public void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
            if (Tags.MOD_ID.equals(event.getModID())) {
                ConfigManager.sync(Tags.MOD_ID, Config.Type.INSTANCE);
                reload();
                reportUnknownItems();
            }
        }
    }
}
