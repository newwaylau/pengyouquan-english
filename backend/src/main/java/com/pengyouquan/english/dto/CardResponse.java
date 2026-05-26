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
    private String challengeType;
    private String faction;
    private String quoteText;
    private int quantity;

    public static CardResponse fromCard(com.pengyouquan.english.model.Card card, int quantity, String showName) {
        return new CardResponse(
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
            card.getChallengeType(),
            card.getFaction(),
            card.getQuoteText(),
            quantity
        );
    }
}
