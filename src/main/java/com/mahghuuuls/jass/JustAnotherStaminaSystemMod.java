package com.mahghuuuls.jass;

import com.mahghuuuls.jass.command.StaminaCommand;
import com.mahghuuuls.jass.config.ConfigModel;
import com.mahghuuuls.jass.gameplay.AttackHandSource;
import com.mahghuuuls.jass.gameplay.JassGameplay;
import com.mahghuuuls.jass.gameplay.hooks.BowHook;
import com.mahghuuuls.jass.gameplay.hooks.JumpHook;
import com.mahghuuuls.jass.gameplay.hooks.MeleeHook;
import com.mahghuuuls.jass.gameplay.hooks.SprintHook;
import com.mahghuuuls.jass.network.JassNetwork;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppedEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(
        modid = Tags.MOD_ID,
        name = Tags.MOD_NAME,
        version = Tags.VERSION,
        acceptedMinecraftVersions = "[1.12.2]",
        dependencies = "required-after:mixinbooter@[11.8,)")
public class JustAnotherStaminaSystemMod {

    public static final Logger LOGGER = LogManager.getLogger(Tags.MOD_NAME);

    @SidedProxy(
            clientSide = "com.mahghuuuls.jass.client.ClientProxy",
            serverSide = "com.mahghuuuls.jass.CommonProxy")
    public static CommonProxy proxy;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        ConfigModel.reload();
        MinecraftForge.EVENT_BUS.register(new ConfigModel.ChangeListener());
        JassNetwork.register();
        JassGameplay gameplay = JassGameplay.create();
        MeleeHook melee = new MeleeHook(gameplay.gate(), AttackHandSource.MAIN_HAND);
        MinecraftForge.EVENT_BUS.register(melee);
        JassNetwork.setServerSink(melee);
        MinecraftForge.EVENT_BUS.register(new SprintHook(gameplay.gate()));
        MinecraftForge.EVENT_BUS.register(new BowHook(gameplay.gate()));
        JumpHook jump = new JumpHook(gameplay.gate());
        MinecraftForge.EVENT_BUS.register(jump);
        JassNetwork.setJumpSink(jump);
        proxy.preInit();
        LOGGER.info("{} {} loaded", Tags.MOD_NAME, Tags.VERSION);
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        ConfigModel.reportUnknownItems();
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new StaminaCommand());
    }

    @Mod.EventHandler
    public void serverStopped(FMLServerStoppedEvent event) {
        JassGameplay.get().clear();
    }
}
