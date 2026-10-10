package com.mahghuuuls.jass.client;

import com.mahghuuuls.jass.api.client.ItemStaminaInfo;
import com.mahghuuuls.jass.core.StatField;
import net.minecraft.util.text.TextFormatting;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * The Stamina tooltip lines for an item (REQ-058): gray value lines, then the configured modifier
 * lines in JAWMS colors, blue when they help the player and red when they hurt. Translation is
 * passed in, so the lines can be tested without a game.
 */
final class TooltipLines {

    /** Formats a translation key with arguments ({@code I18n.format} in the game). */
    interface Translator {
        String format(String key, Object... args);
    }

    private TooltipLines() {
    }

    static List<String> lines(ItemStaminaInfo info, Translator t) {
        List<String> lines = new ArrayList<>();
        if (!Double.isNaN(info.getSwingCost())) {
            lines.add(TextFormatting.GRAY + t.format("tooltip.jass.swing", number(info.getSwingCost())));
        }
        if (!Double.isNaN(info.getDrawPerSecond())) {
            lines.add(TextFormatting.GRAY + (info.getDrawPerSecond() > 0.0
                    ? t.format("tooltip.jass.draw", number(info.getDrawPerSecond()))
                    : t.format("tooltip.jass.draw_free")));
        }
        if (info.isBlocking()) {
            lines.add(TextFormatting.GRAY + t.format("tooltip.jass.stability", number(info.getStability())));
            lines.add(TextFormatting.GRAY + t.format("tooltip.jass.blocks", percent(info.getMeleeShare()),
                    percent(info.getProjectileShare())));
            lines.add(TextFormatting.GRAY + t.format("tooltip.jass.block_cost", number(info.getBlockCostPerDamage())));
        }
        if (!Double.isNaN(info.getWeight())) {
            // Elenai's piece weights are estimates (it rounds the whole set); shown as plain numbers (owner choice).
            lines.add(TextFormatting.GRAY + t.format("tooltip.jass.weight", number(info.getWeight())));
        } else if (!Double.isNaN(info.getHeldWeight())) {
            // Held weight shares the line; armor shows its worn weight instead (REV-014).
            lines.add(TextFormatting.GRAY + t.format("tooltip.jass.weight", number(info.getHeldWeight())));
        }
        for (ItemStaminaInfo.Modifier modifier : info.getModifiers()) {
            lines.add((modifier.isBeneficial() ? TextFormatting.BLUE : TextFormatting.RED) + modifierText(modifier, t));
        }
        return lines;
    }

    /** {@code +15 Maximum Stamina}, {@code -20% Stamina Regeneration}, {@code +0.5 s Stamina Regeneration Delay}. */
    static String modifierText(ItemStaminaInfo.Modifier modifier, Translator t) {
        String field = modifier.getField();
        double value = modifier.getValue();
        String amount;
        if (field.endsWith("_reduction")) {
            amount = signed(-value * 100.0) + "%";
        } else if (field.endsWith("_increase")) {
            amount = signed(value * 100.0) + "%";
        } else if ("flat_delay".equals(field)) {
            amount = signed(value) + " s";
        } else {
            amount = signed(value);
        }
        return t.format("tooltip.jass.stat." + stat(field), amount);
    }

    private static String stat(String field) {
        StatField stat = StatField.byConfigName(field);
        if (stat == null) {
            return "efficiency";
        }
        switch (stat) {
            case FLAT_MAXIMUM:
            case MAXIMUM_INCREASE:
            case MAXIMUM_REDUCTION:
                return "maximum";
            case FLAT_REGENERATION:
            case REGENERATION_INCREASE:
            case REGENERATION_REDUCTION:
                return "regeneration";
            case FLAT_DELAY:
            case DELAY_INCREASE:
            case DELAY_REDUCTION:
                return "delay";
            default:
                return "efficiency";
        }
    }

    /** At most two decimals, no trailing zeros: 12, 0.67, 22.4; {@code ?} for a value that is not a number. */
    static String number(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return "?";
        }
        BigDecimal rounded = BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros();
        return rounded.compareTo(BigDecimal.ZERO) == 0 ? "0" : rounded.toPlainString();
    }

    private static String signed(double value) {
        String magnitude = number(Math.abs(value));
        return (value < 0.0 && !"0".equals(magnitude) ? "-" : "+") + magnitude;
    }

    private static String percent(double fraction) {
        return number(fraction * 100.0) + "%";
    }
}
