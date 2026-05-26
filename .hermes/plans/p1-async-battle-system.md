# P1 异步竞技场 — 实现计划

> Claude Code 执行。git 分支：`sit`，所有改动基于 sit 分支。

## 项目架构说明

**后端**：Spring Boot 3 + JPA + MySQL + Flyway
**前端**：React (TypeScript) + CSS Variables + 无状态函数组件
**数据库**：MySQL 8.0，Flyway 迁移管理
**部署**：Docker Compose（手动 docker cp + restart）

**关键工具类**：
- `@CurrentUserId` → 从JWT提取用户ID（`Long userId`）
- `ApiResponse<T>` → 统一响应格式 `{code, message, data}`，`success(data)`、`error(code, msg)`
- `CardService` → 已有，`grantCardPack()` 可复用
- 已有前端API调用模式：`request(path, options)` + getToken() + Authorization header

**项目根目录**：`/home/heaton/pengyouquan-english`

---

## Task 1: 数据库迁移 - V19 新增三张表

**文件**: `backend/src/main/resources/db/migration/V19__add_pvp_tables.sql`

```sql
CREATE TABLE IF NOT EXISTS friends (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  friend_id BIGINT NOT NULL,
  status ENUM('pending','accepted','blocked') NOT NULL DEFAULT 'pending',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_friendship (user_id, friend_id),
  FOREIGN KEY (user_id) REFERENCES users(id),
  FOREIGN KEY (friend_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS battle_history (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  challenger_id BIGINT NOT NULL,
  defender_id BIGINT NOT NULL,
  winner_id BIGINT DEFAULT NULL,
  challenger_deck_id BIGINT DEFAULT NULL,
  defender_deck_id BIGINT DEFAULT NULL,
  challenger_score INT DEFAULT 0 COMMENT '答对数',
  defender_score INT DEFAULT 0,
  challenger_accuracy DECIMAL(5,2) DEFAULT 0.00,
  defender_accuracy DECIMAL(5,2) DEFAULT 0.00,
  challenger_avg_difficulty DECIMAL(4,2) DEFAULT 0.00,
  defender_avg_difficulty DECIMAL(4,2) DEFAULT 0.00,
  trophy_change INT DEFAULT 0,
  status ENUM('pending','completed','cancelled') NOT NULL DEFAULT 'pending',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  completed_at DATETIME DEFAULT NULL,
  INDEX idx_challenger (challenger_id),
  INDEX idx_defender (defender_id),
  FOREIGN KEY (challenger_id) REFERENCES users(id),
  FOREIGN KEY (defender_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS rank_tiers (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name_cn VARCHAR(50) NOT NULL,
  name_en VARCHAR(50) NOT NULL,
  min_trophies INT NOT NULL,
  max_trophies INT NOT NULL,
  icon VARCHAR(10) DEFAULT '',
  season_reward_type VARCHAR(20) DEFAULT NULL,
  season_reward_count INT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 插入段位数据
INSERT INTO rank_tiers (name_cn, name_en, min_trophies, max_trophies, icon, season_reward_type, season_reward_count) VALUES
('青铜', 'Bronze', 0, 200, '🥉', 'common', 3),
('白银', 'Silver', 201, 500, '🥈', 'rare', 3),
('黄金', 'Gold', 501, 900, '🥇', 'epic', 1),
('白金', 'Platinum', 901, 1500, '💎', 'epic', 3),
('钻石', 'Diamond', 1501, 2500, '🔷', 'legendary', 1),
('传说', 'Legend', 2501, 99999, '🏆', 'legendary', 3);
```

**验证**: `docker exec pengyouquan-mysql mysql -uroot -ppengyouquan123 -e "SHOW TABLES;" pengyouquan_english` 能看到 friends、battle_history、rank_tiers 三张表。

---

## Task 2: 后端 - Friend 实体 + Repository

**文件**: `backend/src/main/java/com/pengyouquan/english/model/Friend.java`

```java
package com.pengyouquan.english.model;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data @Entity @Table(name = "friends")
public class Friend {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_id", nullable = false)
    private Long userId;
    @Column(name = "friend_id", nullable = false)
    private Long friendId;
    @Column(nullable = false, length = 20)
    private String status = "pending"; // pending / accepted / blocked
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); updatedAt = createdAt; }
    @PreUpdate
    protected void onUpdate() { updatedAt = LocalDateTime.now(); }
}
```

**文件**: `backend/src/main/java/com/pengyouquan/english/model/BattleHistory.java`
- 字段对应 battle_history 表：id, challengerId, defenderId, winnerId, challengerDeckId, defenderDeckId, challengerScore, defenderScore, challengerAccuracy, defenderAccuracy, challengerAvgDifficulty, defenderAvgDifficulty, trophyChange, status, createdAt, completedAt

**文件**: `backend/src/main/java/com/pengyouquan/english/model/RankTier.java`
- 字段对应 rank_tiers 表

**文件**: `backend/src/main/java/com/pengyouquan/english/repository/FriendRepository.java`
- `List<Friend> findByUserId(Long userId)`
- `List<Friend> findByFriendIdAndStatus(Long friendId, String status)`
- `Optional<Friend> findByUserIdAndFriendId(Long userId, Long friendId)`

**文件**: `backend/src/main/java/com/pengyouquan/english/repository/BattleHistoryRepository.java`
- `List<BattleHistory> findByChallengerIdOrDefenderIdOrderByCreatedAtDesc(Long userId1, Long userId2)`
- `Optional<BattleHistory> findByIdAndStatus(Long id, String status)`

**文件**: `backend/src/main/java/com/pengyouquan/english/repository/RankTierRepository.java`
- `Optional<RankTier> findByMinTrophiesLessThanEqualAndMaxTrophiesGreaterThanEqual(int trophies, int trophies2)`
- 按 min_trophies 排序: `List<RankTier> findAllByOrderByMinTrophiesAsc()`

---

## Task 3: 后端 DTO

**文件**: `FriendDTO.java`
```java
package com.pengyouquan.english.dto;
// fields: id, friendId, friendNickname, friendEmail, status, createdAt
```

**文件**: `BattleHistoryDTO.java`
```java
// fields: id, challengerId, challengerNickname, defenderId, defenderNickname, winnerId,
//         challengerScore, defenderScore, challengerAccuracy, defenderAccuracy,
//         trophyChange, status, createdAt, completedAt
```

**文件**: `RankInfoDTO.java`
```java
// fields: trophies, tierName, tierIcon, rank, seasonRewardType, seasonRewardCount, winStreak
```

**文件**: `ChallengeRequest.java`
```java
// fields: defenderId, defenderDeckId (防守方选定的卡组)
```

**文件**: `BattleResultRequest.java`
```java
// fields: battleId, score, accuracy, avgDifficulty, cardsPlayed (答题数量)
```

---

## Task 4: 后端 Service

### BattleService.java
**路径**: `backend/src/main/java/com/pengyouquan/english/service/BattleService.java`

核心方法：
1. `challengePlayer(Long challengerId, Long defenderId, Long deckId)` — 发起挑战
   - 验证双方存在
   - 验证 defender 不是自己
   - 检查是否已有 pending 挑战
   - 创建 BattleHistory（status=pending）
2. `acceptChallenge(Long battleId, Long defenderId, Long deckId)` — 接受挑战
   - 从 defender 的卡组抽随机 6 张作为"布阵"
   - AI 模拟攻防（参见 AI 逻辑）
   - 计算胜负
   - 更新奖杯
3. `calculateBattleResult(/* AI模拟逻辑 */)` — AI对战引擎
   - **AI 逻辑**：从防守方卡组随机出牌（优先低费→高费）
   - 答对率使用防守方历史练习平均答对率（从 practice_logs 表统计）
   - 进攻方（真人）的答题数据由前端提交
4. `getBattleHistory(Long userId)` — 获取历史战报
5. `getRankInfo(Long userId)` — 获取段位信息

### AI 对战引擎详细逻辑
```java
public BattleResult simulateBattle(UserDeck deck, double defenderAccuracy) {
    // 1. 从卡组选 6 张卡（按费用曲线优先低费）
    // 2. 每回合 AI 优先出费用最低的卡
    // 3. 每张卡用 defenderAccuracy 概率"答对"
    // 4. 答对=卡上场攻击，答错=卡回手
    // 5. 累计造成的总伤害作为 defender_score
    // 6. 6张卡都出完（或回合结束）后计算总分
}
```

### TrophyService.java
**路径**: `backend/src/main/java/com/pengyouquan/english/service/TrophyService.java`

- `updateTrophies(Long userId, int change)` — 增/减奖杯
- `getTrophies(Long userId)` — 获取当前奖杯
- `getTier(int trophies)` — 根据奖杯查段位

奖杯数据可以存到 `users` 表加一个 `trophies` 字段，或者单独一张表。为简单，建议在 `users` 表加 `trophies INT DEFAULT 0` 字段（通过 Flyway V19 加 ALTER TABLE 语句），或者 create 一个 `user_stats` 表。

更好方案：创建 `user_stats` 表保留扩展性。
```sql
CREATE TABLE IF NOT EXISTS user_stats (
  user_id BIGINT PRIMARY KEY,
  trophies INT DEFAULT 0,
  wins INT DEFAULT 0,
  losses INT DEFAULT 0,
  win_streak INT DEFAULT 0,
  best_trophies INT DEFAULT 0,
  FOREIGN KEY (user_id) REFERENCES users(id)
);
```

---

## Task 5: 后端 Controller

### BattleController.java
**路径**: `backend/src/main/java/com/pengyouquan/english/controller/BattleController.java`

```java
@RestController
@RequestMapping("/api")
public class BattleController {
    @PostMapping("/battle/challenge")         // 发起挑战
    @GetMapping("/battle/pending")            // 获取待处理挑战
    @PostMapping("/battle/{id}/accept")        // 接受并完成挑战
    @GetMapping("/battle/history")             // 战报
    @GetMapping("/battle/rank")                // 段位信息
    @GetMapping("/battle/leaderboard")         // 排行榜
}
```

**接口详情**：
- `POST /api/battle/challenge` — body: `{defenderId, deckId}` → ApiResponse<BattleHistoryDTO>
- `GET /api/battle/pending` — 当前用户作为 defender 且 status=pending 的挑战列表
- `POST /api/battle/{id}/accept` — body: `{deckId, score, accuracy, avgDifficulty}` → ApiResponse<BattleResultDTO>
  - score = 进攻方答题正确的数量
  - accuracy = 进攻方答对率
  - avgDifficulty = 平均难度（1-5）
- `GET /api/battle/history` — 当前用户的战报
- `GET /api/battle/rank` — 当前用户的段位+奖杯
- `GET /api/battle/leaderboard` — 前100名排行榜

---

## Task 6: 后端 - AI 对手逻辑

在 BattleService 内部实现，不需额外文件。

**核心算法**：
```
1. 进攻方（真人）提交 {score, accuracy, avgDifficulty}
2. AI 代表防守方：
   a. 从防守方卡组中随机选 6 张卡（优先低费，费用曲线：1-2费2张，3-4费2张，5+费2张）
   b. 每张卡出牌时，用 defender_accuracy（从 practice_logs 取平均值）概率"答对"
   c. 答对的卡，根据费用造成 1-5 点伤害（费用越高伤害越高）
   d. 累计总伤害作为 defender_score
3. 比较双方 score，高者胜
4. 胜方 +30 奖杯，负方 -25
5. 记录到 battle_history
```

**获取防守方准确率**：
```sql
SELECT AVG(accuracy) FROM practice_logs WHERE user_id = ? AND created_at > DATE_SUB(NOW(), INTERVAL 30 DAY)
```

---

## Task 7: 前端 - cardClient 扩展

**文件**: `frontend/src/api/cardClient.ts`

增加好友/对战相关 API：
```typescript
export const cardApi = {
  // ... 现有方法
  
  // 好友
  searchUsers: async (query: string) => request(`/api/users/search?q=${encodeURIComponent(query)}`),
  sendFriendRequest: async (friendId: number) => 
    request('/api/friends/request', { method: 'POST', body: JSON.stringify({ friendId }) }),
  acceptFriendRequest: async (friendId: number) =>
    request('/api/friends/accept', { method: 'POST', body: JSON.stringify({ friendId }) }),
  getFriends: async () => request('/api/friends'),
  getPendingRequests: async () => request('/api/friends/pending'),

  // 对战
  challengePlayer: async (defenderId: number, deckId: number) =>
    request('/api/battle/challenge', { method: 'POST', body: JSON.stringify({ defenderId, deckId }) }),
  getPendingBattles: async () => request('/api/battle/pending'),
  acceptBattle: async (battleId: number, deckId: number, result: { score: number; accuracy: number; avgDifficulty: number }) =>
    request(`/api/battle/${battleId}/accept`, { method: 'POST', body: JSON.stringify({ deckId, ...result }) }),
  getBattleHistory: async () => request('/api/battle/history'),
  getRank: async () => request('/api/battle/rank'),
  getLeaderboard: async () => request('/api/battle/leaderboard'),
};
```

---

## Task 8: 前端 - BattlePage（对战主页面）

**文件**: `frontend/src/BattlePage.tsx`

三个标签页：
1. **⚔️ 对战** — 好友列表，点好友→选择卡组→发起挑战
2. **📜 战报** — battle_history 列表，显示胜负、得分、奖杯变化
3. **🏆 排名** — 段位信息+排行榜

### 页面结构

```tsx
export default function BattlePage({ user, onNavigate }: { user: any; onNavigate: (target: string, data?: any) => void }) {
  const [tab, setTab] = useState<'battle' | 'history' | 'rank'>('battle');
  const [friends, setFriends] = useState<any[]>([]);
  const [pendingBattles, setPendingBattles] = useState<any[]>([]);
  const [history, setHistory] = useState<any[]>([]);
  const [rank, setRank] = useState<any>(null);
  const [leaderboard, setLeaderboard] = useState<any[]>([]);
  const [decks, setDecks] = useState<any[]>([]);
  const [showChallengeModal, setShowChallengeModal] = useState<any>(null);

  // ... 加载数据、渲染逻辑
}
```

**UI 风格**：对齐现有 CardCollectionPage 的设计语言（深色主题、teal 强调色、圆角卡片、稀有度色标）

**对战标签页布局**：
- 顶部：段位卡片（显示奖杯数、段位名、图标）
- 中部："有 X 个待处理挑战"（显示 pending 挑战数）
- 好友列表：每行显示好友昵称+在线状态+"挑战"按钮

**战报标签页**：
- 列表，每项显示：对手名、胜负图标、得分、奖杯变化、时间

**排名标签页**：
- 当前段位+奖杯进度条
- 排行榜（前50名）：排名、头像、昵称、奖杯、段位

---

## Task 9: 前端 - 底部导航更新

**文件**: `frontend/src/App.tsx`

底部导航增加"⚔️对战"页签，放在"🎴卡牌"和"👑封臣"之间：

```tsx
{ key: 'battle', icon: <span>⚔️</span>, label: '对战', requiresLogin: true },
```

同时：
- `page` state 增加 `'battle'` 类型
- 页面渲染增加 `{page === 'battle' && <BattlePage user={user} onNavigate={handleNavigate} />}`
- topbar 标题增加 `{page === 'battle' && '对战'}`

---

## Task 10: 部署验证

```bash
# 1. 构建后端
cd /home/heaton/pengyouquan-english
./gradlew bootJar

# 2. 构建前端
cd frontend && npm run build && cd ..

# 3. 部署后端
docker cp backend/build/libs/*.jar pengyouquan-backend:/app/app.jar
docker restart pengyouquan-backend

# 4. 部署前端
docker cp frontend/dist/. pengyouquan-frontend:/usr/share/nginx/html/
docker exec pengyouquan-frontend chmod -R 755 /usr/share/nginx/html

# 5. 验证
curl -s https://english-sit.pengyouquan.top/api/cards | head -20
curl -s https://english-sit.pengyouquan.top/api/battle/rank
```

---

## 验收标准

1. ✅ 好友功能：搜索用户→发送请求→接受→成为好友
2. ✅ 发起挑战：选好友→选卡组→创建挑战（battle_history 状态 pending）
3. ✅ 接受挑战：查看待处理→选卡组→AI模拟出结果
4. ✅ 战报：胜/负/平，显示得分、奖杯变化
5. ✅ 段位：青铜→传说 6个段位，奖杯升降正确
6. ✅ 排行榜：前50名显示
7. ✅ 底部导航增加"⚔️对战"页签
8. ✅ 全部功能前后端打通，部署到 SIT
