package com.pengyouquan.english.battle;

import java.util.List;

public class CardState {
    private Long cardId;
    private String nameCn;
    private String nameEn;
    private String cardType;
    private String rarity;
    private int cost;
    private int attack;
    private int health;
    private int baseAttack;
    private int baseHealth;
    private String effectJson;
    private String keywords; // JSON array string like '["taunt","battlecry"]'
    private boolean canAttack;
    private boolean hasTaunt;
    private boolean hasDivineShield;
    private boolean hasDeathrattle;
    private boolean hasBattlecry;
    private boolean hasStealth;
    private boolean hasRush;
    private boolean battlecryTriggered;
    private boolean stealthRevealed;
    private Long challengeSentenceId;

    public CardState() {}

    public CardState(Long cardId, String nameCn, String nameEn, String cardType, String rarity,
                     int cost, int attack, int health, String effectJson, Long challengeSentenceId) {
        this.cardId = cardId;
        this.nameCn = nameCn;
        this.nameEn = nameEn;
        this.cardType = cardType;
        this.rarity = rarity;
        this.cost = cost;
        this.attack = attack;
        this.health = health;
        this.baseAttack = attack;
        this.baseHealth = health;
        this.effectJson = effectJson;
        this.canAttack = false;
        this.hasTaunt = false;
        this.hasDivineShield = false;
        this.hasDeathrattle = false;
        this.hasBattlecry = false;
        this.hasStealth = false;
        this.hasRush = false;
        this.battlecryTriggered = false;
        this.stealthRevealed = false;
        this.challengeSentenceId = challengeSentenceId;
    }

    // --- getters/setters ---

    public Long getCardId() { return cardId; }
    public void setCardId(Long cardId) { this.cardId = cardId; }

    public String getNameCn() { return nameCn; }
    public void setNameCn(String nameCn) { this.nameCn = nameCn; }

    public String getNameEn() { return nameEn; }
    public void setNameEn(String nameEn) { this.nameEn = nameEn; }

    public String getCardType() { return cardType; }
    public void setCardType(String cardType) { this.cardType = cardType; }

    public String getRarity() { return rarity; }
    public void setRarity(String rarity) { this.rarity = rarity; }

    public int getCost() { return cost; }
    public void setCost(int cost) { this.cost = cost; }

    public int getAttack() { return attack; }
    public void setAttack(int attack) { this.attack = attack; }

    public int getHealth() { return health; }
    public void setHealth(int health) { this.health = health; }

    public int getBaseAttack() { return baseAttack; }
    public void setBaseAttack(int baseAttack) { this.baseAttack = baseAttack; }

    public int getBaseHealth() { return baseHealth; }
    public void setBaseHealth(int baseHealth) { this.baseHealth = baseHealth; }

    public String getEffectJson() { return effectJson; }
    public void setEffectJson(String effectJson) { this.effectJson = effectJson; }

    public String getKeywords() { return keywords; }
    public void setKeywords(String keywords) { this.keywords = keywords; }

    public boolean isCanAttack() { return canAttack; }
    public void setCanAttack(boolean canAttack) { this.canAttack = canAttack; }

    public boolean isHasTaunt() { return hasTaunt; }
    public void setHasTaunt(boolean hasTaunt) { this.hasTaunt = hasTaunt; }

    public boolean isHasDivineShield() { return hasDivineShield; }
    public void setHasDivineShield(boolean hasDivineShield) { this.hasDivineShield = hasDivineShield; }

    public boolean isHasDeathrattle() { return hasDeathrattle; }
    public void setHasDeathrattle(boolean hasDeathrattle) { this.hasDeathrattle = hasDeathrattle; }

    public boolean isHasBattlecry() { return hasBattlecry; }
    public void setHasBattlecry(boolean hasBattlecry) { this.hasBattlecry = hasBattlecry; }

    public boolean isHasStealth() { return hasStealth; }
    public void setHasStealth(boolean hasStealth) { this.hasStealth = hasStealth; }

    public boolean isHasRush() { return hasRush; }
    public void setHasRush(boolean hasRush) { this.hasRush = hasRush; }

    public boolean isBattlecryTriggered() { return battlecryTriggered; }
    public void setBattlecryTriggered(boolean battlecryTriggered) { this.battlecryTriggered = battlecryTriggered; }

    public boolean isStealthRevealed() { return stealthRevealed; }
    public void setStealthRevealed(boolean stealthRevealed) { this.stealthRevealed = stealthRevealed; }

    public Long getChallengeSentenceId() { return challengeSentenceId; }
    public void setChallengeSentenceId(Long challengeSentenceId) { this.challengeSentenceId = challengeSentenceId; }
}
