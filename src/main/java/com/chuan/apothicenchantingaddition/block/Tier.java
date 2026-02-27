package com.chuan.apothicenchantingaddition.block;

public enum Tier {
    TIER_1(25.0F, 25.0F, 25.0F, 15),
    TIER_2(50.0F, 50.0F, 50.0F, 15),
    TIER_3(75.0F, 75.0F, 75.0F, 15),
    TIER_4(100.0F, 100.0F, 100.0F, 15);

    private final float maxEterna;
    private final float maxQuanta;
    private final float maxArcana;
    private final int maxClues;

    Tier(float maxEterna, float maxQuanta, float maxArcana, int maxClues) {
        this.maxEterna = maxEterna;
        this.maxQuanta = maxQuanta;
        this.maxArcana = maxArcana;
        this.maxClues = maxClues;
    }

    public float getMaxEterna() { return maxEterna; }
    public float getMaxQuanta() { return maxQuanta; }
    public float getMaxArcana() { return maxArcana; }
    public int getMaxClues() { return maxClues; }
}
