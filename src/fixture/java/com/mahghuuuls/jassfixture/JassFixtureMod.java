package com.mahghuuuls.jassfixture;

import com.mahghuuuls.jass.api.IStaminaService;
import com.mahghuuuls.jass.api.JassActions;
import com.mahghuuuls.jass.api.JassApi;
import com.mahghuuuls.jass.api.StaminaChangeEvent;
import com.mahghuuuls.jass.api.StaminaContribution;
import com.mahghuuuls.jass.api.StaminaCostEvent;
import com.mahghuuuls.jass.api.StaminaMutationResult;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Locale;

/**
 * Development-only API fixture for IMP-012 (never packaged): exercises every JASS API operation
 * through {@code /jassfixture} and logs each result as a {@code JASSFIXTURE} line.
 */
@Mod(modid = "jassfixture", name = "JASS API fixture", version = "dev", dependencies = "required-after:jass")
public final class JassFixtureMod {

    static final Logger LOGGER = LogManager.getLogger("JASSFIXTURE");
    static final ResourceLocation CHARGE = new ResourceLocation("jassfixture", "charge");
    static final ResourceLocation STRIKE = new ResourceLocation("jassfixture", "strike");
    static final ResourceLocation STICK_PROVIDER = new ResourceLocation("jassfixture", "stick");
    static final ResourceLocation THROWING_PROVIDER = new ResourceLocation("jassfixture", "throwing");

    static volatile boolean halveMelee;
    static volatile boolean throwInProvider;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        JassApi.registerItemProvider(STICK_PROVIDER, Items.STICK,
                (player, context) -> StaminaContribution.builder().flatMaximum(10).build());
        JassApi.registerContextualProvider(THROWING_PROVIDER, player -> {
            if (throwInProvider) {
                throw new IllegalStateException("fixture provider failure");
            }
            return StaminaContribution.EMPTY;
        });
        MinecraftForge.EVENT_BUS.register(this);
        if (FMLCommonHandler.instance().getSide() == Side.CLIENT) {
            FixtureClient.init();
        }
        LOGGER.info("JASSFIXTURE loaded api={}", JassApi.API_VERSION);
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new FixtureCommand());
    }

    @SubscribeEvent
    public void onCost(StaminaCostEvent event) {
        if (halveMelee && JassActions.MELEE.equals(event.getAction())) {
            event.setCost(event.getCost() / 2.0);
            LOGGER.info("JASSFIXTURE cost action={} halvedTo={}", event.getAction(), format(event.getCost()));
        }
    }

    @SubscribeEvent
    public void onChange(StaminaChangeEvent event) {
        if (JassActions.SPRINT.equals(event.getCause()) || JassActions.BOW.equals(event.getCause())) {
            return; // per-tick drains would flood the log
        }
        LOGGER.info("JASSFIXTURE change player={} old={} new={} cause={}", event.getPlayer().getName(),
                format(event.getOldState().getStamina()), format(event.getNewState().getStamina()), event.getCause());
    }

    static String format(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    static String describe(StaminaMutationResult result) {
        return "successful=" + result.isSuccessful() + " requested=" + format(result.getRequestedAmount())
                + " delta=" + format(result.getActualDelta()) + " cause=" + result.getCause()
                + " new=" + (result.getNewState() == null ? "none" : format(result.getNewState().getStamina()));
    }

    static final class FixtureCommand extends CommandBase {

        @Override
        public String getName() {
            return "jassfixture";
        }

        @Override
        public String getUsage(ICommandSender sender) {
            return "/jassfixture <spend|discrete|halve on|off|throw on|off|gated|lateregister|state>";
        }

        @Override
        public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
            EntityPlayerMP player = getCommandSenderAsPlayer(sender);
            IStaminaService stamina = JassApi.getStaminaService();
            String sub = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);
            String on = args.length > 1 ? args[1].toLowerCase(Locale.ROOT) : "";
            switch (sub) {
                case "spend":
                    log(sender, "spend " + describe(stamina.spend(player, 10, CHARGE)));
                    break;
                case "discrete":
                    log(sender, "discrete " + describe(stamina.tryDiscreteAction(player, STRIKE, 10, false)));
                    break;
                case "halve":
                    halveMelee = "on".equals(on);
                    log(sender, "halve melee=" + halveMelee);
                    break;
                case "throw":
                    throwInProvider = "on".equals(on);
                    JassApi.markModifierCacheDirty(player);
                    log(sender, "throw provider=" + throwInProvider + " maximum=" + format(stamina.getMaximumStamina(player)));
                    break;
                case "gated":
                    log(sender, "gated=" + stamina.isSpendingGated(player));
                    break;
                case "lateregister":
                    try {
                        JassApi.registerContextualProvider(new ResourceLocation("jassfixture", "late"),
                                p -> StaminaContribution.EMPTY);
                        log(sender, "lateregister accepted (unexpected)");
                    } catch (IllegalStateException e) {
                        log(sender, "lateregister refused: " + e.getMessage());
                    }
                    break;
                case "state":
                    log(sender, "state stamina=" + format(stamina.getState(player).getStamina()) + " maximum="
                            + format(stamina.getMaximumStamina(player)) + " frozen=" + JassApi.isProviderRegistrationFrozen());
                    break;
                default:
                    throw new CommandException(getUsage(sender));
            }
        }

        private static void log(ICommandSender sender, String text) {
            LOGGER.info("JASSFIXTURE {}", text);
            sender.sendMessage(new net.minecraft.util.text.TextComponentString("JASSFIXTURE " + text));
        }
    }
}
