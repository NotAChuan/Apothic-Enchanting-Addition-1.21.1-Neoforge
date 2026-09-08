package com.chuan.apothicflux.recipe;

import java.util.Optional;

/**
 * Pure-logic tests for ritual stat requirements.
 * Run with assertions enabled: {@code java -ea ...}
 */
public final class RitualStatRequirementTest {

    public static void main(String[] args) {
        minimumSatisfied();
        minimumFailsForEachStat();
        maximumSatisfied();
        maximumFailsForEachStat();
        combinedRangeSatisfied();
        combinedRangeRejectedByMinimum();
        combinedRangeRejectedByMaximum();
        noneAlwaysSatisfied();
        System.out.println("All RitualStatRequirement tests passed.");
    }

    private static void minimumSatisfied() {
        RitualStats stats = new RitualStats(50, 20, 30);
        RitualStatRequirement min = new RitualStatRequirement(40, 15, 25);
        assertTrue(min.isMinimumSatisfiedBy(stats), "minimum should be satisfied");
    }

    private static void minimumFailsForEachStat() {
        RitualStatRequirement min = new RitualStatRequirement(40, 15, 25);
        assertFalse(min.isMinimumSatisfiedBy(new RitualStats(39, 20, 30)), "eterna below minimum");
        assertFalse(min.isMinimumSatisfiedBy(new RitualStats(50, 14, 30)), "quanta below minimum");
        assertFalse(min.isMinimumSatisfiedBy(new RitualStats(50, 20, 24)), "arcana below minimum");
    }

    private static void maximumSatisfied() {
        RitualStats stats = new RitualStats(50, 20, 30);
        RitualStatRequirement max = new RitualStatRequirement(60, 20, 35);
        assertTrue(max.isMaximumSatisfiedBy(stats), "maximum should be satisfied");
    }

    private static void maximumFailsForEachStat() {
        RitualStatRequirement max = new RitualStatRequirement(60, 20, 35);
        assertFalse(max.isMaximumSatisfiedBy(new RitualStats(61, 20, 30)), "eterna above maximum");
        assertFalse(max.isMaximumSatisfiedBy(new RitualStats(50, 21, 30)), "quanta above maximum");
        assertFalse(max.isMaximumSatisfiedBy(new RitualStats(50, 20, 36)), "arcana above maximum");
    }

    private static void combinedRangeSatisfied() {
        RitualStats stats = new RitualStats(50, 20, 30);
        RitualStatRequirement min = new RitualStatRequirement(40, 15, 25);
        RitualStatRequirement max = new RitualStatRequirement(60, 25, 35);
        assertTrue(RitualStatRequirement.matches(stats, min, Optional.of(max)), "in-range stats should satisfy");
    }

    private static void combinedRangeRejectedByMinimum() {
        RitualStats stats = new RitualStats(39, 20, 30);
        RitualStatRequirement min = new RitualStatRequirement(40, 15, 25);
        RitualStatRequirement max = new RitualStatRequirement(60, 25, 35);
        assertFalse(RitualStatRequirement.matches(stats, min, Optional.of(max)), "below minimum should be rejected");
    }

    private static void combinedRangeRejectedByMaximum() {
        RitualStats stats = new RitualStats(50, 26, 30);
        RitualStatRequirement min = new RitualStatRequirement(40, 15, 25);
        RitualStatRequirement max = new RitualStatRequirement(60, 25, 35);
        assertFalse(RitualStatRequirement.matches(stats, min, Optional.of(max)), "above maximum should be rejected");
    }

    private static void noneAlwaysSatisfied() {
        RitualStats negativeStats = new RitualStats(-100, -100, -100);
        assertTrue(
                RitualStatRequirement.matches(negativeStats, RitualStatRequirement.NONE, Optional.empty()),
                "NONE requirement should always be satisfied"
        );
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void assertFalse(boolean condition, String message) {
        assertTrue(!condition, message);
    }
}
