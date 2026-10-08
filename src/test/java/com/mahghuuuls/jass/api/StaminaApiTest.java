package com.mahghuuuls.jass.api;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** API value types and registration rules that need no running game. */
class StaminaApiTest {

    /** A service stub; only identity matters for the binding test. */
    private static final class NoService implements IStaminaService {
        public StaminaPublicState getState(EntityPlayer p) { return null; }
        public double getMaximumStamina(EntityPlayer p) { return 0; }
        public double getStaminaRegeneration(EntityPlayer p) { return 0; }
        public double getRegenerationDelay(EntityPlayer p) { return 0; }
        public double getStaminaEfficiency(EntityPlayer p) { return 0; }
        public double getEffectiveWeight(EntityPlayer p) { return 0; }
        public boolean isSpendingGated(EntityPlayer p) { return false; }
        public StaminaMutationResult spend(EntityPlayer p, double a, ResourceLocation c) { return null; }
        public StaminaMutationResult restore(EntityPlayer p, double a, ResourceLocation c) { return null; }
        public StaminaMutationResult set(EntityPlayer p, double v, ResourceLocation c) { return null; }
        public StaminaMutationResult tryDiscreteAction(EntityPlayer p, ResourceLocation a, double b, boolean m) { return null; }
        public StaminaMutationResult drainContinuousTick(EntityPlayer p, ResourceLocation a, double c, boolean m) { return null; }
    }

    @Test
    void contributionBuilderKeepsValuesAndRejectsNonFiniteOnes() {
        StaminaContribution c = StaminaContribution.builder().flatMaximum(10).efficiency(25).build();
        assertEquals(10.0, c.getFlatMaximum(), 1e-9);
        assertEquals(25.0, c.getEfficiency(), 1e-9);
        assertFalse(c.isEmpty());
        assertTrue(StaminaContribution.EMPTY.isEmpty());
        assertThrows(IllegalArgumentException.class, () -> StaminaContribution.builder().flatMaximum(Double.NaN));
    }

    @Test
    void mutationResultReportsTheDelta() {
        StaminaMutationResult result = new StaminaMutationResult(true, 10, new ResourceLocation("testaddon", "charge"),
                new StaminaPublicState(60, 60), new StaminaPublicState(50, 60));
        assertEquals(-10.0, result.getActualDelta(), 1e-9);
        assertEquals("testaddon:charge", result.getCause().toString());
    }

    @Test
    void publicStateSplitsVisibleStaminaAndDebt() {
        StaminaPublicState state = new StaminaPublicState(-7, 60);
        assertEquals(0.0, state.getVisibleStamina(), 1e-9);
        assertEquals(7.0, state.getDebt(), 1e-9);
    }

    @Test
    void registrationRejectsDuplicatesAndClosesWhenFrozen() {
        ResourceLocation id = new ResourceLocation("testaddon", "context");
        JassApi.registerContextualProvider(id, player -> StaminaContribution.EMPTY);
        assertThrows(IllegalArgumentException.class,
                () -> JassApi.registerContextualProvider(id, player -> StaminaContribution.EMPTY));
        JassApi.Control control = JassApi.bind(new NoService(), player -> { });
        assertThrows(IllegalStateException.class, () -> JassApi.bind(new NoService(), player -> { }));
        control.freezeRegistration();
        assertTrue(JassApi.isProviderRegistrationFrozen());
        assertThrows(IllegalStateException.class, () -> JassApi.registerContextualProvider(
                new ResourceLocation("testaddon", "late"), player -> StaminaContribution.EMPTY));
    }
}
