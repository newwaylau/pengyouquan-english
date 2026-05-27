package com.pengyouquan.english.battle;

public class WeaponState {
    private Long cardId;
    private String nameCn;
    private int attack;
    private int durability;
    private int maxDurability;

    public WeaponState() {}

    public WeaponState(Long cardId, String nameCn, int attack, int durability) {
        this.cardId = cardId;
        this.nameCn = nameCn;
        this.attack = attack;
        this.durability = durability;
        this.maxDurability = durability;
    }

    public Long getCardId() { return cardId; }
    public void setCardId(Long cardId) { this.cardId = cardId; }

    public String getNameCn() { return nameCn; }
    public void setNameCn(String nameCn) { this.nameCn = nameCn; }

    public int getAttack() { return attack; }
    public void setAttack(int attack) { this.attack = attack; }

    public int getDurability() { return durability; }
    public void setDurability(int durability) { this.durability = durability; }

    public int getMaxDurability() { return maxDurability; }
    public void setMaxDurability(int maxDurability) { this.maxDurability = maxDurability; }
}
