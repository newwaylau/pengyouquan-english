package com.pengyouquan.english.battle;

import java.util.List;

/**
 * WebSocket 消息类型（纯卡牌策略对战）
 */
public class BattleMessage {

    private String type;
    private String sessionId;
    private Long playerId;
    private Object payload;

    public BattleMessage() {}

    public BattleMessage(String type, String sessionId, Long playerId, Object payload) {
        this.type = type;
        this.sessionId = sessionId;
        this.playerId = playerId;
        this.payload = payload;
    }

    // --- getters/setters ---

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public Long getPlayerId() { return playerId; }
    public void setPlayerId(Long playerId) { this.playerId = playerId; }

    public Object getPayload() { return payload; }
    public void setPayload(Object payload) { this.payload = payload; }

    // ====== 消息类型常量 ======

    public static final String TYPE_MATCH_FOUND = "MATCH_FOUND";
    public static final String TYPE_GAME_START = "GAME_START";
    public static final String TYPE_QUEUE_STATUS = "QUEUE_STATUS";
    public static final String TYPE_TURN_START = "TURN_START";
    public static final String TYPE_CARD_PLAY_RESULT = "CARD_PLAY_RESULT";
    public static final String TYPE_ATTACK_DECLARED = "ATTACK_DECLARED";
    public static final String TYPE_ATTACK_RESULT = "ATTACK_RESULT";
    public static final String TYPE_GAME_OVER = "GAME_OVER";
    public static final String TYPE_ERROR = "ERROR";
    public static final String TYPE_OPPONENT_DISCONNECTED = "OPPONENT_DISCONNECTED";
    public static final String TYPE_MULLIGAN_START = "MULLIGAN_START";

    // ====== Payload 类型 ======

    public static class MatchFound {
        public String sessionId;
        public Long opponentId;
        public String opponentName;
        public int opponentTrophies;

        public MatchFound() {}
        public MatchFound(String sessionId, Long opponentId, String opponentName, int opponentTrophies) {
            this.sessionId = sessionId;
            this.opponentId = opponentId;
            this.opponentName = opponentName;
            this.opponentTrophies = opponentTrophies;
        }
    }

    public static class QueueStatus {
        public int queuePosition;
        public int estimatedWaitSeconds;
        public QueueStatus() {}
        public QueueStatus(int queuePosition, int estimatedWaitSeconds) {
            this.queuePosition = queuePosition;
            this.estimatedWaitSeconds = estimatedWaitSeconds;
        }
    }

    public static class GameStart {
        public String sessionId;
        public PlayerInfo you;
        public PlayerInfo opponent;
        public List<CardState> hand;
        public int startingMana;
        public boolean goingFirst;

        public GameStart() {}
    }

    public static class PlayerInfo {
        public Long userId;
        public String nickname;
        public int health;
        public int trophies;
        public int deckCount;

        public PlayerInfo() {}
        public PlayerInfo(Long userId, String nickname, int health, int trophies, int deckCount) {
            this.userId = userId;
            this.nickname = nickname;
            this.health = health;
            this.trophies = trophies;
            this.deckCount = deckCount;
        }
    }

    public static class TurnStart {
        public int turnNumber;
        public int mana;
        public int maxMana;
        public List<CardState> hand;
        public CardState drawnCard;

        public TurnStart() {}
    }

    public static class CardPlayResult {
        public Long cardId;
        public boolean success;
        public String errorMessage;
        public CardState playedCard;
        public int manaRemaining;
        public int damageDealt;

        public CardPlayResult() {}
    }

    public static class AttackDeclared {
        public Long attackerId;
        public String attackerName;
        public int attackerAttack;
        public Long targetId;
        public String targetType;

        public AttackDeclared() {}
    }

    public static class AttackResult {
        public Long attackerId;
        public Long defenderId;
        public int damage;
        public boolean defenderDead;
        public int defenderHealthLeft;

        public AttackResult() {}
    }

    public static class GameOver {
        public Long winnerId;
        public Long loserId;
        public String result;
        public int trophyChange;
        public PlayerFinalStats winnerStats;
        public PlayerFinalStats loserStats;

        public GameOver() {}
    }

    public static class PlayerFinalStats {
        public Long userId;
        public String nickname;
        public int healthRemaining;
        public int cardsPlayed;
        public int accuracy;
        public int trophiesAfter;

        public PlayerFinalStats() {}
    }

    public static class ErrorMessage {
        public String code;
        public String message;
        public ErrorMessage() {}
        public ErrorMessage(String code, String message) {
            this.code = code;
            this.message = message;
        }
    }

    public static class MulliganStart {
        public List<CardState> hand;
        public boolean goingFirst;

        public MulliganStart() {}
    }
}
