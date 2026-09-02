package com.chuan.apothicflux.util;

public final class ExpConverterTopScale {
    public static final long MAX_SCALE_XP = 50_000L;
    public static final float MIN_SCALE = 0.1F;
    public static final float MAX_SCALE = 1.6F;

    private ExpConverterTopScale() {
    }

    public static float scaleForStoredXp(long storedXp) {
        float progress = Math.min(1.0F, Math.max(0.0F, storedXp / (float) MAX_SCALE_XP));
        return MIN_SCALE + (MAX_SCALE - MIN_SCALE) * progress;
    }
}
