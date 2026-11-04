package com.mahghuuuls.jass.gameplay;

import com.mahghuuuls.jass.api.StaminaContribution;
import com.mahghuuuls.jass.core.ModifierSet;
import com.mahghuuuls.jass.core.StatField;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ProviderMappingTest {

    @Test
    void theNewProviderFieldsMapToTheirOwnStatFields() {
        ModifierSet set = ProfileService.toModifierSet(StaminaContribution.builder()
                .stability(25).flatWeight(-2).weightIncrease(0.3).weightReduction(0.4).build(), "test");
        assertEquals(25.0, set.get(StatField.STABILITY), 1e-9);
        assertEquals(-2.0, set.get(StatField.FLAT_WEIGHT), 1e-9);
        assertEquals(0.3, set.get(StatField.WEIGHT_INCREASE), 1e-9);
        assertEquals(0.4, set.get(StatField.WEIGHT_REDUCTION), 1e-9);
        assertFalse(set.has(StatField.EFFICIENCY));
    }

    @Test
    void theExistingProviderFieldsStillMap() {
        ModifierSet set = ProfileService.toModifierSet(StaminaContribution.builder()
                .flatMaximum(10).efficiency(5).delayReduction(0.2).build(), "test");
        assertEquals(10.0, set.get(StatField.FLAT_MAXIMUM), 1e-9);
        assertEquals(5.0, set.get(StatField.EFFICIENCY), 1e-9);
        assertEquals(0.2, set.get(StatField.DELAY_REDUCTION), 1e-9);
        assertFalse(set.has(StatField.STABILITY));
    }
}
