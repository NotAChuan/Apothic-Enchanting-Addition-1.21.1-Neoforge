package com.chuan.apothicflux.util;

/** Client-side timing state for the converter top intro. */
public final class ExpConverterAnimationState {
    public static final int INTRO_LENGTH = 59;

    private int time;
    private boolean complete;

    public void tick() {
        if (complete) {
            return;
        }

        time++;
        if (time >= INTRO_LENGTH) {
            complete = true;
            time = 0;
        }
    }

    public int time() {
        return time;
    }

    public float progress(float partialTick) {
        return complete ? 1.0F : Math.min(1.0F, (time + partialTick) / INTRO_LENGTH);
    }

    public boolean isIntro() {
        return !complete;
    }

    public boolean isComplete() {
        return complete;
    }

    public void setComplete(boolean complete) {
        this.complete = complete;
        if (complete) {
            this.time = 0;
        }
    }
}
