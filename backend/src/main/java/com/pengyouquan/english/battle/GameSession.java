package com.pengyouquan.english.battle;

import java.util.List;

/**
 * 一场实时对战的完整状态。
 * 存在内存中，数据库只存最终结果。
 */
public class GameSession {
    private String sessionId;
    private Long player1Id;
    private Long player2Id;
    private PlayerState player1;
    private PlayerState player2;
    private int turnNumber;
    private Long currentPlayerId;
    private GamePhase phase;
    private Long winnerId;
    private Long startTime;
    private Long lastActionTime;

    public GameSession() {}

    // --- getters/setters ---

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public Long getPlayer1Id() { return player1Id; }
    public void setPlayer1Id(Long player1Id) { this.player1Id = player1Id; }

    public Long getPlayer2Id() { return player2Id; }
    public void setPlayer2Id(Long player2Id) { this.player2Id = player2Id; }

    public PlayerState getPlayer1() { return player1; }
    public void setPlayer1(PlayerState player1) { this.player1 = player1; }

    public PlayerState getPlayer2() { return player2; }
    public void setPlayer2(PlayerState player2) { this.player2 = player2; }

    public int getTurnNumber() { return turnNumber; }
    public void setTurnNumber(int turnNumber) { this.turnNumber = turnNumber; }

    public Long getCurrentPlayerId() { return currentPlayerId; }
    public void setCurrentPlayerId(Long currentPlayerId) { this.currentPlayerId = currentPlayerId; }

    public GamePhase getPhase() { return phase; }
    public void setPhase(GamePhase phase) { this.phase = phase; }

    public Long getWinnerId() { return winnerId; }
    public void setWinnerId(Long winnerId) { this.winnerId = winnerId; }

    public Long getStartTime() { return startTime; }
    public void setStartTime(Long startTime) { this.startTime = startTime; }

    public Long getLastActionTime() { return lastActionTime; }
    public void setLastActionTime(Long lastActionTime) { this.lastActionTime = lastActionTime; }

    /** 获取指定玩家的状态 */
    public PlayerState getPlayerState(Long userId) {
        if (player1 != null && player1.getUserId().equals(userId)) return player1;
        if (player2 != null && player2.getUserId().equals(userId)) return player2;
        return null;
    }

    /** 获取对手 */
    public PlayerState getOpponent(Long userId) {
        if (player1 != null && player1.getUserId().equals(userId)) return player2;
        if (player2 != null && player2.getUserId().equals(userId)) return player1;
        return null;
    }

    /** 获取对手的用户ID */
    public Long getOpponentId(Long userId) {
        if (player1Id.equals(userId)) return player2Id;
        return player1Id;
    }
}

enum GamePhase {
    WAITING_MATCH, PLAYING, FINISHED
}
