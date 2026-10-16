package com.mahghuuuls.jassfixture;

import com.mahghuuuls.jass.api.IItemValueProvider;
import com.mahghuuuls.jass.api.IStaminaService;
import com.mahghuuuls.jass.api.JassActions;
import com.mahghuuuls.jass.api.JassApi;
import com.mahghuuuls.jass.api.StaminaChangeEvent;
import com.mahghuuuls.jass.api.StaminaContribution;
import com.mahghuuuls.jass.api.StaminaCostEvent;
import com.mahghuuuls.jass.api.StaminaMutationResult;
import com.mahghuuuls.jass.api.PreparationResult;
import com.mahghuuuls.jass.api.PreparedStaminaAction;
import com.mahghuuuls.jass.api.SettlementResult;
import com.mahghuuuls.jass.api.StaminaPublicState;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.EnumAction;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Locale;
import java.util.UUID;

/**
 * Development-only API fixture for IMP-012 (never packaged): exercises every JASS API operation
 * through {@code /jassfixture} and logs each result as a {@code JASSFIXTURE} line.
 */
@Mod(modid = "jassfixture", name = "JASS API fixture", version = "dev", dependencies = "required-after:jass")
public final class JassFixtureMod {

    static final Logger LOGGER = LogManager.getLogger("JASSFIXTURE");
    static final ResourceLocation CHARGE = new ResourceLocation("jassfixture", "charge");
    static final ResourceLocation STRIKE = new ResourceLocation("jassfixture", "strike");
    /** A spell-like action the prepared-action scenarios use (CYCLE-001). */
    static final ResourceLocation CAST = new ResourceLocation("jassfixture", "cast");
    /** A channeled continuous action prepared every tick (CYCLE-001 IMP-002). */
    static final ResourceLocation CHANNEL = new ResourceLocation("jassfixture", "channel");
    /** A plain addon drain used against an open reserve. */
    static final ResourceLocation DRAIN = new ResourceLocation("jassfixture", "drain");
    /** Channel cost per second: 2 per tick before efficiency. */
    static final double CHANNEL_COST = 40;
    static final ResourceLocation STICK_PROVIDER = new ResourceLocation("jassfixture", "stick");
    static final ResourceLocation THROWING_PROVIDER = new ResourceLocation("jassfixture", "throwing");

    static volatile boolean halveMelee;
    static volatile boolean throwInProvider;
    /** One-shot: the next pricing of a cast starts another action from inside the cost event. */
    static volatile boolean nestOnPrice;
    /** One-shot: the next change notification for a cast throws (a broken listener). */
    static volatile boolean throwOnChange;
    /** Armed by `prepared logout`: prepare at logout before JASS removes the session, settle after. */
    static volatile boolean prepareAtLogout;
    private static PreparedStaminaAction logoutCast;
    /** The id of the player channeling, or null; set by `/jassfixture channel on`. Kept across respawn. */
    private static UUID channeler;
    private static PreparedStaminaAction channelTick;
    private static int channelTicks;

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
        JassApi.registerItemValueProvider(new ResourceLocation("jassfixture", "values"), new IItemValueProvider() {
            // Golden hoe: no configuration entry, so the provider's swing cost applies.
            @Override
            public double meleeCost(ItemStack stack) {
                // Wooden hoe: the campaign configures 3, which wins over this 5.
                return stack.getItem() == Items.GOLDEN_HOE ? 7.0 : stack.getItem() == Items.WOODEN_HOE ? 5.0 : Double.NaN;
            }

            // Shield: applies only while the configuration has no shield entries (configuration wins).
            @Override
            public double shieldStability(ItemStack stack) {
                return stack.getItem() == Items.SHIELD ? 30.0 : Double.NaN;
            }

            @Override
            public double shieldMeleeBlock(ItemStack stack) {
                return stack.getItem() == Items.SHIELD ? 0.5 : Double.NaN;
            }

            @Override
            public double shieldProjectileBlock(ItemStack stack) {
                return stack.getItem() == Items.SHIELD ? 0.25 : Double.NaN;
            }

            // Chainmail chestplate has a configured weight (4), which wins; the pumpkin has none.
            @Override
            public double armorWeight(ItemStack stack) {
                return stack.getItem() == Items.CHAINMAIL_CHESTPLATE ? 9.0
                        : stack.getItem() == Item.getItemFromBlock(Blocks.PUMPKIN) ? 3.0 : Double.NaN;
            }

            // Golden hoe has no held_weight entry, so 5 applies; the shield's configured 4 wins over 9.
            @Override
            public double heldWeight(ItemStack stack) {
                return stack.getItem() == Items.GOLDEN_HOE ? 5.0 : stack.getItem() == Items.SHIELD ? 9.0 : Double.NaN;
            }
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

    private static String lastCostContext = "";

    @SubscribeEvent
    public void onCost(StaminaCostEvent event) {
        if (CAST.equals(event.getAction())) {
            LOGGER.info("JASSFIXTURE cost cast player={} cost={}", event.getPlayer().getName(), format(event.getCost()));
        }
        if (nestOnPrice && CAST.equals(event.getAction())) {
            nestOnPrice = false;
            LOGGER.info("JASSFIXTURE prepared listener inner {}", describe(
                    JassApi.getStaminaService().tryDiscreteAction(event.getPlayer(), STRIKE, 10, false)));
        }
        String context = event.getAction() + " item=" + (event.getStack().isEmpty() ? "none"
                : event.getStack().getItem().getRegistryName()) + " hand=" + event.getHand();
        if (!context.equals(lastCostContext)) {
            lastCostContext = context;
            LOGGER.info("JASSFIXTURE cost context action={} cost={}", context, format(event.getCost()));
        }
        if (halveMelee && JassActions.MELEE.equals(event.getAction())) {
            event.setCost(event.getCost() / 2.0);
            LOGGER.info("JASSFIXTURE cost action={} halvedTo={}", event.getAction(), format(event.getCost()));
        }
    }

    /** Once a second while Stamina is below maximum: the internal value and whether a shield is raised (REV-015 cards). */
    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.world.isRemote
                || event.player.world.getTotalWorldTime() % 20 != 0) {
            return;
        }
        StaminaPublicState state = JassApi.getStaminaService().getState(event.player);
        if (state == null || state.getStamina() >= state.getMaximumStamina()) {
            return;
        }
        boolean raised = event.player.isHandActive()
                && event.player.getActiveItemStack().getItemUseAction() == EnumAction.BLOCK;
        LOGGER.info("JASSFIXTURE pool internal={} shieldRaised={}", format(state.getStamina()), raised);
    }

    @SubscribeEvent
    public void onChange(StaminaChangeEvent event) {
        if (throwOnChange && CAST.equals(event.getCause())) {
            throwOnChange = false;
            LOGGER.info("JASSFIXTURE prepared throwlistener throwing now");
            throw new IllegalStateException("fixture change listener failure");
        }
        if (JassActions.SPRINT.equals(event.getCause()) || JassActions.BOW.equals(event.getCause())
                || CHANNEL.equals(event.getCause())) {
            return; // per-tick drains would flood the log
        }
        LOGGER.info("JASSFIXTURE change player={} old={} new={} cause={}", event.getPlayer().getName(),
                format(event.getOldState().getStamina()), format(event.getNewState().getStamina()), event.getCause());
    }

    /** Channel, part 1: prepares this tick before JASS drains sprint (sprint drains at tick start, normal priority). */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onChannelPrepare(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.START || event.player.world.isRemote
                || !event.player.getUniqueID().equals(channeler)) {
            return;
        }
        try {
            channelTick = JassApi.getStaminaService().prepareContinuousTick(event.player, CHANNEL, CHANNEL_COST, false,
                    ItemStack.EMPTY, null);
        } catch (RuntimeException e) {
            LOGGER.warn("JASSFIXTURE channel prepare failed; channel off", e);
            channelTick = null;
            channeler = null;
            return;
        }
        if (channelTick.getResult() == PreparationResult.REFUSED) {
            LOGGER.info("JASSFIXTURE channel refused after {} ticks; channel off", channelTicks);
            channelTick = null;
            channeler = null;
        }
    }

    /** Channel, part 2: the tick's effect happened, so settle; stop on PAID_STOP (owner terminal policy A). */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onChannelSettle(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.world.isRemote || channelTick == null
                || !event.player.getUniqueID().equals(channeler)) {
            return;
        }
        PreparedStaminaAction tick = channelTick;
        channelTick = null;
        channelTicks++;
        SettlementResult result = tick.settle();
        if (result.mustStop() || channelTicks % 20 == 0) {
            LOGGER.info("JASSFIXTURE channel tick={} reserve={} outcome={} paid={} new={}", channelTicks,
                    format(tick.getFinalCost()), result.getOutcome(), format(result.getAmountPaid()),
                    result.getNewState() == null ? "none" : format(result.getNewState().getStamina()));
        }
        if (result.mustStop()) {
            LOGGER.info("JASSFIXTURE channel stopped by PAID_STOP after {} ticks; channel off", channelTicks);
            channeler = null;
        }
    }

    /** Logout scenario, part 1: runs before JASS removes the session, so the cast is accepted. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onLogoutPrepare(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.player.getUniqueID().equals(channeler)) {
            LOGGER.info("JASSFIXTURE channel ended by logout after {} ticks; channel off", channelTicks);
            channelTick = null;
            channeler = null;
        }
        if (!prepareAtLogout || event.player.world.isRemote) {
            return;
        }
        prepareAtLogout = false;
        logoutCast = JassApi.getStaminaService().prepareDiscreteAction(event.player, CAST, 12, false, ItemStack.EMPTY, null);
        LOGGER.info("JASSFIXTURE prepared logout result={} cost={} valid={}", logoutCast.getResult(),
                format(logoutCast.getFinalCost()), logoutCast.isValid());
    }

    /** Logout scenario, part 2: runs after JASS removed the session, so settling must be invalidated. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onLogoutSettle(PlayerEvent.PlayerLoggedOutEvent event) {
        if (logoutCast == null) {
            return;
        }
        PreparedStaminaAction cast = logoutCast;
        logoutCast = null;
        SettlementResult result = cast.settle();
        LOGGER.info("JASSFIXTURE prepared logout settle outcome={} paid={} valid={}", result.getOutcome(),
                format(result.getAmountPaid()), cast.isValid());
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
            return "/jassfixture <spend|discrete|halve on|off|throw on|off|gated|lateregister|state"
                    + "|prepared success|fail|pending|nested|listener|set|throwlistener|logout|reserve|spendfloor|twice"
                    + "|channel on|off>";
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
                case "prepared":
                    prepared(sender, player, stamina, on);
                    break;
                case "channel":
                    channelTick = null;
                    channelTicks = 0;
                    channeler = "on".equals(on) ? player.getUniqueID() : null;
                    log(sender, "channel " + (channeler != null ? "on" : "off") + " " + state(player, stamina));
                    break;
                default:
                    throw new CommandException(getUsage(sender));
            }
        }

        /** Prepared-action scenarios: a cast of base cost 12 that succeeds, fails, or meets other actions. */
        private static void prepared(ICommandSender sender, EntityPlayerMP player, IStaminaService stamina, String scenario)
                throws CommandException {
            PreparedStaminaAction cast;
            switch (scenario) {
                case "success":
                    cast = prepare(sender, player, stamina, "success");
                    settle(sender, "success", cast);
                    break;
                case "fail":
                    cast = prepare(sender, player, stamina, "fail");
                    cast.release();
                    log(sender, "prepared fail released valid=" + cast.isValid() + " " + state(player, stamina));
                    break;
                case "pending":
                    cast = prepare(sender, player, stamina, "pending");
                    log(sender, "prepared pending left open valid=" + cast.isValid());
                    break;
                case "nested":
                    cast = prepare(sender, player, stamina, "nested-outer");
                    PreparedStaminaAction inner = stamina.prepareDiscreteAction(player, CAST, 12, false, ItemStack.EMPTY, null);
                    log(sender, "prepared nested inner-prepare result=" + inner.getResult());
                    inner.release();
                    log(sender, "prepared nested inner-direct " + describe(stamina.tryDiscreteAction(player, STRIKE, 10, false)));
                    cast.release();
                    log(sender, "prepared nested outer released " + state(player, stamina));
                    break;
                case "listener":
                    nestOnPrice = true;
                    cast = prepare(sender, player, stamina, "listener");
                    nestOnPrice = false;
                    settle(sender, "listener", cast);
                    break;
                case "set":
                    cast = prepare(sender, player, stamina, "set");
                    stamina.set(player, 40, CHARGE);
                    log(sender, "prepared set to 40 valid=" + cast.isValid());
                    settle(sender, "set", cast);
                    break;
                case "throwlistener":
                    throwOnChange = true;
                    cast = prepare(sender, player, stamina, "throwlistener");
                    settle(sender, "throwlistener", cast);
                    throwOnChange = false;
                    break;
                case "logout":
                    prepareAtLogout = true;
                    log(sender, "prepared logout armed: leave the world now");
                    break;
                case "reserve": {
                    // REQ-089 criterion 2 and REQ-094 criterion 1: Stamina 5, a reserve, a drain beside it, then settle.
                    stamina.set(player, 5, CHARGE);
                    PreparedStaminaAction tick = stamina.prepareContinuousTick(player, CHANNEL, CHANNEL_COST, false,
                            ItemStack.EMPTY, null);
                    log(sender, "prepared reserve result=" + tick.getResult() + " reserve=" + format(tick.getFinalCost())
                            + " " + state(player, stamina));
                    log(sender, "prepared reserve drain-beside " + describe(stamina.drainContinuousTick(player, DRAIN, 200, false)));
                    settle(sender, "reserve", tick);
                    break;
                }
                case "spendfloor": {
                    // REQ-089 criterion 4: Stamina 5, a reserve, spend 10 stops above the reserve.
                    stamina.set(player, 5, CHARGE);
                    PreparedStaminaAction tick = stamina.prepareContinuousTick(player, CHANNEL, CHANNEL_COST, false,
                            ItemStack.EMPTY, null);
                    log(sender, "prepared spendfloor result=" + tick.getResult() + " reserve=" + format(tick.getFinalCost())
                            + " " + state(player, stamina));
                    log(sender, "prepared spendfloor spend " + describe(stamina.spend(player, 10, CHARGE)));
                    settle(sender, "spendfloor", tick);
                    break;
                }
                case "twice":
                    cast = prepare(sender, player, stamina, "twice");
                    settle(sender, "twice-1", cast);
                    settle(sender, "twice-2", cast);
                    break;
                default:
                    throw new CommandException("/jassfixture prepared <success|fail|pending|nested|listener|set|throwlistener|logout"
                            + "|reserve|spendfloor|twice>");
            }
        }

        private static PreparedStaminaAction prepare(ICommandSender sender, EntityPlayerMP player, IStaminaService stamina,
                String label) {
            String before = state(player, stamina);
            PreparedStaminaAction cast = stamina.prepareDiscreteAction(player, CAST, 12, false, player.getHeldItemMainhand(),
                    net.minecraft.util.EnumHand.MAIN_HAND);
            log(sender, "prepared " + label + " result=" + cast.getResult() + " cost=" + format(cast.getFinalCost())
                    + " valid=" + cast.isValid() + " before " + before + " after-prepare " + state(player, stamina));
            return cast;
        }

        private static void settle(ICommandSender sender, String label, PreparedStaminaAction cast) {
            SettlementResult result = cast.settle();
            log(sender, "prepared " + label + " settle outcome=" + result.getOutcome() + " paid=" + format(result.getAmountPaid())
                    + " new=" + (result.getNewState() == null ? "none" : format(result.getNewState().getStamina())));
        }

        private static String state(EntityPlayerMP player, IStaminaService stamina) {
            return "stamina=" + format(stamina.getState(player).getStamina());
        }

        private static void log(ICommandSender sender, String text) {
            LOGGER.info("JASSFIXTURE {}", text);
            sender.sendMessage(new net.minecraft.util.text.TextComponentString("JASSFIXTURE " + text));
        }
    }
}
