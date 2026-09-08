package com.chuan.apothicflux.recipe;

/**
 * Effective enchanting stats gathered around a ritual core.
 * <p>
 * Quanta includes Apothic's built-in base value of 15, matching the enchanting
 * table exactly.
 */
public record RitualStats(float eterna, float quanta, float arcana) {

    public static final RitualStats BASE = new RitualStats(0, 15, 0);
}
