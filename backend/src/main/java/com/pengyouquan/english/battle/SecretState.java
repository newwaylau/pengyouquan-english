package com.pengyouquan.english.battle;

public class SecretState {
    private Long cardId;
    private String nameCn;
    private String triggerCondition; // "counter", "ice_barrier", "vaporize", "effigy", "noble_sacrifice"
    private boolean revealed;

    public SecretState() {}

    public SecretState(Long cardId, String nameCn, String triggerCondition) {
        this.cardId = cardId;
        this.nameCn = nameCn;
        this.triggerCondition = triggerCondition;
        this.revealed = false;
    }

    public Long getCardId() { return cardId; }
    public void setCardId(Long cardId) { this.cardId = cardId; }

    public String getNameCn() { return nameCn; }
    public void setNameCn(String nameCn) { this.nameCn = nameCn; }

    public String getTriggerCondition() { return triggerCondition; }
    public void setTriggerCondition(String triggerCondition) { this.triggerCondition = triggerCondition; }

    public boolean isRevealed() { return revealed; }
    public void setRevealed(boolean revealed) { this.revealed = revealed; }
}
