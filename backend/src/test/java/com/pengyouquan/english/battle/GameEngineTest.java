package com.pengyouquan.english.battle;

import com.pengyouquan.english.model.Card;
import com.pengyouquan.english.model.Deck;
import com.pengyouquan.english.repository.CardRepository;
import com.pengyouquan.english.repository.DeckRepository;
import com.pengyouquan.english.repository.UserCardRepository;
import com.pengyouquan.english.repository.UserRepository;
import com.pengyouquan.english.service.TrophyService;
import com.pengyouquan.english.battle.GameEngine.PlayCardResult;
import com.pengyouquan.english.battle.GameEngine.AttackResult;
import com.pengyouquan.english.battle.GameEngine.EndTurnResult;
import com.pengyouquan.english.battle.GameEngine.GameOverResult;
import com.pengyouquan.english.battle.GameEngine.MulliganResult;
import com.pengyouquan.english.battle.GameEngine.TurnStartResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("游戏引擎测试")
class GameEngineTest {

    @Mock
    private CardRepository cardRepository;

    @Mock
    private UserCardRepository userCardRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private DeckRepository deckRepository;

    @Mock
    private TrophyService trophyService;

    @InjectMocks
    private GameEngine gameEngine;

    private List<Card> allTestCards;
    private Deck testDeck;

    @BeforeEach
    void setUp() {
        initTestCards();

        // 20张卡牌ID的牌组
        testDeck = new Deck();
        testDeck.setUserId(1L);
        testDeck.setCardIds("[1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20]");
        testDeck.setIsActive(true);

        // 默认 Mock 行为
        when(deckRepository.findByUserIdAndIsActiveTrue(anyLong())).thenReturn(Optional.of(testDeck));
        when(cardRepository.findAllById(anyList())).thenAnswer(invocation -> {
            List<Long> ids = invocation.getArgument(0);
            return allTestCards.stream()
                    .filter(c -> ids.contains(c.getId()))
                    .collect(Collectors.toList());
        });
        when(userCardRepository.findByUserId(anyLong())).thenReturn(new ArrayList<>());
    }

    // ============================================================
    //   Helper 方法
    // ============================================================

    private void initTestCards() {
        allTestCards = new ArrayList<>();
        allTestCards.add(createCard(1, "小兵", "Footman", "minion", 1, 1, 2, null, null));
        allTestCards.add(createCard(2, "步兵", "Soldier", "minion", 2, 2, 3, null, null));
        allTestCards.add(createCard(3, "嘲讽卫士", "Taunt Guard", "minion", 3, 2, 4, null, "[\"taunt\"]"));
        allTestCards.add(createCard(4, "圣盾骑士", "Divine Knight", "minion", 2, 3, 1, null, "[\"divine_shield\"]"));
        allTestCards.add(createCard(5, "吸血蝙蝠", "Vampire Bat", "minion", 3, 3, 3, null, "[\"lifesteal\"]"));
        allTestCards.add(createCard(6, "毒蛇", "Poison Snake", "minion", 2, 2, 1, null, "[\"poisonous\"]"));
        allTestCards.add(createCard(7, "风怒鹰", "Windfury Eagle", "minion", 4, 4, 3, null, "[\"windfury\"]"));
        allTestCards.add(createCard(8, "法强龙", "Spell Dragon", "minion", 4, 2, 5, null, "[\"spell_damage\"]"));
        allTestCards.add(createCard(9, "潜行刺客", "Stealth Assassin", "minion", 3, 4, 2, null, "[\"stealth\"]"));
        allTestCards.add(createCard(10, "火球术", "Fireball", "spell", 4, 0, 0, null, null));
        allTestCards.add(createCard(11, "真银剑", "Truesilver Sword", "weapon", 3, 4, 2, null, null));
        allTestCards.add(createCard(12, "法术反制", "Counterspell", "secret", 3, 0, 0,
                "{\"secretType\":\"counter\"}", null));
        allTestCards.add(createCard(13, "寒冰屏障", "Ice Barrier", "secret", 3, 0, 0,
                "{\"secretType\":\"ice_barrier\"}", null));
        allTestCards.add(createCard(14, "蒸发", "Vaporize", "secret", 3, 0, 0,
                "{\"secretType\":\"vaporize\"}", null));
        allTestCards.add(createCard(15, "突袭狼", "Rush Wolf", "minion", 3, 3, 2, null, "[\"rush\"]"));
        allTestCards.add(createCard(16, "大块头", "Big Guy", "minion", 5, 5, 5, null, null));
        allTestCards.add(createCard(17, "巨人", "Giant", "minion", 6, 6, 6, null, null));
        allTestCards.add(createCard(18, "侍从", "Attendant", "minion", 2, 1, 3, null, null));
        allTestCards.add(createCard(19, "战士", "Warrior", "minion", 3, 2, 3, null, null));
        allTestCards.add(createCard(20, "统领", "Commander", "minion", 4, 4, 4, null, null));
    }

    private Card createCard(long id, String nameCn, String nameEn, String cardType,
                            int cost, Integer attack, Integer health,
                            String effectJson, String keywords) {
        Card c = new Card();
        c.setId(id);
        c.setNameCn(nameCn);
        c.setNameEn(nameEn);
        c.setCardType(cardType);
        c.setCost(cost);
        c.setAttack(attack);
        c.setHealth(health);
        c.setEffectJson(effectJson);
        c.setKeywords(keywords);
        return c;
    }

    /** 创建游戏并完成双方调度，进入 PLAYING 阶段 */
    private GameSession createTestGameWithMulliganDone() {
        GameSession session = gameEngine.createGame(1L, 2L, "Player1", "Player2", 100, 100);
        String sid = session.getSessionId();
        gameEngine.processMulligan(sid, 1L, new ArrayList<>());
        gameEngine.processMulligan(sid, 2L, new ArrayList<>());
        return session;
    }

    /** 创建 CardState 快捷方法 */
    private CardState cardState(long cardId, String nameCn, String nameEn, String cardType,
                                int cost, int attack, int health, String effectJson) {
        return new CardState(cardId, nameCn, nameEn, cardType, "common", cost, attack, health, effectJson);
    }

    // ============================================================
    //   1. 游戏创建 & 先手后手
    // ============================================================

    @Test
    @DisplayName("创建游戏：双方初始化为30血、3手牌、MULLIGAN阶段")
    void createGame_playersInitialized() {
        GameSession session = gameEngine.createGame(1L, 2L, "P1", "P2", 100, 200);

        assertThat(session.getPhase()).isEqualTo(GamePhase.MULLIGAN);
        assertThat(session.getPlayer1Id()).isEqualTo(1L);
        assertThat(session.getPlayer2Id()).isEqualTo(2L);

        PlayerState p1 = session.getPlayer1();
        PlayerState p2 = session.getPlayer2();
        assertThat(p1.getHealth()).isEqualTo(30);
        assertThat(p2.getHealth()).isEqualTo(30);
        // 先手3张，后手5张（3初始+1额外+1硬币），但先后顺序随机
        PlayerState first = p1.isGoingFirst() ? p1 : p2;
        PlayerState second = p1.isGoingFirst() ? p2 : p1;
        assertThat(first.getHand()).hasSize(3);
        assertThat(second.getHand()).hasSize(5);
        assertThat(p1.getTrophies()).isEqualTo(100);
        assertThat(p2.getTrophies()).isEqualTo(200);
    }

    @Test
    @DisplayName("创建游戏：确认先手后手，后手有幸运币和多1张牌")
    void createGame_goingFirstAndSecond() {
        GameSession session = gameEngine.createGame(1L, 2L, "P1", "P2", 100, 100);

        PlayerState p1 = session.getPlayer1();
        PlayerState p2 = session.getPlayer2();

        // 恰好一人先手
        assertThat(p1.isGoingFirst() ^ p2.isGoingFirst()).isTrue();
        // currentPlayerId 与先手一致
        Long firstPlayer = p1.isGoingFirst() ? 1L : 2L;
        assertThat(session.getCurrentPlayerId()).isEqualTo(firstPlayer);

        // 后手有幸运币，手牌数多1+1=2张
        PlayerState second = p1.isGoingFirst() ? p2 : p1;
        PlayerState first = p1.isGoingFirst() ? p1 : p2;
        assertThat(first.getHand()).hasSize(3);
        assertThat(second.getHand()).hasSize(5); // 3初始 + 1额外 + 1硬币
        assertThat(second.getHand().stream().anyMatch(c -> c.getCardId() == 0L)).isTrue();
    }

    // ============================================================
    //   2. 法力水晶
    // ============================================================

    @Test
    @DisplayName("法力水晶：T1=1, T2=2, ..., T10=10, T11起封顶10")
    void manaCrystals_t1ToT10() {
        GameSession session = createTestGameWithMulliganDone();
        String sid = session.getSessionId();

        for (int expectedTurn = 1; expectedTurn <= 10; expectedTurn++) {
            TurnStartResult result = gameEngine.startTurn(sid);
            assertThat(result.turnNumber).isEqualTo(expectedTurn);
            assertThat(result.maxMana).isEqualTo(expectedTurn);
            assertThat(result.mana).isEqualTo(expectedTurn);

            gameEngine.endTurn(sid, session.getCurrentPlayerId());
        }

        // T11 封顶 10
        TurnStartResult r11 = gameEngine.startTurn(sid);
        assertThat(r11.maxMana).isEqualTo(10);
        assertThat(r11.mana).isEqualTo(10);
    }

    // ============================================================
    //   3. 幸运币
    // ============================================================

    @Test
    @DisplayName("幸运币：后手打出后mana+1")
    void playCoin_increasesMana() {
        GameSession session = createTestGameWithMulliganDone();
        String sid = session.getSessionId();
        Long firstPlayer = session.getCurrentPlayerId();
        Long secondPlayer = session.getOpponentId(firstPlayer);

        // T1 先手，结束
        gameEngine.startTurn(sid);
        gameEngine.endTurn(sid, firstPlayer);

        // T2 后手：mana = maxMana = 2，先消耗法力再打硬币验证增长
        gameEngine.startTurn(sid);
        assertThat(session.getCurrentPlayerId()).isEqualTo(secondPlayer);

        PlayerState p2 = session.getPlayerState(secondPlayer);
        p2.setMana(0); // 消耗所有法力

        // 打出幸运币(cardId=0)：mana = min(0+1, 2) = 1
        PlayCardResult result = gameEngine.playCard(sid, secondPlayer, 0L, null);
        assertThat(result.success).isTrue();
        assertThat(result.manaRemaining).isEqualTo(1);
    }

    // ============================================================
    //   4. Mulligan 调度
    // ============================================================

    @Test
    @DisplayName("Mulligan：换牌后手牌数量不变")
    void processMulligan_handSizeUnchanged() {
        GameSession session = gameEngine.createGame(1L, 2L, "P1", "P2", 100, 100);
        String sid = session.getSessionId();

        int initialSize = session.getPlayer1().getHand().size();
        // 换掉第一张牌
        Long firstCardId = session.getPlayer1().getHand().get(0).getCardId();
        MulliganResult result = gameEngine.processMulligan(sid, 1L, List.of(firstCardId));

        assertThat(result.success).isTrue();
        assertThat(session.getPlayer1().getHand()).hasSize(initialSize);
    }

    @Test
    @DisplayName("Mulligan：双方提交后bothReady=true，进入PLAYING阶段")
    void processMulligan_bothReady_phasePlaying() {
        GameSession session = gameEngine.createGame(1L, 2L, "P1", "P2", 100, 100);
        String sid = session.getSessionId();

        MulliganResult r1 = gameEngine.processMulligan(sid, 1L, new ArrayList<>());
        assertThat(r1.bothReady).isFalse();
        assertThat(session.getPhase()).isEqualTo(GamePhase.MULLIGAN);

        MulliganResult r2 = gameEngine.processMulligan(sid, 2L, new ArrayList<>());
        assertThat(r2.bothReady).isTrue();
        assertThat(session.getPhase()).isEqualTo(GamePhase.PLAYING);
    }

    // ============================================================
    //   5. 关键词
    // ============================================================

    @Test
    @DisplayName("关键词-吸血(Lifesteal)：攻击随从恢复英雄等量生命")
    void keyword_lifesteal_healsHero() {
        GameSession session = createTestGameWithMulliganDone();
        String sid = session.getSessionId();
        Long current = session.getCurrentPlayerId();
        PlayerState p = session.getPlayerState(current);
        PlayerState o = session.getOpponent(current);

        gameEngine.startTurn(sid);

        CardState lifesteal = cardState(100L, "吸血者", "Lifestealer", "minion", 3, 3, 3, null);
        lifesteal.setHasLifesteal(true);
        lifesteal.setCanAttack(true);
        p.setBoard(new ArrayList<>(List.of(lifesteal)));
        p.setHealth(20); // 英雄受伤

        CardState target = cardState(101L, "目标", "Target", "minion", 2, 2, 2, null);
        o.setBoard(new ArrayList<>(List.of(target)));

        AttackResult result = gameEngine.declareAttack(sid, current, 100L, "minion", 101L);
        assertThat(result.success).isTrue();
        assertThat(result.lifestealHealed).isEqualTo(3);
        assertThat(p.getHealth()).isEqualTo(23);
    }

    @Test
    @DisplayName("关键词-剧毒(Poisonous)：直接消灭随从")
    void keyword_poisonous_killsMinion() {
        GameSession session = createTestGameWithMulliganDone();
        String sid = session.getSessionId();
        Long current = session.getCurrentPlayerId();
        PlayerState p = session.getPlayerState(current);
        PlayerState o = session.getOpponent(current);

        gameEngine.startTurn(sid);

        CardState poison = cardState(102L, "毒蛇", "Viper", "minion", 2, 2, 1, null);
        poison.setHasPoisonous(true);
        poison.setCanAttack(true);
        p.setBoard(new ArrayList<>(List.of(poison)));

        CardState bigTarget = cardState(103L, "大块头", "Big", "minion", 5, 5, 10, null);
        o.setBoard(new ArrayList<>(List.of(bigTarget)));

        AttackResult result = gameEngine.declareAttack(sid, current, 102L, "minion", 103L);
        assertThat(result.success).isTrue();
        assertThat(result.poisonousKill).isTrue();
        assertThat(result.defenderDead).isTrue();
        // 大块头从对战移除
        assertThat(o.getBoard()).doesNotContain(bigTarget);
    }

    @Test
    @DisplayName("关键词-风怒(Windfury)：一回合可攻击两次")
    void keyword_windfury_attacksTwice() {
        GameSession session = createTestGameWithMulliganDone();
        String sid = session.getSessionId();
        Long current = session.getCurrentPlayerId();
        PlayerState p = session.getPlayerState(current);
        PlayerState o = session.getOpponent(current);

        gameEngine.startTurn(sid);

        CardState windfury = cardState(104L, "风怒者", "Windfury", "minion", 4, 3, 3, null);
        windfury.setHasWindfury(true);
        windfury.setWindfuryAttacksRemaining(2);
        windfury.setCanAttack(true);
        p.setBoard(new ArrayList<>(List.of(windfury)));

        CardState t1 = cardState(105L, "目标1", "T1", "minion", 1, 1, 3, null);
        CardState t2 = cardState(106L, "目标2", "T2", "minion", 1, 1, 3, null);
        o.setBoard(new ArrayList<>(List.of(t1, t2)));

        // 第一次攻击
        AttackResult r1 = gameEngine.declareAttack(sid, current, 104L, "minion", 105L);
        assertThat(r1.success).isTrue();
        assertThat(windfury.isCanAttack()).isTrue();
        assertThat(windfury.getWindfuryAttacksRemaining()).isEqualTo(1);

        // 第二次攻击
        AttackResult r2 = gameEngine.declareAttack(sid, current, 104L, "minion", 106L);
        assertThat(r2.success).isTrue();
        assertThat(windfury.isCanAttack()).isFalse();
        assertThat(windfury.getWindfuryAttacksRemaining()).isEqualTo(0);

        // 第三次攻击应失败
        AttackResult r3 = gameEngine.declareAttack(sid, current, 104L, "minion", 106L);
        assertThat(r3.success).isFalse();
    }

    @Test
    @DisplayName("关键词-法强(Spell Damage)：增加法术伤害+1")
    void keyword_spellDamage_boostsDamage() {
        GameSession session = createTestGameWithMulliganDone();
        String sid = session.getSessionId();
        Long current = session.getCurrentPlayerId();
        PlayerState p = session.getPlayerState(current);
        PlayerState o = session.getOpponent(current);

        // 提前放一个法强随从在场上
        CardState spellDmg = cardState(8L, "法强龙", "Spell Dragon", "minion", 4, 2, 5, null);
        spellDmg.setHasSpellDamage(true);
        p.setBoard(new ArrayList<>(List.of(spellDmg)));
        p.addSpellDamage(1);

        // 手上放一张4费法术
        CardState fireball = cardState(10L, "火球术", "Fireball", "spell", 4, 0, 0, null);
        p.setHand(new ArrayList<>(List.of(fireball)));
        p.setMana(10);
        p.setMaxMana(10);

        PlayCardResult result = gameEngine.playCard(sid, current, 10L, null);
        assertThat(result.success).isTrue();
        // 法术伤害 = max(1,4) + 1(法强) = 5
        assertThat(result.damageDealt).isEqualTo(5);
        assertThat(o.getHealth()).isEqualTo(25);
    }

    // ============================================================
    //   6. 武器
    // ============================================================

    @Test
    @DisplayName("武器：装备后设置武器状态")
    void weapon_equip_setsWeaponState() {
        GameSession session = createTestGameWithMulliganDone();
        String sid = session.getSessionId();
        Long current = session.getCurrentPlayerId();
        PlayerState p = session.getPlayerState(current);

        CardState weaponCard = cardState(11L, "真银剑", "Sword", "weapon", 3, 4, 2, null);
        p.setHand(new ArrayList<>(List.of(weaponCard)));
        p.setMana(10);
        p.setMaxMana(10);

        PlayCardResult result = gameEngine.playCard(sid, current, 11L, null);
        assertThat(result.success).isTrue();

        WeaponState w = p.getWeapon();
        assertThat(w).isNotNull();
        assertThat(w.getAttack()).isEqualTo(4);
        assertThat(w.getDurability()).isEqualTo(2);
    }

    @Test
    @DisplayName("武器：英雄攻击消耗耐久")
    void weapon_attack_consumesDurability() {
        GameSession session = createTestGameWithMulliganDone();
        String sid = session.getSessionId();
        Long current = session.getCurrentPlayerId();
        PlayerState p = session.getPlayerState(current);
        PlayerState o = session.getOpponent(current);

        p.setWeapon(new WeaponState(11L, "真银剑", 4, 2));
        p.setMana(10);
        p.setMaxMana(10);

        gameEngine.startTurn(sid);

        AttackResult result = gameEngine.heroAttack(sid, current, "hero", null);
        assertThat(result.success).isTrue();
        assertThat(result.isHeroAttack).isTrue();
        assertThat(result.damage).isEqualTo(4);
        assertThat(o.getHealth()).isEqualTo(26);
        assertThat(p.getWeapon().getDurability()).isEqualTo(1);
    }

    @Test
    @DisplayName("武器：耐久归零后损坏")
    void weapon_breaks_whenDurabilityZero() {
        GameSession session = createTestGameWithMulliganDone();
        String sid = session.getSessionId();
        Long current = session.getCurrentPlayerId();
        PlayerState p = session.getPlayerState(current);

        p.setWeapon(new WeaponState(11L, "短剑", 3, 1));
        p.setHasAttackedThisTurn(false);
        p.setMana(10);
        p.setMaxMana(10);

        gameEngine.startTurn(sid);

        AttackResult result = gameEngine.heroAttack(sid, current, "hero", null);
        assertThat(result.success).isTrue();
        // 武器应被移除
        assertThat(p.getWeapon()).isNull();
    }

    @Test
    @DisplayName("武器：没有武器时英雄攻击失败")
    void heroAttack_noWeapon_fails() {
        GameSession session = createTestGameWithMulliganDone();
        String sid = session.getSessionId();
        Long current = session.getCurrentPlayerId();

        gameEngine.startTurn(sid);

        AttackResult result = gameEngine.heroAttack(sid, current, "hero", null);
        assertThat(result.success).isFalse();
        assertThat(result.errorMessage).contains("没有装备武器");
    }

    // ============================================================
    //   7. 奥秘
    // ============================================================

    @Test
    @DisplayName("奥秘-法术反制(Counter)：抵消法术")
    void secret_counter_negatesSpell() {
        GameSession session = createTestGameWithMulliganDone();
        String sid = session.getSessionId();
        Long current = session.getCurrentPlayerId();
        PlayerState p = session.getPlayerState(current);
        PlayerState o = session.getOpponent(current);

        gameEngine.startTurn(sid);

        // 对手挂上法术反制奥秘
        o.setSecrets(new ArrayList<>(List.of(new SecretState(12L, "法术反制", "counter"))));

        // 当前玩家放一张法术
        CardState fireball = cardState(10L, "火球术", "Fireball", "spell", 4, 0, 0, null);
        p.setHand(new ArrayList<>(List.of(fireball)));
        p.setMana(10);
        p.setMaxMana(10);

        PlayCardResult result = gameEngine.playCard(sid, current, 10L, null);
        assertThat(result.success).isTrue();
        assertThat(result.wasCountered).isTrue();
        // 法术被反制，无伤害
        assertThat(result.damageDealt).isEqualTo(0);
        assertThat(o.getHealth()).isEqualTo(30);
        // 奥秘已被移除
        assertThat(o.getSecrets()).isEmpty();
    }

    @Test
    @DisplayName("奥秘-冰甲(Ice Barrier)：英雄攻击后给对手+8护甲")
    void secret_iceBarrier_grantsHealth() {
        GameSession session = createTestGameWithMulliganDone();
        String sid = session.getSessionId();
        Long current = session.getCurrentPlayerId();
        PlayerState p = session.getPlayerState(current);
        PlayerState o = session.getOpponent(current);

        // 给对手挂冰甲
        o.setSecrets(new ArrayList<>(List.of(new SecretState(13L, "寒冰屏障", "ice_barrier"))));

        // 装备武器
        p.setWeapon(new WeaponState(11L, "真银剑", 4, 2));
        p.setHasAttackedThisTurn(false);

        gameEngine.startTurn(sid);

        int healthBefore = o.getHealth(); // 30
        AttackResult result = gameEngine.heroAttack(sid, current, "hero", null);
        assertThat(result.success).isTrue();

        // 冰甲给对手(防御方)+8血：30+8=38，武器4攻打完后：38-4=34
        // 注：ice_barrier触发后opponent增加8血，然后武器攻击造成4伤害
        // triggerSecretEffect: opponent.setHealth(min(30+8, 38)) = 38
        // 然后 heroAttack 中: opponent.setHealth(38 - 4) = 34
        assertThat(o.getHealth()).isEqualTo(34);
        assertThat(o.getSecrets()).isEmpty();
    }

    @Test
    @DisplayName("奥秘-蒸发(Vaporize)：消灭攻击英雄的随从")
    void secret_vaporize_destroysAttacker() {
        GameSession session = createTestGameWithMulliganDone();
        String sid = session.getSessionId();
        Long current = session.getCurrentPlayerId();
        PlayerState p = session.getPlayerState(current);
        PlayerState o = session.getOpponent(current);

        gameEngine.startTurn(sid);

        // 对手挂蒸发
        o.setSecrets(new ArrayList<>(List.of(new SecretState(14L, "蒸发", "vaporize"))));

        // 当前玩家场上有个可以攻击的随从
        CardState attacker = cardState(107L, "攻击者", "Attacker", "minion", 2, 3, 3, null);
        attacker.setCanAttack(true);
        p.setBoard(new ArrayList<>(List.of(attacker)));

        AttackResult result = gameEngine.declareAttack(sid, current, 107L, "hero", null);
        assertThat(result.success).isTrue();
        assertThat(result.vaporized).isTrue();
        // 攻击者从场上移除
        assertThat(p.getBoard()).doesNotContain(attacker);
        // 奥秘已消耗
        assertThat(o.getSecrets()).isEmpty();
    }

    // ============================================================
    //   8. 攻击流程
    // ============================================================

    @Test
    @DisplayName("攻击：普通攻击随从造成伤害")
    void attack_normal_dealsDamage() {
        GameSession session = createTestGameWithMulliganDone();
        String sid = session.getSessionId();
        Long current = session.getCurrentPlayerId();
        PlayerState p = session.getPlayerState(current);
        PlayerState o = session.getOpponent(current);

        gameEngine.startTurn(sid);

        CardState attacker = cardState(108L, "攻击者", "Attacker", "minion", 2, 3, 4, null);
        attacker.setCanAttack(true);
        p.setBoard(new ArrayList<>(List.of(attacker)));

        CardState defender = cardState(109L, "防御者", "Defender", "minion", 2, 2, 5, null);
        o.setBoard(new ArrayList<>(List.of(defender)));

        AttackResult result = gameEngine.declareAttack(sid, current, 108L, "minion", 109L);
        assertThat(result.success).isTrue();
        assertThat(result.damage).isEqualTo(3);
        assertThat(result.defenderHealthLeft).isEqualTo(2); // 5 - 3
    }

    @Test
    @DisplayName("攻击-嘲讽(Taunt)：必须先攻击嘲讽随从")
    void attack_taunt_mustAttackTauntFirst() {
        GameSession session = createTestGameWithMulliganDone();
        String sid = session.getSessionId();
        Long current = session.getCurrentPlayerId();
        PlayerState p = session.getPlayerState(current);
        PlayerState o = session.getOpponent(current);

        gameEngine.startTurn(sid);

        CardState attacker = cardState(110L, "攻击者", "Attacker", "minion", 2, 3, 3, null);
        attacker.setCanAttack(true);
        p.setBoard(new ArrayList<>(List.of(attacker)));

        CardState nonTaunt = cardState(111L, "非嘲讽", "Normal", "minion", 2, 2, 2, null);
        CardState taunt = cardState(112L, "嘲讽怪", "Taunter", "minion", 3, 1, 4, null);
        taunt.setHasTaunt(true);
        o.setBoard(new ArrayList<>(List.of(nonTaunt, taunt)));

        // 直接攻击非嘲讽 -> 失败
        AttackResult result = gameEngine.declareAttack(sid, current, 110L, "minion", 111L);
        assertThat(result.success).isFalse();
        assertThat(result.errorMessage).contains("嘲讽");

        // 攻击嘲讽 -> 成功
        AttackResult result2 = gameEngine.declareAttack(sid, current, 110L, "minion", 112L);
        assertThat(result2.success).isTrue();
    }

    @Test
    @DisplayName("攻击-圣盾(Divine Shield)：抵挡一次伤害")
    void attack_divineShield_blocksDamage() {
        GameSession session = createTestGameWithMulliganDone();
        String sid = session.getSessionId();
        Long current = session.getCurrentPlayerId();
        PlayerState p = session.getPlayerState(current);
        PlayerState o = session.getOpponent(current);

        gameEngine.startTurn(sid);

        CardState attacker = cardState(113L, "攻击者", "Attacker", "minion", 2, 5, 3, null);
        attacker.setCanAttack(true);
        p.setBoard(new ArrayList<>(List.of(attacker)));

        CardState shielded = cardState(114L, "圣盾兵", "Shielded", "minion", 2, 3, 2, null);
        shielded.setHasDivineShield(true);
        o.setBoard(new ArrayList<>(List.of(shielded)));

        AttackResult result = gameEngine.declareAttack(sid, current, 113L, "minion", 114L);
        assertThat(result.success).isTrue();
        assertThat(result.divineShieldBlocked).isTrue();
        // 圣盾抵挡后目标血量不变
        assertThat(result.defenderHealthLeft).isEqualTo(2);
        // 圣盾已移除
        assertThat(shielded.isHasDivineShield()).isFalse();
    }

    @Test
    @DisplayName("攻击-潜行(Stealth)：无法攻击潜行随从")
    void attack_stealth_cannotBeTargeted() {
        GameSession session = createTestGameWithMulliganDone();
        String sid = session.getSessionId();
        Long current = session.getCurrentPlayerId();
        PlayerState p = session.getPlayerState(current);
        PlayerState o = session.getOpponent(current);

        gameEngine.startTurn(sid);

        CardState attacker = cardState(115L, "攻击者", "Attacker", "minion", 2, 3, 3, null);
        attacker.setCanAttack(true);
        p.setBoard(new ArrayList<>(List.of(attacker)));

        CardState stealth = cardState(116L, "潜行怪", "Stealthed", "minion", 3, 4, 2, null);
        stealth.setHasStealth(true);
        o.setBoard(new ArrayList<>(List.of(stealth)));

        AttackResult result = gameEngine.declareAttack(sid, current, 115L, "minion", 116L);
        assertThat(result.success).isFalse();
        assertThat(result.errorMessage).contains("潜行");
    }

    @Test
    @DisplayName("攻击：攻击英雄直接扣血")
    void attack_hero_dealsDamage() {
        GameSession session = createTestGameWithMulliganDone();
        String sid = session.getSessionId();
        Long current = session.getCurrentPlayerId();
        PlayerState p = session.getPlayerState(current);
        PlayerState o = session.getOpponent(current);

        gameEngine.startTurn(sid);

        CardState attacker = cardState(117L, "攻击者", "Attacker", "minion", 2, 3, 3, null);
        attacker.setCanAttack(true);
        p.setBoard(new ArrayList<>(List.of(attacker)));

        int healthBefore = o.getHealth();
        AttackResult result = gameEngine.declareAttack(sid, current, 117L, "hero", null);
        assertThat(result.success).isTrue();
        assertThat(result.damage).isEqualTo(3);
        assertThat(o.getHealth()).isEqualTo(healthBefore - 3);
    }

    @Test
    @DisplayName("攻击：攻击力为0不能攻击")
    void attack_zeroAttack_fails() {
        GameSession session = createTestGameWithMulliganDone();
        String sid = session.getSessionId();
        Long current = session.getCurrentPlayerId();
        PlayerState p = session.getPlayerState(current);

        gameEngine.startTurn(sid);

        CardState attacker = cardState(118L, "弱者", "Weak", "minion", 1, 0, 2, null);
        attacker.setCanAttack(true);
        p.setBoard(new ArrayList<>(List.of(attacker)));

        AttackResult result = gameEngine.declareAttack(sid, current, 118L, "hero", null);
        assertThat(result.success).isFalse();
    }

    // ============================================================
    //   9. 结束回合 & 认输
    // ============================================================

    @Test
    @DisplayName("结束回合：切换到对方")
    void endTurn_switchesToOpponent() {
        GameSession session = createTestGameWithMulliganDone();
        String sid = session.getSessionId();
        Long current = session.getCurrentPlayerId();
        Long opponent = session.getOpponentId(current);

        gameEngine.startTurn(sid);
        EndTurnResult result = gameEngine.endTurn(sid, current);

        assertThat(result).isNotNull();
        assertThat(result.nextPlayerId).isEqualTo(opponent);
        assertThat(session.getCurrentPlayerId()).isEqualTo(opponent);
    }

    @Test
    @DisplayName("认输：游戏结束，更新奖杯")
    void concede_endsGameAndUpdatesTrophies() {
        when(trophyService.updateTrophies(2L, 30)).thenReturn(30);
        when(trophyService.updateTrophies(1L, -25)).thenReturn(-25);
        when(trophyService.getTrophies(2L)).thenReturn(130);
        when(trophyService.getTrophies(1L)).thenReturn(75);

        GameSession session = gameEngine.createGame(1L, 2L, "P1", "P2", 100, 100);
        String sid = session.getSessionId();

        GameOverResult result = gameEngine.concede(sid, 1L);

        assertThat(result).isNotNull();
        assertThat(result.winnerId).isEqualTo(2L);
        assertThat(result.loserId).isEqualTo(1L);
        verify(trophyService).updateTrophies(2L, 30);
        verify(trophyService).updateTrophies(1L, -25);
    }

    // ============================================================
    //   10. 边界情况
    // ============================================================

    @Test
    @DisplayName("不是当前玩家出牌返回错误")
    void playCard_notYourTurn_returnsError() {
        GameSession session = createTestGameWithMulliganDone();
        String sid = session.getSessionId();
        Long notCurrent = session.getOpponentId(session.getCurrentPlayerId());

        PlayCardResult result = gameEngine.playCard(sid, notCurrent, 1L, null);

        assertThat(result.success).isFalse();
        assertThat(result.errorMessage).isEqualTo("不是你的回合");
    }

    @Test
    @DisplayName("费用不足不能出牌")
    void playCard_insufficientMana_returnsError() {
        GameSession session = createTestGameWithMulliganDone();
        String sid = session.getSessionId();
        Long current = session.getCurrentPlayerId();
        PlayerState p = session.getPlayerState(current);

        // T1只有1费，手上放一张2费卡
        gameEngine.startTurn(sid);
        CardState card = cardState(2L, "步兵", "Soldier", "minion", 2, 2, 3, null);
        p.setHand(new ArrayList<>(List.of(card)));

        PlayCardResult result = gameEngine.playCard(sid, current, 2L, null);
        assertThat(result.success).isFalse();
        assertThat(result.errorMessage).isEqualTo("费用不足");
    }

    @Test
    @DisplayName("牌库抽空后抽牌造成疲劳伤害")
    void drawCard_emptyDeck_takesFatigueDamage() {
        GameSession session = createTestGameWithMulliganDone();
        String sid = session.getSessionId();
        Long current = session.getCurrentPlayerId();
        PlayerState p = session.getPlayerState(current);

        p.setDeck(new ArrayList<>()); // 空牌库
        int healthBefore = p.getHealth();

        gameEngine.startTurn(sid);

        assertThat(p.getHealth()).isEqualTo(healthBefore - 1);
    }

    @Test
    @DisplayName("游戏不存在时返回null/错误")
    void operations_nonexistentSession_returnsNullOrError() {
        assertThat(gameEngine.startTurn("nonexistent")).isNull();
        assertThat(gameEngine.endTurn("nonexistent", 1L)).isNull();
        assertThat(gameEngine.concede("nonexistent", 1L)).isNull();

        PlayCardResult playResult = gameEngine.playCard("nonexistent", 1L, 1L, null);
        assertThat(playResult.success).isFalse();
        assertThat(playResult.errorMessage).isEqualTo("游戏不存在");

        AttackResult attackResult = gameEngine.declareAttack("nonexistent", 1L, 1L, "hero", null);
        assertThat(attackResult.success).isFalse();
        assertThat(attackResult.errorMessage).isEqualTo("游戏不存在");
    }

    @Test
    @DisplayName("直接攻击英雄：对方有嘲讽时必须先打嘲讽")
    void attack_hero_withTauntOnBoard_fails() {
        GameSession session = createTestGameWithMulliganDone();
        String sid = session.getSessionId();
        Long current = session.getCurrentPlayerId();
        PlayerState p = session.getPlayerState(current);
        PlayerState o = session.getOpponent(current);

        gameEngine.startTurn(sid);

        CardState attacker = cardState(119L, "攻击者", "Attacker", "minion", 2, 3, 3, null);
        attacker.setCanAttack(true);
        p.setBoard(new ArrayList<>(List.of(attacker)));

        CardState taunt = cardState(120L, "嘲讽怪", "Taunt", "minion", 3, 1, 4, null);
        taunt.setHasTaunt(true);
        o.setBoard(new ArrayList<>(List.of(taunt)));

        AttackResult result = gameEngine.declareAttack(sid, current, 119L, "hero", null);
        assertThat(result.success).isFalse();
        assertThat(result.errorMessage).contains("嘲讽");
    }
}
