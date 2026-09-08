package com.chuan.apothicflux.recipe;

import java.util.Optional;

/**
 * A min or max requirement for the three Apothic enchanting stats.
 * <p>
 * Min values use {@link Float#NEGATIVE_INFINITY} when absent, and max values use
 * {@link Float#POSITIVE_INFINITY} when absent, so an omitted stat never blocks a
 * recipe.
 */
public record RitualStatRequirement(float eterna, float quanta, float arcana) {

    public static final float NO_MIN = Float.NEGATIVE_INFINITY;
    public static final float NO_MAX = Float.POSITIVE_INFINITY;

    public static final RitualStatRequirement NONE = new RitualStatRequirement(NO_MIN, NO_MIN, NO_MIN);

    public boolean isMinimumSatisfiedBy(RitualStats stats) {
        return stats.eterna() >= this.eterna
                && stats.quanta() >= this.quanta
                && stats.arcana() >= this.arcana;
    }

    public boolean isMaximumSatisfiedBy(RitualStats stats) {
        return stats.eterna() <= this.eterna
                && stats.quanta() <= this.quanta
                && stats.arcana() <= this.arcana;
    }

    public static boolean matches(
            RitualStats stats,
            RitualStatRequirement minimum,
            Optional<RitualStatRequirement> maximum
    ) {
        return minimum.isMinimumSatisfiedBy(stats)
                && maximum.map(m -> m.isMaximumSatisfiedBy(stats)).orElse(true);
    }
}
