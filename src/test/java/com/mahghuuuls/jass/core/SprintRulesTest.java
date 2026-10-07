package com.mahghuuuls.jass.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SprintRulesTest {

    @Test
    void sprintingOnLandCosts() {
        assertTrue(SprintRules.costsStamina(true, false, false, false, false));
    }

    @Test
    void notSprintingNeverCosts() {
        assertFalse(SprintRules.costsStamina(false, false, false, false, false));
    }

    @Test
    void waterRidingGlidingAndFlyingAreFree() {
        assertFalse(SprintRules.costsStamina(true, true, false, false, false));
        assertFalse(SprintRules.costsStamina(true, false, true, false, false));
        assertFalse(SprintRules.costsStamina(true, false, false, true, false));
        assertFalse(SprintRules.costsStamina(true, false, false, false, true));
    }
}
