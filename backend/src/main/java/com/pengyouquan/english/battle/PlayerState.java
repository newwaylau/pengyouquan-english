package com.pengyouquan.english.battle;

import java.util.List;

public class PlayerState {
    private Long userId;
    private String nickname;
    private int health = 30;
    private int mana = 1;
    private int maxMana = 1;
    private List<CardState> hand;
    private List<CardState> board;
    private List<CardState> deck;
    private int answeredCorrectly = 0;
    private int answeredTotal = 0;
    private int consecutiveCorrect = 0;
    private int consecutiveWrong = 0;
    private boolean goingFirst = false;
    private boolean hasPlayedThisTurn = false;
    private boolean hasAttackedThisTurn = false;
    private int trophies;

    // --- getters/setters ---

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }

    public int getHealth() { return health; }
    public void setHealth(int health) { this.health = health; }

    public int getMana() { return mana; }
    public void setMana(int mana) { this.mana = mana; }

    public int getMaxMana() { return maxMana; }
    public void setMaxMana(int maxMana) { this.maxMana = maxMana; }

    public List<CardState> getHand() { return hand; }
    public void setHand(List<CardState> hand) { this.hand = hand; }

    public List<CardState> getBoard() { return board; }
    public void setBoard(List<CardState> board) { this.board = board; }

    public List<CardState> getDeck() { return deck; }
    public void setDeck(List<CardState> deck) { this.deck = deck; }

    public int getAnsweredCorrectly() { return answeredCorrectly; }
    public void setAnsweredCorrectly(int answeredCorrectly) { this.answeredCorrectly = answeredCorrectly; }

    public int getAnsweredTotal() { return answeredTotal; }
    public void setAnsweredTotal(int answeredTotal) { this.answeredTotal = answeredTotal; }

    public int getConsecutiveCorrect() { return consecutiveCorrect; }
    public void setConsecutiveCorrect(int consecutiveCorrect) { this.consecutiveCorrect = consecutiveCorrect; }

    public int getConsecutiveWrong() { return consecutiveWrong; }
    public void setConsecutiveWrong(int consecutiveWrong) { this.consecutiveWrong = consecutiveWrong; }

    public boolean isGoingFirst() { return goingFirst; }
    public void setGoingFirst(boolean goingFirst) { this.goingFirst = goingFirst; }

    public boolean isHasPlayedThisTurn() { return hasPlayedThisTurn; }
    public void setHasPlayedThisTurn(boolean hasPlayedThisTurn) { this.hasPlayedThisTurn = hasPlayedThisTurn; }

    public boolean isHasAttackedThisTurn() { return hasAttackedThisTurn; }
    public void setHasAttackedThisTurn(boolean hasAttackedThisTurn) { this.hasAttackedThisTurn = hasAttackedThisTurn; }

    public int getTrophies() { return trophies; }
    public void setTrophies(int trophies) { this.trophies = trophies; }
}
