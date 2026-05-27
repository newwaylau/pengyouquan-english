package com.pengyouquan.english.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;

@Data
@AllArgsConstructor
public class CardResponse {
    private Long id;
    private Long showId;
    private String showName;
    private String nameCn;
    private String nameEn;
    private String cardType;
    private String rarity;
    private Integer cost;
    private Integer attack;
    private Integer health;
    private String effectJson;
    private String keywords;
    private String challengeType;
    private String faction;
    private String race;
    private String element;
    private String quoteText;
    private int quantity;
    private boolean hasGolden;
    private boolean isGolden;
    /** 金卡带来的额外攻击力（普通→金卡的提升） */
    private int goldenAttackBonus;
    /** 金卡带来的额外生命值（普通→金卡的提升） */
    private int goldenHealthBonus;

    public static CardResponse fromCard(com.pengyouquan.english.model.Card card, int quantity, String showName) {
        CardResponse r = new CardResponse(
            card.getId(),
            card.getShowId(),
            showName,
            card.getNameCn(),
            card.getNameEn(),
            card.getCardType(),
            card.getRarity(),
            card.getCost(),
            card.getAttack(),
            card.getHealth(),
            card.getEffectJson(),
            card.getKeywords(),
            card.getChallengeType(),
            card.getFaction(),
            card.getRace(),
            card.getElement(),
            card.getQuoteText(),
            quantity,
            Boolean.TRUE.equals(card.getHasGolden()),
            false,
            0, 0
        );
        return r;
    }

    public static CardResponse fromCardWithGolden(com.pengyouquan.english.model.Card card, int quantity, String showName, boolean isGolden) {
        CardResponse r = fromCard(card, quantity, showName);
        r.setGolden(isGolden);
        if (isGolden) {
            r.setGoldenAttackBonus(1);
            r.setGoldenHealthBonus(1);
            // 金卡：攻击+1，生命+1（如果是法术则攻击+1伤害）
            r.setAttack((r.getAttack() != null ? r.getAttack() : 0) + 1);
            r.setHealth((r.getHealth() != null ? r.getHealth() : 0) + 1);
        }
        return r;
    }

    /**
     * 获取有效攻击力（考虑了金卡加成）
     */
    public int getEffectiveAttack() {
        int base = attack != null ? attack : 0;
        if (isGolden) return base + 1;
        return base;
    }

    /**
     * 获取有效生命值（考虑了金卡加成）
     */
    public int getEffectiveHealth() {
        int base = health != null ? health : 0;
        if (isGolden) return base + 1;
        return base;
    }
}
