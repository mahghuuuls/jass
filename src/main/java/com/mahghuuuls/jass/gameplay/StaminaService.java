package com.mahghuuuls.jass.gameplay;

import com.mahghuuuls.jass.api.IStaminaService;
import com.mahghuuuls.jass.api.StaminaMutationResult;
import com.mahghuuuls.jass.api.StaminaPublicState;
import com.mahghuuuls.jass.core.StaminaProfile;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;

/**
 * The API service (ARC-009): validates calls and forwards every change to {@link ActionGate}, so
 * addon actions follow exactly the rules built-in actions follow.
 */
final class StaminaService implements IStaminaService {

    private final ActionGate gate;

    StaminaService(ActionGate gate) {
        this.gate = gate;
    }

    @Override
    public StaminaPublicState getState(EntityPlayer player) {
        serverSide(player);
        StaminaReadout readout = gate.read(player);
        return readout == null ? null : new StaminaPublicState(readout.internal(), readout.profile().maximum());
    }

    @Override
    public double getMaximumStamina(EntityPlayer player) {
        return profile(player).maximum();
    }

    @Override
    public double getStaminaRegeneration(EntityPlayer player) {
        return profile(player).regeneration();
    }

    @Override
    public double getRegenerationDelay(EntityPlayer player) {
        return profile(player).regenerationDelay();
    }

    @Override
    public double getStaminaEfficiency(EntityPlayer player) {
        return profile(player).efficiency();
    }

    @Override
    public double getEffectiveWeight(EntityPlayer player) {
        return profile(player).effectiveWeight();
    }

    @Override
    public boolean isSpendingGated(EntityPlayer player) {
        serverSide(player);
        return gate.gated(player);
    }

    @Override
    public StaminaMutationResult spend(EntityPlayer player, double amount, ResourceLocation cause) {
        check(player, amount, cause);
        StaminaPublicState before = getState(player);
        boolean done = gate.spendDirect(player, amount, cause);
        return new StaminaMutationResult(done, amount, cause, before, getState(player));
    }

    @Override
    public StaminaMutationResult restore(EntityPlayer player, double amount, ResourceLocation cause) {
        check(player, amount, cause);
        StaminaPublicState before = getState(player);
        boolean done = gate.restoreAmount(player, amount, cause);
        return new StaminaMutationResult(done, amount, cause, before, getState(player));
    }

    @Override
    public StaminaMutationResult set(EntityPlayer player, double value, ResourceLocation cause) {
        check(player, 0.0, cause);
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            throw new IllegalArgumentException("value must be a finite number");
        }
        StaminaPublicState before = getState(player);
        boolean done = gate.set(player, value, cause);
        return new StaminaMutationResult(done, value, cause, before, getState(player));
    }

    @Override
    public StaminaMutationResult tryDiscreteAction(EntityPlayer player, ResourceLocation action, double baseCost,
            boolean movement) {
        check(player, baseCost, action);
        StaminaPublicState before = getState(player);
        boolean done = before != null
                && gate.tryDiscrete(player, action, baseCost, movement ? CostKind.MOVEMENT : CostKind.STANDARD);
        return new StaminaMutationResult(done, baseCost, action, before, getState(player));
    }

    @Override
    public StaminaMutationResult drainContinuousTick(EntityPlayer player, ResourceLocation action,
            double costPerSecond, boolean movement) {
        check(player, costPerSecond, action);
        StaminaPublicState before = getState(player);
        boolean done = before != null && gate.drainContinuous(player, action, costPerSecond,
                movement ? CostKind.MOVEMENT : CostKind.STANDARD);
        return new StaminaMutationResult(done, costPerSecond, action, before, getState(player));
    }

    private StaminaProfile profile(EntityPlayer player) {
        serverSide(player);
        StaminaReadout readout = gate.read(player);
        if (readout == null) {
            throw new IllegalArgumentException("player has no Stamina session");
        }
        return readout.profile();
    }

    /** Every service call is for the logical server; client code reads ClientStaminaApi. */
    private static void serverSide(EntityPlayer player) {
        if (player == null) {
            throw new IllegalArgumentException("player is required");
        }
        if (player.world.isRemote) {
            throw new IllegalStateException("IStaminaService is server-side; on the client use ClientStaminaApi");
        }
    }

    private static void check(EntityPlayer player, double amount, ResourceLocation cause) {
        serverSide(player);
        if (cause == null) {
            throw new IllegalArgumentException("cause is required");
        }
        if (Double.isNaN(amount) || Double.isInfinite(amount) || amount < 0.0) {
            throw new IllegalArgumentException("amount must be a finite number of at least 0");
        }
    }
}
