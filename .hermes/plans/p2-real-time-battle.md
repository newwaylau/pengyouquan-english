# P2-1 实时匹配对战系统 — 实现计划

> Claude Code 执行。git 分支：`sit`，所有改动基于当前 sit 分支。

## 架构概览

**通信方式**：WebSocket + STOMP 协议（Spring Boot 原生支持）
**游戏状态**：内存管理（每个对战一个 GameSession 对象），不需要持久化
**前端**：React + @stomp/stompjs 客户端
**已有可复用**：CardService.getCollection(), battle API, cardClient.ts

---

## Task 1: 后端 - WebSocket 配置

**文件**: `backend/src/main/java/com/pengyouquan/english/config/WebSocketConfig.java`

```java
package com.pengyouquan.english.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // 客户端订阅的前缀（接收服务器消息）
        config.enableSimpleBroker("/topic", "/queue");
        // 客户端发送消息的前缀
        config.setApplicationDestinationPrefixes("/app");
        // 点对点前缀
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // WebSocket 连接端点
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }
}
```

**Maven 依赖**：检查 `backend/pom.xml` 是否已有 `spring-boot-starter-websocket`。没有则添加：
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-websocket</artifactId>
</dependency>
```

还需要 `jackson-databind` （通常已包含在 spring-boot-starter-web 中）。

---

## Task 2: 后端 - 游戏状态模型

**文件**: `backend/src/main/java/com/pengyouquan/english/battle/GameSession.java`

```java
package com.pengyouquan.english.battle;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 一场实时对战的完整状态。
 * 存在内存中，数据库只存最终结果。
 */
public class GameSession {
    private String sessionId;          // UUID
    private Long player1Id;
    private Long player2Id;
    private PlayerState player1;
    private PlayerState player2;
    private int turnNumber;            // 当前回合数（从1开始）
    private Long currentPlayerId;      // 当前行动玩家
    private GamePhase phase;           // WAITING_MATCH / PLAYING / FINISHED
    private Long winnerId;
    private Long startTime;
    private Long lastActionTime;       // 最后行动时间戳（用于超时检测）

    // getters/setters
}

enum GamePhase {
    WAITING_MATCH, PLAYING, FINISHED
}

class PlayerState {
    private Long userId;
    private int health = 30;
    private int mana = 3;              // 当前可用费用
    private int maxMana = 3;           // 当前回合费用上限
    private List<CardState> hand;      // 手牌（最多8张）
    private List<CardState> board;     // 场上随从
    private List<CardState> deck;      // 牌库
    private int answeredCorrectly = 0; // 本局答对数
    private int answeredTotal = 0;     // 本局答题总数
    private int consecutiveCorrect = 0;// 连续正确数
    private int consecutiveWrong = 0;  // 连续错误数
    private boolean hasPlayedThisTurn = false; // 本回合是否已出牌
    private boolean hasAttackedThisTurn = false; // 本回合是否已攻击
}

class CardState {
    private Long cardId;
    private String nameCn;
    private String nameEn;
    private String cardType;           // minion/spell/equipment/location
    private String rarity;
    private int cost;
    private int attack;
    private int health;
    private int baseAttack;            // 原始攻击（用于计算buff后数值）
    private int baseHealth;            // 原始生命
    private String effectJson;
    private boolean canAttack;         // 本回合是否可以攻击
    private boolean hasTaunt;          // 嘲讽
}
```

**文件**: `backend/src/main/java/com/pengyouquan/english/battle/BattleMessage.java`

```java
package com.pengyouquan.english.battle;

import java.util.List;

/**
 * WebSocket 消息类型
 */
public class BattleMessage {
    private String type;    // MATCH_FOUND / GAME_START / TURN_START / QUESTION / ATTACK_RESULT / DEFENSE_QUESTION / DEFENSE_RESULT / GAME_OVER / ERROR
    private String sessionId;
    private Long playerId;
    private Object payload;

    // 各类型专用内部类
    @lombok.Data
    public static class MatchFound {
        public String sessionId;
        public Long opponentId;
        public String opponentName;
    }

    @lombok.Data
    public static class GameStart {
        public String sessionId;
        public PlayerInfo you;
        public PlayerInfo opponent;
        public List<CardState> hand;
        public int startingMana;
    }

    @lombok.Data
    public static class PlayerInfo {
        public Long userId;
        public String nickname;
        public int health;
        public int trophies;
    }

    @lombok.Data
    public static class TurnStart {
        public int turnNumber;
        public int mana;
        public int maxMana;
        public List<CardState> hand;
        public CardState drawnCard;
    }

    @lombok.Data
    public static class Question {
        public Long cardId;
        public String cardNameCn;
        public String questionType;    // listening / spelling / dictation / ordering
        public String questionData;    // JSON 包含题目内容
        public int timeLimit;          // 秒
    }

    @lombok.Data
    public static class CardPlayResult {
        public Long cardId;
        public boolean success;
        public String errorMessage;
        public CardState playedCard;
        public int manaRemaining;
    }

    @lombok.Data
    public static class AttackResult {
        public Long attackerId;
        public Long defenderId;        // 对手的卡牌ID或英雄（player）
        public int damage;
        public boolean defenderDead;
        public int defenderHealthLeft;
    }
}
```

---

## Task 3: 后端 - 匹配服务

**文件**: `backend/src/main/java/com/pengyouquan/english/battle/MatchmakingService.java`

```java
package com.pengyouquan.english.battle;

import org.springframework.stereotype.Service;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 匹配队列。
 * 玩家加入队列后，系统尝试匹配同段位玩家。
 */
@Service
public class MatchmakingService {
    
    // 等待队列：每个元素包含 userId 和 trophies
    private final ConcurrentLinkedQueue<MatchRequest> queue = new ConcurrentLinkedQueue<>();
    private final Map<Long, MatchRequest> pendingRequests = new ConcurrentHashMap<>();
    
    @Data
    @AllArgsConstructor
    public static class MatchRequest {
        private Long userId;
        private String nickname;
        private int trophies;
        private long joinTime;
    }
    
    /**
     * 加入匹配队列。
     * 返回：如果找到对手则返回 sessionId，否则返回 null（表示在队列中等待）
     */
    public synchronized MatchResult joinQueue(Long userId, String nickname, int trophies) {
        // 1. 尝试匹配已在队列中的对手（同段位 ±200 奖杯）
        // 2. 如果匹配到，创建 GameSession，从队列中移除两人，返回 MatchResult
        // 3. 如果没匹配到，加入队列，返回 null
    }
    
    /**
     * 主动取消匹配
     */
    public void cancelMatch(Long userId) {
        queue.removeIf(r -> r.userId.equals(userId));
        pendingRequests.remove(userId);
    }
}
```

**匹配算法**：
1. 新玩家加入队列
2. 遍历队列中已有的玩家，找奖杯差距 ≤200 且等待时间 ≤30秒的
3. 如果找到，创建 GameSession，通过 WebSocket 通知双方（`/queue/match-found`）
4. 如果没找到，加入队列末尾
5. 每5秒检查一次，对等待超过15秒的放宽匹配范围（±400）
6. 对等待超过30秒的放宽到±800

---

## Task 4: 后端 - 游戏引擎

**文件**: `backend/src/main/java/com/pengyouquan/english/battle/GameEngine.java`

```java
package com.pengyouquan.english.battle;

import org.springframework.stereotype.Service;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 对战的回合逻辑引擎。
 * 管理所有活跃的游戏会话。
 */
@Service
public class GameEngine {
    
    private final ConcurrentHashMap<String, GameSession> activeGames = new ConcurrentHashMap<>();
    private final CardRepository cardRepository;
    private final SentenceRepository sentenceRepository;
    
    /**
     * 创建新游戏会话
     */
    public GameSession createGame(Long player1Id, Long player2Id, 
                                   String player1Name, String player2Name,
                                   int trophies1, int trophies2) {
        GameSession session = new GameSession();
        session.setSessionId(UUID.randomUUID().toString());
        session.setPlayer1Id(player1Id);
        session.setPlayer2Id(player2Id);
        session.setTurnNumber(0);
        session.setPhase(GamePhase.PLAYING);
        session.setStartTime(System.currentTimeMillis());
        
        // 初始化双方牌组（从用户卡组随机抽30张）
        PlayerState p1 = initPlayer(player1Id);
        PlayerState p2 = initPlayer(player2Id);
        session.setPlayer1(p1);
        session.setPlayer2(p2);
        
        // 决定先手（随机）
        boolean p1First = new Random().nextBoolean();
        session.setCurrentPlayerId(p1First ? player1Id : player2Id);
        
        activeGames.put(session.getSessionId(), session);
        return session;
    }
    
    /**
     * 初始化玩家的牌组和手牌
     */
    private PlayerState initPlayer(Long userId) {
        // 1. 从用户收藏中获取传说/史诗/稀有/普通各类卡牌
        // 2. 从用户保存的卡组中获取30张卡
        // 3. 洗牌
        // 4. 抽3张起始手牌
        return playerState;
    }
    
    /**
     * 开始新回合
     */
    public TurnStartResult startTurn(String sessionId) {
        GameSession session = activeGames.get(sessionId);
        // 1. 回合数+1
        // 2. maxMana = min(turnNumber + 2, 10)
        // 3. mana = maxMana
        // 4. 抽1张牌
        // 5. 重置攻击状态
        // 返回当前回合信息
    }
    
    /**
     * 玩家出牌
     */
    public QuestionResult playCard(String sessionId, Long userId, Long cardId) {
        // 1. 验证是当前玩家的回合
        // 2. 验证手牌中有该卡
        // 3. 验证费用足够
        // 4. 根据卡牌费用生成对应难度的英语考题
        // 5. 返回考题给前端，等待玩家作答
    }
    
    /**
     * 玩家答题结果
     */
    public AnswerResult submitAnswer(String sessionId, Long userId, boolean correct) {
        // 答对：
        //   1. 卡牌上场（随从）/ 生效（法术）
        //   2. 费用扣除
        //   3. 检查连击（3连→下一张免费）
        // 答错：
        //   1. 卡牌回手
        //   2. 费用不退
        //   3. 对手抽1张+回2血
    }
    
    /**
     * 随从攻击
     */
    public AttackResult performAttack(String sessionId, Long userId, 
                                       Long attackerCardId, String targetType, Long targetId) {
        // 1. 验证攻击者属于当前玩家且可攻击
        // 2. 验证目标合法（有嘲讽必须先打嘲讽）
        // 3. 生成防御题给对手
    }
    
    /**
     * 防御题结果
     */
    public DefenseResult submitDefense(String sessionId, Long userId, boolean correct) {
        // 答对：伤害减半
        // 答错：全额伤害
        // 如果目标随从死亡，从场上移除
        // 如果英雄血量≤0，游戏结束
    }
    
    /**
     * 结束回合
     */
    public void endTurn(String sessionId, Long userId) {
        // 1. 验证是当前玩家
        // 2. 标记hasPlayedThisTurn = false
        // 3. 切换到对手回合
        // 4. 通知对手"你的回合开始了"
    }
    
    /**
     * 超时处理（60秒未操作）
     */
    public void handleTimeout(String sessionId, Long userId) {
        // 自动结束回合，对手本次攻击全中
    }
    
    /**
     * 游戏结束
     */
    public GameOverResult endGame(String sessionId, Long winnerId) {
        // 1. 记录胜者
        // 2. 更新奖杯
        // 3. 保存 battle_history
        // 4. 从活跃games中移除
        // 返回GameOver消息
    }
}
```

**生成考题的逻辑**：
```java
public Question generateQuestion(Long userId, Long cardId, int cost) {
    // 1. 从该卡的 challenge_sentence_id 获取句子
    // 2. 如果没有绑定句子，从该剧集已练句子中随机选一句
    // 3. 根据 cost 决定题型：
    //    cost 1-2: 听力选词（播放音频，4个选项选正确单词）
    //    cost 3-4: 拼写填空（显示中文，拼英文）
    //    cost 5-6: 句子听写（播句子，逐词填）
    //    cost 7-8: 排序（打乱单词排句子）
    //    cost 9-10: 段落理解（播一段，回答问题）
}
```

---

## Task 5: 后端 - WebSocket 消息处理

**文件**: `backend/src/main/java/com/pengyouquan/english/battle/BattleWebSocketHandler.java`

```java
package com.pengyouquan.english.battle;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class BattleWebSocketHandler {
    
    private final SimpMessagingTemplate messaging;
    private final MatchmakingService matchmaking;
    private final GameEngine gameEngine;
    
    // WebSocket 端点
    
    @MessageMapping("/battle/join-queue")
    public void joinQueue(@Payload JoinQueueRequest request, 
                          SimpMessageHeaderAccessor headerAccessor) {
        Long userId = getUserId(headerAccessor);
        // 加入匹配队列
        // 如果匹配到：发送 MATCH_FOUND 到 /queue/match-found
    }
    
    @MessageMapping("/battle/cancel-queue")
    public void cancelQueue(SimpMessageHeaderAccessor headerAccessor) {
        Long userId = getUserId(headerAccessor);
        matchmaking.cancelMatch(userId);
    }
    
    @MessageMapping("/battle/play-card")
    public void playCard(@Payload PlayCardRequest request,
                          SimpMessageHeaderAccessor headerAccessor) {
        // 出牌 → 返回考题
    }
    
    @MessageMapping("/battle/submit-answer")
    public void submitAnswer(@Payload AnswerSubmission submission,
                              SimpMessageHeaderAccessor headerAccessor) {
        // 提交答题结果 → 出牌成功/失败
    }
    
    @MessageMapping("/battle/attack")
    public void attack(@Payload AttackRequest request,
                        SimpMessageHeaderAccessor headerAccessor) {
        // 攻击 → 给对手发防御题
    }
    
    @MessageMapping("/battle/submit-defense")
    public void submitDefense(@Payload DefenseSubmission submission,
                               SimpMessageHeaderAccessor headerAccessor) {
        // 防御题结果 → 伤害计算
    }
    
    @MessageMapping("/battle/end-turn")
    public void endTurn(SimpMessageHeaderAccessor headerAccessor) {
        // 结束回合
    }
    
    @MessageMapping("/battle/concede")
    public void concede(SimpMessageHeaderAccessor headerAccessor) {
        // 认输
    }
    
    private Long getUserId(SimpMessageHeaderAccessor headerAccessor) {
        // 从 WebSocket 会话中提取用户ID
        // （连接时通过 JWT 验证并存入 session attributes）
    }
}
```

### JWT WebSocket 认证

**文件**: `backend/src/main/java/com/pengyouquan/english/config/WebSocketAuthInterceptor.java`

```java
package com.pengyouquan.english.config;

/**
 * 在 WebSocket 握手阶段验证 JWT Token。
 * 客户端连接时传入 token 参数：new WebSocket('/ws?token=xxx')
 */
public class WebSocketAuthInterceptor implements ChannelInterceptor {
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        // 从 STOMP CONNECT 帧中提取 token
        // 验证 JWT
        // 将 userId 存入 session
    }
}
```

---

## Task 6: 前端 - WebSocket 客户端

### 安装依赖

```bash
cd /home/heaton/pengyouquan-english/frontend
npm install @stomp/stompjs
npm install sockjs-client
npm install @types/sockjs-client --save-dev
```

**文件**: `frontend/src/api/battleWebSocket.ts`

```typescript
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

class BattleWebSocket {
  private client: Client;
  private token: string;
  private connected: boolean = false;

  constructor(token: string) {
    this.token = token;
    this.client = new Client({
      webSocketFactory: () => new SockJS(`/ws?token=${token}`),
      reconnectDelay: 5000,
      onConnect: () => {
        this.connected = true;
        console.log('WebSocket connected');
      },
      onDisconnect: () => {
        this.connected = false;
      },
    });
  }

  connect() { this.client.activate(); }
  disconnect() { this.client.deactivate(); }

  // 加入匹配队列
  joinQueue(callback: (msg: any) => void) {
    this.client.subscribe('/queue/match-found', (message) => {
      callback(JSON.parse(message.body));
    });
    this.client.publish({
      destination: '/app/battle/join-queue',
      body: JSON.stringify({}),
    });
  }

  // 订阅游戏事件
  subscribeToGame(sessionId: string, callbacks: {
    onGameStart: (data: any) => void;
    onTurnStart: (data: any) => void;
    onQuestion: (data: any) => void;
    onAttackResult: (data: any) => void;
    onDefenseQuestion: (data: any) => void;
    onGameOver: (data: any) => void;
    onError: (data: any) => void;
  }) {
    this.client.subscribe(`/topic/game/${sessionId}`, (message) => {
      const msg = JSON.parse(message.body);
      switch (msg.type) {
        case 'GAME_START': callbacks.onGameStart(msg.payload); break;
        case 'TURN_START': callbacks.onTurnStart(msg.payload); break;
        case 'QUESTION': callbacks.onQuestion(msg.payload); break;
        case 'ATTACK_RESULT': callbacks.onAttackResult(msg.payload); break;
        case 'DEFENSE_QUESTION': callbacks.onDefenseQuestion(msg.payload); break;
        case 'GAME_OVER': callbacks.onGameOver(msg.payload); break;
        case 'ERROR': callbacks.onError(msg.payload); break;
      }
    });
  }

  // 出牌
  playCard(cardId: number) {
    this.client.publish({
      destination: '/app/battle/play-card',
      body: JSON.stringify({ cardId }),
    });
  }

  // 提交答案
  submitAnswer(correct: boolean) {
    this.client.publish({
      destination: '/app/battle/submit-answer',
      body: JSON.stringify({ correct }),
    });
  }

  // 攻击
  attack(attackerCardId: number, targetType: 'minion' | 'hero', targetId?: number) {
    this.client.publish({
      destination: '/app/battle/attack',
      body: JSON.stringify({ attackerCardId, targetType, targetId }),
    });
  }

  // 防御
  submitDefense(correct: boolean) {
    this.client.publish({
      destination: '/app/battle/submit-defense',
      body: JSON.stringify({ correct }),
    });
  }

  // 结束回合
  endTurn() {
    this.client.publish({ destination: '/app/battle/end-turn', body: '{}' });
  }

  // 认输
  concede() {
    this.client.publish({ destination: '/app/battle/concede', body: '{}' });
  }
}

export default BattleWebSocket;
```

---

## Task 7: 前端 - 实时对战页面

**文件**: `frontend/src/BattleArenaPage.tsx`（新建，与BattlePage共存）

这是一个全新的全屏对战页面，包含：

### 状态管理

```typescript
interface BattleState {
  phase: 'idle' | 'matching' | 'playing' | 'finished';
  matchCountdown: number;        // 匹配等待秒数
  sessionId: string | null;
  
  // 己方信息
  myHealth: number;
  myMana: number;
  myMaxMana: number;
  myHand: CardState[];
  myBoard: CardState[];
  myDeckCount: number;
  
  // 对手信息
  opponentHealth: number;
  opponentName: string;
  opponentBoard: CardState[];
  opponentHandCount: number;
  opponentDeckCount: number;
  
  // 当前操作
  turnNumber: number;
  isMyTurn: boolean;
  timer: number;                 // 剩余秒数
  
  // 答题弹窗
  currentQuestion: Question | null;
  answerResult: 'waiting' | 'correct' | 'wrong' | null;
  
  // 对战结果
  result: 'win' | 'lose' | 'draw' | null;
  resultData: any;
}
```

### UI 布局（手机端竖屏）

```
┌─────────────────────┐
│  对手  ♥30  ✋3  🃏20  │ ← 对手信息栏
├─────────────────────┤
│  [随从] [随从]        │ ← 对手场上随从
│  [随从]              │
├─────────────────────┤
│  ⏱️ 回合3·你的回合 45秒│ ← 回合状态栏
├─────────────────────┤
│  [随从] [随从]        │ ← 己方场上随从
│  [随从]              │
├─────────────────────┤
│  ♥30  ⛽5/7  🃏4张    │ ← 己方状态栏
├─────────────────────┤
│ [卡] [卡] [卡] [卡] │ ← 手牌区
│ 点击出牌             │
├─────────────────────┤
│ [攻击] [结束回合]     │ ← 操作按钮
└─────────────────────┘
```

### 答题弹窗

当玩家出牌时，弹出一个覆盖整个屏幕的**答题面板**：

```
┌─────────────────────┐
│                      │
│  ⚔️ 冰原狼 · 出牌    │ ← 卡牌名称
│                      │
│  ┌─────────────┐    │
│  │             │    │
│  │  听音频选词  │    │ ← 题目区域
│  │  "winter"   │    │
│  │             │    │
│  │  ○ cold     │    │
│  │  ○ winter   │    │ ← 选项
│  │  ○ window   │    │
│  │  ○ winner   │    │
│  │             │    │
│  └─────────────┘    │
│                      │
│  ⏱️ 剩余 45 秒       │ ← 倒计时
│                      │
│  [选择答案]          │
└─────────────────────┘
```

### 按鈕狀態邏輯

- **攻击模式**：点击场上己方随从 → 进入攻击模式 → 点击对手随从/英雄头像 → 出发攻击 → 给对手发防御题
- **结束回合**：只有本回合出过牌或场上没有可行动随从才能结束
- **认输**：任何时候可用，弹确认框

### CSS 样式

使用现有项目的 CSS 变量（`--teal`, `--card`, `--bg`, `--radius-*` 等），保持视觉一致性。
对战页面需要新增的 CSS 写入 `frontend/src/index.css` 末尾。

---

## Task 8: 前端 - 集成到 App.tsx

1. 在底部导航"⚔️对战"增加**子导航**：点击后显示两个选项：
   - ⚡ 实时对战（新页面 BattleArenaPage）
   - 📡 异步挑战（现有 BattlePage）

或更简单：底部导航点击⚔️进入 BattlePage，内部增加"实时匹配"入口按钮。

2. `App.tsx` 增加 `page` 类型 `'battle-arena'`
3. 路由：`{page === 'battle-arena' && <BattleArenaPage user={user} onBack={() => setPage('battle')} />}`

---

## Task 9: 构建与部署

```bash
# 1. 安装前端依赖
cd /home/heaton/pengyouquan-english/frontend && npm install

# 2. 构建后端
cd /home/heaton/pengyouquan-english/backend && mvn package -DskipTests -q

# 3. 构建前端
cd /home/heaton/pengyouquan-english/frontend && npm run build

# 4. 部署后端
docker cp backend/target/*.jar pengyouquan-backend:/app/app.jar
docker restart pengyouquan-backend

# 5. 部署前端
docker cp frontend/dist/. pengyouquan-frontend:/usr/share/nginx/html/
docker exec pengyouquan-frontend chmod -R 755 /usr/share/nginx/html

# 6. 验证
sleep 15
curl -s https://english-sit.pengyouquan.top/api/cards | head -c 50
```

---

## 验收标准

1. ✅ 玩家 A 进入实时对战页面 → 点击"开始匹配" → 进入匹配队列
2. ✅ 玩家 B 也进入实时对战 → 匹配到对方 → 双方收到 MATCH_FOUND
3. ✅ 对战开始：双方看到起始手牌3张、费用3/3、血量30
4. ✅ 出牌流程：点击手牌 → 弹出考题 → 答对卡上场 / 答错牌回手
5. ✅ 攻击流程：点击场上随从 → 选择目标 → 对手答防御题 → 伤害结算
6. ✅ 回合切换：结束回合按钮 → 轮到对手 → 刷新费用和手牌
7. ✅ 游戏结束：一方血量归零 → 显示胜负 → 奖杯更新
8. ✅ 完整的 BattleArenaPage UI 手机端适配
