package com.mahghuuuls.jass.command;

import com.mahghuuuls.jass.JustAnotherStaminaSystemMod;
import com.mahghuuuls.jass.config.ConfigModel;
import com.mahghuuuls.jass.core.StaminaProfile;
import com.mahghuuuls.jass.gameplay.ActionGate;
import com.mahghuuuls.jass.gameplay.DenialRecord;
import com.mahghuuuls.jass.gameplay.JassGameplay;
import com.mahghuuuls.jass.gameplay.StaminaReadout;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * {@code /stamina inspect|restore|set}: operator tools for reading and controlling Stamina.
 * Every result is also written to the server log so it can be read after a test.
 */
public final class StaminaCommand extends CommandBase {

    private static final String USAGE = "/stamina inspect [player] | restore [player] | set <player> <value>";

    @Override
    public String getName() {
        return "stamina";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return USAGE;
    }

    @Override
    public int getRequiredPermissionLevel() {
        return ConfigModel.server().commandPermissionLevel();
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length == 0) {
            throw new WrongUsageException(USAGE);
        }
        ActionGate gate = JassGameplay.get().gate();
        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "inspect": {
                EntityPlayerMP target = target(server, sender, args, 1);
                StaminaReadout readout = gate.read(target);
                reply(sender, describe(target, readout));
                if (readout != null) {
                    for (String contribution : readout.profile().contributions()) {
                        reply(sender, "JASS contribution player=" + target.getName() + " " + contribution);
                    }
                }
                break;
            }
            case "restore": {
                EntityPlayerMP target = target(server, sender, args, 1);
                gate.restore(target);
                reply(sender, "JASS restore player=" + target.getName() + " " + values(gate.read(target)));
                break;
            }
            case "set": {
                if (args.length < 3) {
                    throw new WrongUsageException(USAGE);
                }
                EntityPlayerMP target = getPlayer(server, sender, args[1]);
                double value = parseDouble(args[2]);
                gate.set(target, value);
                reply(sender, "JASS set player=" + target.getName() + " requested=" + format(value) + " "
                        + values(gate.read(target)));
                break;
            }
            default:
                throw new WrongUsageException(USAGE);
        }
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, BlockPos pos) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, "inspect", "restore", "set");
        }
        if (args.length == 2) {
            return getListOfStringsMatchingLastWord(args, server.getOnlinePlayerNames());
        }
        return Collections.emptyList();
    }

    private static EntityPlayerMP target(MinecraftServer server, ICommandSender sender, String[] args, int index)
            throws CommandException {
        return args.length > index ? getPlayer(server, sender, args[index]) : getCommandSenderAsPlayer(sender);
    }

    private static String describe(EntityPlayerMP player, StaminaReadout readout) {
        if (readout == null) {
            return "JASS inspect player=" + player.getName() + " no-session";
        }
        StaminaProfile profile = readout.profile();
        return "JASS inspect player=" + player.getName() + " " + values(readout)
                + " regeneration=" + format(profile.regeneration())
                + " delay=" + format(profile.regenerationDelay())
                + " delayRemaining=" + format(readout.delayRemaining())
                + " efficiency=" + format(profile.efficiency())
                + " weight=" + format(profile.effectiveWeight())
                + " weightSource=" + profile.weightSource()
                + " spending=" + (readout.exempt() ? "exempt" : readout.gated() ? "gated" : "active")
                + " guardBreak=" + format(readout.guardBreakTicks() / 20.0)
                + " lastDenial=" + denial(readout.lastDenial(), player.world.getTotalWorldTime());
    }

    private static String denial(DenialRecord record, long now) {
        if (record == null) {
            return "none";
        }
        return record.action() + "/" + record.reason() + " ticksAgo=" + (now - record.worldTick());
    }

    private static String values(StaminaReadout readout) {
        if (readout == null) {
            return "no-session";
        }
        return "internal=" + format(readout.internal())
                + " visible=" + format(readout.visible())
                + " debt=" + format(readout.debt())
                + " maximum=" + format(readout.profile().maximum());
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private static void reply(ICommandSender sender, String line) {
        sender.sendMessage(new TextComponentString(line));
        JustAnotherStaminaSystemMod.LOGGER.info(line);
    }
}
