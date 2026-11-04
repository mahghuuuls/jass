package com.mahghuuuls.jass.core;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProfileAggregatorTest {

    private static final double EPS = 1e-9;

    private static ModifierSet set(Object... pairs) {
        Map<StatField, Double> values = new EnumMap<>(StatField.class);
        for (int i = 0; i < pairs.length; i += 2) {
            values.put((StatField) pairs[i], ((Number) pairs[i + 1]).doubleValue());
        }
        return new ModifierSet(values, "test");
    }

    @Test
    void requirementExampleGivesNinetySix() {
        ProfileAggregator.Stats stats = ProfileAggregator.aggregate(60, 25, 0.75, 0.0, Arrays.asList(
                set(StatField.FLAT_MAXIMUM, 20),
                set(StatField.MAXIMUM_INCREASE, 0.5),
                set(StatField.MAXIMUM_REDUCTION, 0.2)));
        assertEquals(96.0, stats.maximum, EPS);
    }

    @Test
    void noContributionsKeepTheBaseValues() {
        ProfileAggregator.Stats stats = ProfileAggregator.aggregate(60, 25, 0.75, 0.0, Collections.<ModifierSet>emptyList());
        assertEquals(60.0, stats.maximum, EPS);
        assertEquals(25.0, stats.regeneration, EPS);
        assertEquals(0.75, stats.regenerationDelay, EPS);
        assertEquals(0.0, stats.efficiency, EPS);
    }

    @Test
    void goldenBootsExampleDoublesRegenerationAndHalvesTheDelay() {
        ProfileAggregator.Stats stats = ProfileAggregator.aggregate(60, 25, 0.75, 0.0, Collections.singletonList(
                set(StatField.FLAT_REGENERATION, 25, StatField.DELAY_REDUCTION, 0.5)));
        assertEquals(50.0, stats.regeneration, EPS);
        assertEquals(0.375, stats.regenerationDelay, EPS);
    }

    @Test
    void reductionsMultiplyAndEfficiencyAdds() {
        ProfileAggregator.Stats stats = ProfileAggregator.aggregate(100, 0, 0, 0.0, Arrays.asList(
                set(StatField.MAXIMUM_REDUCTION, 0.5, StatField.EFFICIENCY, 30),
                set(StatField.MAXIMUM_REDUCTION, 0.5, StatField.EFFICIENCY, 70)));
        assertEquals(25.0, stats.maximum, EPS);
        assertEquals(100.0, stats.efficiency, EPS);
    }

    @Test
    void twoPenaltiesNeverBecomeABonus() {
        ProfileAggregator.Stats stats = ProfileAggregator.aggregate(60, 25, 0.75, 0.0, Collections.singletonList(
                set(StatField.FLAT_MAXIMUM, -80, StatField.MAXIMUM_INCREASE, -2)));
        assertEquals(0.0, stats.maximum, EPS);
    }

    @Test
    void resolvedValuesNeverGoBelowZero() {
        ProfileAggregator.Stats stats = ProfileAggregator.aggregate(60, 25, 0.75, 0.0, Collections.singletonList(
                set(StatField.FLAT_MAXIMUM, -100)));
        assertEquals(0.0, stats.maximum, EPS);
    }


    private static double weightOf(double sourceWeight, java.util.List<ModifierSet> contributions) {
        return ProfileAggregator.aggregate(60, 25, 0.75, sourceWeight, contributions).effectiveWeight;
    }

    @Test
    void weightModifiersUseTheStatFormula() {
        // REQ-095: (20 - 5) x 1.5 x 0.8 = 18.
        double weight = weightOf(20.0, Arrays.asList(
                set(StatField.FLAT_WEIGHT, -5),
                set(StatField.WEIGHT_INCREASE, 0.5),
                set(StatField.WEIGHT_REDUCTION, 0.2)));
        assertEquals(18.0, weight, EPS);
    }

    @Test
    void weightNeverGoesBelowZero() {
        assertEquals(0.0, weightOf(20.0, Collections.singletonList(set(StatField.FLAT_WEIGHT, -30))), EPS);
        assertEquals(0.0, weightOf(20.0, Collections.singletonList(set(StatField.WEIGHT_INCREASE, -2))), EPS);
        assertEquals(0.0, weightOf(20.0, Collections.singletonList(set(StatField.WEIGHT_REDUCTION, 1.5))), EPS);
    }

    @Test
    void weightWithoutModifiersIsTheSourceWeight() {
        assertEquals(17.5, weightOf(17.5, Collections.<ModifierSet>emptyList()), EPS);
        assertEquals(17.5, weightOf(17.5, Collections.singletonList(set(StatField.FLAT_MAXIMUM, 20))), EPS);
    }

    @Test
    void stabilityBonusIsTheSumAndMayBeNegative() {
        ProfileAggregator.Stats stats = ProfileAggregator.aggregate(60, 25, 0.75, 0.0, Arrays.asList(
                set(StatField.STABILITY, 50), set(StatField.STABILITY, 25), set(StatField.STABILITY, -100)));
        assertEquals(-25.0, stats.stabilityBonus, EPS);
        assertEquals(60.0, stats.maximum, EPS);
    }

    @Test
    void newFieldsDoNotChangeTheOtherStats() {
        ProfileAggregator.Stats stats = ProfileAggregator.aggregate(60, 25, 0.75, 0.0, Collections.singletonList(
                set(StatField.STABILITY, 50, StatField.FLAT_WEIGHT, -5, StatField.WEIGHT_INCREASE, 1,
                        StatField.WEIGHT_REDUCTION, 0.5)));
        assertEquals(60.0, stats.maximum, EPS);
        assertEquals(25.0, stats.regeneration, EPS);
        assertEquals(0.75, stats.regenerationDelay, EPS);
        assertEquals(0.0, stats.efficiency, EPS);
    }

    @Test
    void noContributionsGiveNoStabilityBonus() {
        assertEquals(0.0, ProfileAggregator.aggregate(60, 25, 0.75, 0.0, Collections.<ModifierSet>emptyList()).stabilityBonus, EPS);
    }
}
