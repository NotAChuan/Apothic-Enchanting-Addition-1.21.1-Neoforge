package com.chuan.apothicenchantingaddition.util;

public final class BookAnimationHelper {

    private BookAnimationHelper() {
    }

    public static float nextOpen(float currentOpen, boolean hasNearbyPlayer) {
        return clamp(currentOpen + (hasNearbyPlayer ? 0.1F : -0.1F), 0.0F, 1.0F);
    }

    public static float targetRotation(double tableX, double tableZ, double playerX, double playerZ) {
        return (float) Math.atan2(playerZ - tableZ, playerX - tableX);
    }

    public static float nextRotation(float currentRotation, float targetRotation) {
        float current = wrapRadians(currentRotation);
        float target = wrapRadians(targetRotation);
        float delta = wrapRadians(target - current);
        return current + delta * 0.4F;
    }

    public static float rotationDelta(float previousRotation, float currentRotation) {
        return wrapRadians(currentRotation - previousRotation);
    }

    private static float wrapRadians(float angle) {
        while (angle >= (float) Math.PI) {
            angle -= (float) (Math.PI * 2);
        }

        while (angle < (float) -Math.PI) {
            angle += (float) (Math.PI * 2);
        }

        return angle;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
