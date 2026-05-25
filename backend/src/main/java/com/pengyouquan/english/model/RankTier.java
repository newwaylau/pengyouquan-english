package com.pengyouquan.english.model;

import lombok.Getter;

@Getter
public enum RankTier {
    FREEFOLK(1, "自由民", "Free Folk", 0),
    SWORN_BROTHER(2, "守夜人", "Sworn Brother", 100),
    LANDED_KNIGHT(3, "骑士", "Landed Knight", 300),
    LORD(4, "领主", "Lord", 600),
    WARLORD(5, "warlord / 军阀", "Warlord", 1000),
    PRINCE(6, "王子", "Prince", 1500),
    KING(7, "国王", "King", 2200),
    HAND_OF_KING(8, "国王之手", "Hand of the King", 3000),
    THREE_DRAGONS(9, "龙之母", "Mother of Dragons", 4000),
    IRON_THRONE(10, "铁王座", "Iron Throne", 5500);

    private final int tier;
    private final String titleCn;
    private final String titleEn;
    private final int requiredPrestige;

    RankTier(int tier, String titleCn, String titleEn, int requiredPrestige) {
        this.tier = tier;
        this.titleCn = titleCn;
        this.titleEn = titleEn;
        this.requiredPrestige = requiredPrestige;
    }

    public static RankTier fromPrestige(int prestige) {
        RankTier result = FREEFOLK;
        for (RankTier r : values()) {
            if (prestige >= r.getRequiredPrestige()) result = r;
        }
        return result;
    }

    public static RankTier fromTier(int tier) {
        for (RankTier r : values()) {
            if (r.getTier() == tier) return r;
        }
        return FREEFOLK;
    }

    public int getNextRequiredPrestige() {
        for (RankTier r : values()) {
            if (r.getTier() == this.tier + 1) return r.getRequiredPrestige();
        }
        return this.requiredPrestige;
    }

    public int getRangeSize() {
        int next = getNextRequiredPrestige();
        return next - this.requiredPrestige;
    }

    public int getProgress(int prestige) {
        int range = getRangeSize();
        if (range <= 0) return 100;
        int within = prestige - this.requiredPrestige;
        return Math.min(100, within * 100 / range);
    }
}
