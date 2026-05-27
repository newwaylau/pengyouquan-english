# Backend P0 — 核心对战修复 (GameEngine.java)

## 项目路径
- 项目: /home/heaton/pengyouquan-english
- 后端: /home/heaton/pengyouquan-english/backend

## 需要修改的文件
1. src/main/java/com/pengyouquan/english/battle/GameEngine.java (当前676行)
2. src/main/java/com/pengyouquan/english/battle/GameSession.java
3. src/main/java/com/pengyouquan/english/battle/CardState.java
4. src/main/java/com/pengyouquan/english/battle/PlayerState.java
5. src/main/java/com/pengyouquan/english/battle/BattleWebSocketHandler.java
6. src/main/java/com/pengyouquan/english/battle/BattleMessage.java

## 任务1: 法力水晶修正 (优先级最高)

**当前代码:**
```java
int maxMana = Math.min(turn + 2, 10); // T1=3, T2=4...
```

**改成:**
```java
int maxMana = Math.min(turn, 10); // T1=1, T2=2...
```

同时修正 `initPlayer` 中的初始法力值：
```java
state.setMana(1);  // 原来是3
state.setMaxMana(1);  // 原来是3
```

## 任务2: 先手后手 + 幸运币

- 随机决定先手
- 先手(goingFirst=true)：起始手牌3张
- 后手(goingFirst=false)：起始手牌4张 + 幸运币(The Coin)
- 幸运币是一张特殊的0费法术牌，卡牌ID=0（特殊标记），打出时获得1点法力水晶
- 在 GameEngine.createGame() 中实现
- 在 GameSession 中添加 `goingFirst` 标记

### 幸运币逻辑
- 卡牌ID固定为0，nameCn="幸运币", nameEn="The Coin"
- cardType="spell", cost=0
- 当打出幸运币时，玩家 mana 增加1（不超过maxMana）
- BattleWebSocketHandler 需要识别幸运币的特殊行为

## 任务3: Mulligan 换牌阶段

### 新阶段: MULLIGAN
- 在 GamePhase 中添加 MULLIGAN 阶段
- 游戏创建后先进入 MULLIGAN 阶段，再进入 PLAYING

### 流程
1. createGame 后，session.phase = MULLIGAN
2. 通过 WebSocket 发送 mulliganStart 事件给双方，包含手牌信息
3. 玩家选择要换掉的牌的ID列表（可空，表示全留）
4. 玩家发送 `mulligan` 操作：
   - 后端把选中的牌放回牌堆，重新洗牌，抽等量牌
   - 当双方都提交后，自动进入 PLAYING 阶段
   - 发送 gameStart 事件

### 前端
- 在 BattleArenaPage 添加 Mulligan 界面（选择换牌）
- 显示手牌，让玩家点击选择要换的牌（高亮），确认后提交
- WebSocket 操作: `{ type: "mulligan", cardIds: [...] }`

## 任务4: GameEngine 冷却

- 在 GameEngine 中添加 @Scheduled 定时任务
- 每60秒执行一次
- 检查所有活跃游戏的 lastActionTime
- 超过5分钟无操作 → 结束游戏，双方无奖杯变化
- 超过10分钟 → 从 activeGames 中移除

## 测试验证
- 编译: cd /home/heaton/pengyouquan-english/backend && mvn compile -q
- Docker部署: 详见下文

## 部署
Docker 部署方式:
1. 修改完代码后 cd /home/heaton/pengyouquan-english/backend
2. mvn package -DskipTests -q
3. docker cp target/english-0.0.1-SNAPSHOT.jar pengyouquan-english-backend-1:/app/app.jar
4. docker restart pengyouquan-english-backend-1
