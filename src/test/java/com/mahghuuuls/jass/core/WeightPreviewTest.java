package com.mahghuuuls.jass.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WeightPreviewTest {

    @Test
    void standaloneUsesTheConfiguredWeightOrArmorPoints() {
        assertEquals(6.0, WeightPreview.standalone(6.0, 8.0, 1.0));
        assertEquals(8.0, WeightPreview.standalone(Double.NaN, 8.0, 1.0));
        assertEquals(4.0, WeightPreview.standalone(Double.NaN, 8.0, 0.5));
    }

    @Test
    void elenaiPieceFromArmorPointsWithIntegerHalving() {
        // Iron chestplate, 6 armor: (6 / 2) x 1.8 = 5.4, times 1.4 = 7.56.
        assertEquals(7.56, WeightPreview.elenaiPiece(Double.NaN, true, 6, 1.4), 1e-9);
        // Leather helmet, 1 armor: (1 / 2 = 0) x 1.8 = 0.
        assertEquals(0.0, WeightPreview.elenaiPiece(Double.NaN, true, 1, 1.4), 1e-9);
    }

    @Test
    void elenaiOverrideWinsAndNonArmorWithoutOverrideWeighsNothing() {
        assertEquals(14.0, WeightPreview.elenaiPiece(10.0, true, 6, 1.4), 1e-9);
        assertEquals(0.0, WeightPreview.elenaiPiece(Double.NaN, false, 0, 1.4), 1e-9);
        assertEquals(0.0, WeightPreview.elenaiPiece(10.0, true, 6, -1.0), 1e-9);
    }
}
