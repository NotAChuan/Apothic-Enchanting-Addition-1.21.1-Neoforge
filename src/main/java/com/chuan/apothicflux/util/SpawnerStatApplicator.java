package com.chuan.apothicflux.util;

public final class SpawnerStatApplicator {

    public enum Mode {
        ADD,
        SET
    }

    private SpawnerStatApplicator() {
    }

    public static int applyLowerBoundStat(int current, int value, Mode mode, int lowerBound, int hardUpperBound) {
        if (mode == Mode.SET) {
            return value;
        }

        if (value < 0) {
            if (current <= lowerBound) {
                return current;
            }
            return Math.max(lowerBound, current + value);
        }
        if (value > 0) {
            if (current >= hardUpperBound) {
                return current;
            }
            return Math.min(hardUpperBound, current + value);
        }
        return current;
    }

    public static int applyUpperBoundStat(int current, int value, Mode mode, int hardLowerBound, int upperBound) {
        if (mode == Mode.SET) {
            return value;
        }

        if (value > 0) {
            if (current >= upperBound) {
                return current;
            }
            return Math.min(upperBound, current + value);
        }
        if (value < 0) {
            if (current <= hardLowerBound) {
                return current;
            }
            return Math.max(hardLowerBound, current + value);
        }
        return current;
    }
}
