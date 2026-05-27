package com.pengyouquan.english.controller;

import com.pengyouquan.english.dto.DeckDTO;
import com.pengyouquan.english.model.Deck;
import com.pengyouquan.english.repository.DeckRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/deck")
public class DeckController {

    private final DeckRepository deckRepository;

    public DeckController(DeckRepository deckRepository) {
        this.deckRepository = deckRepository;
    }

    /** 获取当前用户ID */
    private Long getUserId(Authentication auth) {
        if (auth == null || auth.getName() == null) return null;
        try { return Long.parseLong(auth.getName()); } catch (NumberFormatException e) { return null; }
    }

    /** 保存牌组（20张卡牌ID） */
    @PostMapping("/save")
    public ResponseEntity<?> saveDeck(Authentication auth, @RequestBody Map<String, Object> body) {
        Long userId = getUserId(auth);
        if (userId == null) return ResponseEntity.status(401).body(Map.of("error", "未认证"));

        String name = body.containsKey("name") ? (String) body.get("name") : "未命名卡组";
        List<?> rawIds = body.get("cardIds") instanceof List ? (List<?>) body.get("cardIds") : new ArrayList<>();

        if (rawIds.size() != 20) {
            return ResponseEntity.badRequest().body(Map.of("error", "牌组必须包含20张卡牌"));
        }

        List<Long> cardIds = rawIds.stream()
                .filter(Number.class::isInstance)
                .map(v -> ((Number) v).longValue())
                .collect(Collectors.toList());

        Deck deck = new Deck();
        deck.setUserId(userId);
        deck.setName(name);
        deck.setCardIds(new com.fasterxml.jackson.databind.ObjectMapper().valueToTree(cardIds).toString());

        // 如果是第一套牌组，设为激活
        List<Deck> existing = deckRepository.findByUserId(userId);
        if (existing.isEmpty()) {
            deck.setIsActive(true);
        }

        Deck saved = deckRepository.save(deck);

        DeckDTO dto = new DeckDTO(saved.getId(), saved.getName(), cardIds, cardIds.size());
        return ResponseEntity.ok(Map.of("success", true, "deck", dto));
    }

    /** 获取己方牌组列表 */
    @GetMapping("/list")
    public ResponseEntity<?> listDecks(Authentication auth) {
        Long userId = getUserId(auth);
        if (userId == null) return ResponseEntity.status(401).body(Map.of("error", "未认证"));

        List<Deck> decks = deckRepository.findByUserId(userId);
        List<DeckDTO> dtos = decks.stream().map(d -> {
            List<Long> ids = parseCardIds(d.getCardIds());
            return new DeckDTO(d.getId(), d.getName(), ids, ids.size());
        }).collect(Collectors.toList());

        return ResponseEntity.ok(Map.of("success", true, "decks", dtos));
    }

    /** 获取当前使用牌组 */
    @GetMapping("/current")
    public ResponseEntity<?> getCurrentDeck(Authentication auth) {
        Long userId = getUserId(auth);
        if (userId == null) return ResponseEntity.status(401).body(Map.of("error", "未认证"));

        Optional<Deck> active = deckRepository.findByUserIdAndIsActiveTrue(userId);
        if (active.isEmpty()) {
            // 如果没有激活的牌组，取第一套
            List<Deck> decks = deckRepository.findByUserId(userId);
            if (decks.isEmpty()) {
                return ResponseEntity.ok(Map.of("success", true, "deck", null));
            }
            Deck first = decks.get(0);
            first.setIsActive(true);
            deckRepository.save(first);
            active = Optional.of(first);
        }

        Deck d = active.get();
        List<Long> ids = parseCardIds(d.getCardIds());
        DeckDTO dto = new DeckDTO(d.getId(), d.getName(), ids, ids.size());
        return ResponseEntity.ok(Map.of("success", true, "deck", dto));
    }

    /** 选中使用牌组 */
    @PostMapping("/select/{deckId}")
    public ResponseEntity<?> selectDeck(Authentication auth, @PathVariable Long deckId) {
        Long userId = getUserId(auth);
        if (userId == null) return ResponseEntity.status(401).body(Map.of("error", "未认证"));

        Optional<Deck> deckOpt = deckRepository.findById(deckId);
        if (deckOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "牌组不存在"));
        }

        Deck target = deckOpt.get();
        if (!target.getUserId().equals(userId)) {
            return ResponseEntity.status(403).body(Map.of("error", "无权操作"));
        }

        // 取消所有牌组激活
        List<Deck> userDecks = deckRepository.findByUserId(userId);
        for (Deck d : userDecks) {
            if (Boolean.TRUE.equals(d.getIsActive())) {
                d.setIsActive(false);
                deckRepository.save(d);
            }
        }

        // 激活选中的牌组
        target.setIsActive(true);
        deckRepository.save(target);

        List<Long> ids = parseCardIds(target.getCardIds());
        DeckDTO dto = new DeckDTO(target.getId(), target.getName(), ids, ids.size());
        return ResponseEntity.ok(Map.of("success", true, "deck", dto));
    }

    // ====== 工具方法 ======

    private List<Long> parseCardIds(String cardIdsJson) {
        if (cardIdsJson == null || cardIdsJson.isEmpty()) return new ArrayList<>();
        try {
            com.fasterxml.jackson.databind.JsonNode arr = new com.fasterxml.jackson.databind.ObjectMapper().readTree(cardIdsJson);
            List<Long> ids = new ArrayList<>();
            if (arr.isArray()) {
                for (com.fasterxml.jackson.databind.JsonNode n : arr) {
                    ids.add(n.asLong());
                }
            }
            return ids;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}
