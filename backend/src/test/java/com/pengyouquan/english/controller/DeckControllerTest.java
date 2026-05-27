package com.pengyouquan.english.controller;

import com.pengyouquan.english.model.Deck;
import com.pengyouquan.english.repository.DeckRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("牌组控制器测试")
class DeckControllerTest {

    @Mock
    private DeckRepository deckRepository;

    @Mock
    private Authentication auth;

    @InjectMocks
    private DeckController deckController;

    private Deck testDeck;

    @BeforeEach
    void setUp() {
        testDeck = new Deck();
        testDeck.setId(1L);
        testDeck.setUserId(1L);
        testDeck.setName("测试牌组");
        testDeck.setCardIds("[1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20]");
        testDeck.setIsActive(false);
    }

    // ── 认证相关 ──

    @Test
    @DisplayName("未认证用户保存牌组返回401")
    void saveDeck_unauthorized_returns401() {
        Map<String, Object> body = new HashMap<>();
        body.put("name", "我的牌组");
        body.put("cardIds", createCardIdList(20));

        ResponseEntity<?> response = deckController.saveDeck(null, body);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        Map<?, ?> respBody = (Map<?, ?>) response.getBody();
        assertThat(respBody.get("error")).isEqualTo("未认证");
    }

    // ── 保存牌组 ──

    @Test
    @DisplayName("保存牌组：20张卡牌保存成功")
    void saveDeck_validRequest_returnsSuccess() {
        when(auth.getName()).thenReturn("1");
        when(deckRepository.findByUserId(1L)).thenReturn(new ArrayList<>());
        when(deckRepository.save(any(Deck.class))).thenAnswer(invocation -> {
            Deck saved = invocation.getArgument(0);
            saved.setId(100L);
            return saved;
        });

        Map<String, Object> body = new HashMap<>();
        body.put("name", "我的牌组");
        body.put("cardIds", createCardIdList(20));

        ResponseEntity<?> response = deckController.saveDeck(auth, body);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<?, ?> respBody = (Map<?, ?>) response.getBody();
        assertThat(respBody.get("success")).isEqualTo(true);
        assertThat(respBody.get("deck")).isNotNull();
    }

    @Test
    @DisplayName("保存牌组：卡牌数量不是20张返回400")
    void saveDeck_wrongCardCount_returns400() {
        when(auth.getName()).thenReturn("1");

        Map<String, Object> body = new HashMap<>();
        body.put("name", "不完整牌组");
        body.put("cardIds", createCardIdList(19)); // only 19 cards

        ResponseEntity<?> response = deckController.saveDeck(auth, body);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        Map<?, ?> respBody = (Map<?, ?>) response.getBody();
        assertThat(respBody.get("error")).isNotNull();
    }

    @Test
    @DisplayName("保存牌组：默认名称使用'未命名卡组'")
    void saveDeck_noName_usesDefaultName() {
        when(auth.getName()).thenReturn("1");
        when(deckRepository.findByUserId(1L)).thenReturn(new ArrayList<>());
        when(deckRepository.save(any(Deck.class))).thenAnswer(invocation -> {
            Deck saved = invocation.getArgument(0);
            saved.setId(101L);
            return saved;
        });

        Map<String, Object> body = new HashMap<>();
        body.put("cardIds", createCardIdList(20));
        // No "name" key

        ResponseEntity<?> response = deckController.saveDeck(auth, body);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(deckRepository).save(argThat(d -> "未命名卡组".equals(d.getName())));
    }

    @Test
    @DisplayName("保存牌组：第一套牌组自动激活")
    void saveDeck_firstDeck_autoActive() {
        when(auth.getName()).thenReturn("1");
        when(deckRepository.findByUserId(1L)).thenReturn(new ArrayList<>()); // no existing decks
        when(deckRepository.save(any(Deck.class))).thenAnswer(invocation -> {
            Deck saved = invocation.getArgument(0);
            saved.setId(102L);
            return saved;
        });

        Map<String, Object> body = new HashMap<>();
        body.put("name", "第一套");
        body.put("cardIds", createCardIdList(20));

        deckController.saveDeck(auth, body);

        verify(deckRepository).save(argThat(Deck::getIsActive));
    }

    // ── 列出牌组 ──

    @Test
    @DisplayName("列出牌组：未认证返回401")
    void listDecks_unauthorized_returns401() {
        ResponseEntity<?> response = deckController.listDecks(null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("列出牌组：用户有牌组返回列表")
    void listDecks_withDecks_returnsList() {
        when(auth.getName()).thenReturn("1");
        when(deckRepository.findByUserId(1L)).thenReturn(List.of(testDeck));

        ResponseEntity<?> response = deckController.listDecks(auth);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<?, ?> respBody = (Map<?, ?>) response.getBody();
        assertThat(respBody.get("success")).isEqualTo(true);
        assertThat(respBody.get("decks")).asList().hasSize(1);
    }

    @Test
    @DisplayName("列出牌组：用户无牌组返回空列表")
    void listDecks_empty_returnsEmptyList() {
        when(auth.getName()).thenReturn("1");
        when(deckRepository.findByUserId(1L)).thenReturn(new ArrayList<>());

        ResponseEntity<?> response = deckController.listDecks(auth);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<?, ?> respBody = (Map<?, ?>) response.getBody();
        assertThat(respBody.get("decks")).asList().isEmpty();
    }

    // ── 选择牌组 ──

    @Test
    @DisplayName("选择牌组：未认证返回401")
    void selectDeck_unauthorized_returns401() {
        ResponseEntity<?> response = deckController.selectDeck(null, 1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("选择牌组：牌组不存在返回400")
    void selectDeck_notFound_returns400() {
        when(auth.getName()).thenReturn("1");
        when(deckRepository.findById(999L)).thenReturn(Optional.empty());

        ResponseEntity<?> response = deckController.selectDeck(auth, 999L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        Map<?, ?> respBody = (Map<?, ?>) response.getBody();
        assertThat(respBody.get("error")).isEqualTo("牌组不存在");
    }

    @Test
    @DisplayName("选择牌组：无权操作他人牌组返回403")
    void selectDeck_notOwner_returns403() {
        Deck otherDeck = new Deck();
        otherDeck.setId(2L);
        otherDeck.setUserId(2L); // different user

        when(auth.getName()).thenReturn("1");
        when(deckRepository.findById(2L)).thenReturn(Optional.of(otherDeck));

        ResponseEntity<?> response = deckController.selectDeck(auth, 2L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        Map<?, ?> respBody = (Map<?, ?>) response.getBody();
        assertThat(respBody.get("error")).isEqualTo("无权操作");
    }

    @Test
    @DisplayName("选择牌组：成功激活指定牌组并取消其他激活")
    void selectDeck_success_activatesAndDeactivates() {
        Deck otherActive = new Deck();
        otherActive.setId(10L);
        otherActive.setUserId(1L);
        otherActive.setIsActive(true);

        when(auth.getName()).thenReturn("1");
        when(deckRepository.findById(1L)).thenReturn(Optional.of(testDeck));
        when(deckRepository.findByUserId(1L)).thenReturn(List.of(otherActive, testDeck));
        when(deckRepository.save(any(Deck.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResponseEntity<?> response = deckController.selectDeck(auth, 1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<?, ?> respBody = (Map<?, ?>) response.getBody();
        assertThat(respBody.get("success")).isEqualTo(true);

        // Other deck should be deactivated, target activated
        verify(deckRepository, times(2)).save(any(Deck.class));
        assertThat(testDeck.getIsActive()).isTrue();
        assertThat(otherActive.getIsActive()).isFalse();
    }

    // ── 当前牌组 ──

    @Test
    @DisplayName("获取当前牌组：有激活牌组返回该牌组")
    void getCurrentDeck_activeExists_returnsIt() {
        when(auth.getName()).thenReturn("1");
        when(deckRepository.findByUserIdAndIsActiveTrue(1L)).thenReturn(Optional.of(testDeck));

        ResponseEntity<?> response = deckController.getCurrentDeck(auth);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<?, ?> respBody = (Map<?, ?>) response.getBody();
        assertThat(respBody.get("success")).isEqualTo(true);
        assertThat(respBody.get("deck")).isNotNull();
    }

    @Test
    @DisplayName("获取当前牌组：无激活牌组时自动激活第一套")
    void getCurrentDeck_noActiveDeck_autoActivatesFirst() {
        when(auth.getName()).thenReturn("1");
        when(deckRepository.findByUserIdAndIsActiveTrue(1L)).thenReturn(Optional.empty());
        when(deckRepository.findByUserId(1L)).thenReturn(List.of(testDeck));
        when(deckRepository.save(any(Deck.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResponseEntity<?> response = deckController.getCurrentDeck(auth);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<?, ?> respBody = (Map<?, ?>) response.getBody();
        assertThat(respBody.get("success")).isEqualTo(true);
        assertThat(respBody.get("deck")).isNotNull();
        assertThat(testDeck.getIsActive()).isTrue();
    }

    // ── 工具方法 ──

    private List<Long> createCardIdList(int count) {
        List<Long> ids = new ArrayList<>();
        for (long i = 1; i <= count; i++) ids.add(i);
        return ids;
    }
}
