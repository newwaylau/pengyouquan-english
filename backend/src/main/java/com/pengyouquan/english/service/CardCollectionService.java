package com.pengyouquan.english.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pengyouquan.english.model.*;
import com.pengyouquan.english.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class CardCollectionService {

    private final UserCollectionRepository userCollectionRepository;
    private final ExpeditionCardRepository expeditionCardRepository;
    private final ExpeditionRepository expeditionRepository;
    private final ObjectMapper objectMapper;

    public CardCollectionService(UserCollectionRepository userCollectionRepository,
                                 ExpeditionCardRepository expeditionCardRepository,
                                 ExpeditionRepository expeditionRepository,
                                 ObjectMapper objectMapper) {
        this.userCollectionRepository = userCollectionRepository;
        this.expeditionCardRepository = expeditionCardRepository;
        this.expeditionRepository = expeditionRepository;
        this.objectMapper = objectMapper;
    }

    // ==================== 1. 获取用户收藏 ====================

    public List<Map<String, Object>> getCollection(Long userId) {
        List<UserCollection> collections = userCollectionRepository.findByUserId(userId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (UserCollection uc : collections) {
            expeditionCardRepository.findById(uc.getCardId()).ifPresent(card -> {
                Map<String, Object> entry = new HashMap<>();
                entry.put("collectionId", uc.getId());
                entry.put("cardId", card.getId());
                entry.put("cardName", card.getCardName());
                entry.put("cardNameEn", card.getCardNameEn());
                entry.put("cardType", card.getCardType());
                entry.put("rarity", card.getRarity());
                entry.put("cost", card.getCost());
                entry.put("baseDamage", card.getBaseDamage());
                entry.put("baseBlock", card.getBaseBlock());
                entry.put("description", card.getDescription());
                entry.put("isUpgraded", uc.getIsUpgraded());
                entry.put("quantity", uc.getQuantity());
                entry.put("acquiredFrom", uc.getAcquiredFrom());
                result.add(entry);
            });
        }
        return result;
    }

    // ==================== 2. 添加卡到收藏 ====================

    @Transactional
    public UserCollection addCardToCollection(Long userId, Long cardId, String acquiredFrom) {
        if (acquiredFrom == null) acquiredFrom = "reward";
        Optional<UserCollection> existing = userCollectionRepository.findByUserIdAndCardId(userId, cardId);
        if (existing.isPresent()) {
            UserCollection uc = existing.get();
            uc.setQuantity(uc.getQuantity() + 1);
            return userCollectionRepository.save(uc);
        }
        UserCollection uc = new UserCollection();
        uc.setUserId(userId);
        uc.setCardId(cardId);
        uc.setQuantity(1);
        uc.setIsUpgraded(false);
        uc.setAcquiredFrom(acquiredFrom);
        return userCollectionRepository.save(uc);
    }

    // ==================== 3. 战斗奖励卡牌池 ====================

    public List<Map<String, Object>> getCardPoolForReward(Long userId, int count) {
        List<ExpeditionCard> allCards = expeditionCardRepository.findAll();
        // 排除基础卡
        List<ExpeditionCard> pool = allCards.stream()
                .filter(c -> !"basic".equals(c.getRarity()))
                .collect(Collectors.toList());

        Collections.shuffle(pool, new Random());
        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = 0; i < count && i < pool.size(); i++) {
            ExpeditionCard card = pool.get(i);
            Map<String, Object> entry = new HashMap<>();
            entry.put("cardId", card.getId());
            entry.put("cardName", card.getCardName());
            entry.put("cardNameEn", card.getCardNameEn());
            entry.put("cardType", card.getCardType());
            entry.put("rarity", card.getRarity());
            entry.put("cost", card.getCost());
            entry.put("baseDamage", card.getBaseDamage());
            entry.put("baseBlock", card.getBaseBlock());
            entry.put("description", card.getDescription());
            result.add(entry);
        }
        return result;
    }

    // ==================== 4. 将收藏卡加入远征牌组 ====================

    @Transactional
    public Expedition addCardToDeck(Long userId, Long cardId) {
        Expedition exp = expeditionRepository.findByUserIdAndStatus(userId, "in_progress")
                .orElseThrow(() -> new IllegalStateException("没有进行中的远征"));

        Optional<UserCollection> uc = userCollectionRepository.findByUserIdAndCardId(userId, cardId);
        if (uc.isEmpty() || uc.get().getQuantity() <= 0) {
            throw new IllegalStateException("收藏中没有该卡牌");
        }

        // 将卡加入牌组
        List<Long> deck = parseDeck(exp);
        deck.add(cardId);
        try {
            exp.setCurrentDeck(objectMapper.writeValueAsString(deck));
        } catch (Exception e) {
            throw new IllegalStateException("牌组序列化失败");
        }

        // 减少收藏
        UserCollection collection = uc.get();
        collection.setQuantity(collection.getQuantity() - 1);
        userCollectionRepository.save(collection);

        return expeditionRepository.save(exp);
    }

    // ==================== 5. 初始化基础收藏 ====================

    @Transactional
    public void initBasicCollection(Long userId) {
        List<ExpeditionCard> basicCards = expeditionCardRepository.findByRarity("basic");
        for (ExpeditionCard card : basicCards) {
            Optional<UserCollection> existing = userCollectionRepository.findByUserIdAndCardId(userId, card.getId());
            if (existing.isEmpty()) {
                UserCollection uc = new UserCollection();
                uc.setUserId(userId);
                uc.setCardId(card.getId());
                uc.setQuantity(1);
                uc.setIsUpgraded(false);
                uc.setAcquiredFrom("basic");
                userCollectionRepository.save(uc);
            }
        }
    }

    // ==================== 辅助方法 ====================

    private List<Long> parseDeck(Expedition exp) {
        try {
            String deckStr = exp.getCurrentDeck();
            if (deckStr != null && !deckStr.isBlank() && !"[]".equals(deckStr)) {
                return objectMapper.readValue(deckStr, new TypeReference<List<Long>>() {});
            }
            deckStr = exp.getStartingDeck();
            if (deckStr != null && !deckStr.isBlank()) {
                return objectMapper.readValue(deckStr, new TypeReference<List<Long>>() {});
            }
        } catch (Exception e) {
            // ignore
        }
        return new ArrayList<>();
    }
}
