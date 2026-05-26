package com.pengyouquan.english.service;

import com.pengyouquan.english.dto.CardPackResult;
import com.pengyouquan.english.dto.CardResponse;
import com.pengyouquan.english.dto.DeckDTO;
import com.pengyouquan.english.model.Card;
import com.pengyouquan.english.model.Deck;
import com.pengyouquan.english.model.UserCard;
import com.pengyouquan.english.model.Show;
import com.pengyouquan.english.repository.CardRepository;
import com.pengyouquan.english.repository.DeckRepository;
import com.pengyouquan.english.repository.ShowRepository;
import com.pengyouquan.english.repository.UserCardRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class CardService {

    private final CardRepository cardRepository;
    private final UserCardRepository userCardRepository;
    private final DeckRepository deckRepository;
    private final ShowRepository showRepository;

    public CardService(CardRepository cardRepository,
                       UserCardRepository userCardRepository,
                       DeckRepository deckRepository,
                       ShowRepository showRepository) {
        this.cardRepository = cardRepository;
        this.userCardRepository = userCardRepository;
        this.deckRepository = deckRepository;
        this.showRepository = showRepository;
    }

    /**
     * 获取某剧集所有可收集卡牌
     */
    public List<CardResponse> getCards(Long showId) {
        List<Card> cards;
        if (showId != null) {
            cards = cardRepository.findByShowId(showId);
        } else {
            cards = cardRepository.findAll();
        }
        return cards.stream()
            .map(c -> CardResponse.fromCard(c, 0, getShowName(c.getShowId())))
            .collect(Collectors.toList());
    }

    /**
     * 获取用户卡牌收集情况
     */
    public List<CardResponse> getCollection(Long userId, Long showId) {
        List<UserCard> userCards;
        if (showId != null) {
            // 先查对该 show 的卡牌
            List<Card> showCards = cardRepository.findByShowId(showId);
            userCards = userCardRepository.findByUserId(userId).stream()
                .filter(uc -> showCards.stream().anyMatch(c -> c.getId().equals(uc.getCardId())))
                .collect(Collectors.toList());
        } else {
            userCards = userCardRepository.findByUserId(userId);
        }

        List<CardResponse> result = new ArrayList<>();
        for (UserCard uc : userCards) {
            Card card = cardRepository.findById(uc.getCardId()).orElse(null);
            if (card != null) {
                result.add(CardResponse.fromCard(card, uc.getQuantity(), getShowName(card.getShowId())));
            }
        }
        // 按稀有度排序: legendary > epic > rare > common
        List<String> rarityOrder = List.of("legendary", "epic", "rare", "common");
        result.sort(Comparator.comparingInt(r -> rarityOrder.indexOf(r.getRarity())));
        return result;
    }

    /**
     * 根据准确率发放卡牌包
     */
    @Transactional
    public CardPackResult grantCardPack(Long userId, Long showId, double accuracy) {
        List<Card> pool = cardRepository.findByShowId(showId);
        if (pool.isEmpty()) {
            throw new IllegalStateException("该剧集暂无卡牌可发放");
        }

        // 根据准确率决定奖励等级
        int epicCount = 0, rareCount = 0, commonCount = 0;
        String packType;

        if (accuracy >= 95) {
            epicCount = 1;
            rareCount = 2;
            commonCount = 2;
            packType = "golden";
        } else if (accuracy >= 80) {
            rareCount = 1;
            commonCount = 4;
            packType = "silver";
        } else if (accuracy >= 60) {
            commonCount = 5;
            packType = "bronze";
        } else {
            commonCount = 3;
            packType = "basic";
        }

        Random rand = new Random();
        List<Card> resultCards = new ArrayList<>();

        // 随机选取史诗卡
        for (int i = 0; i < epicCount; i++) {
            List<Card> epics = pool.stream()
                .filter(c -> "epic".equals(c.getRarity()))
                .collect(Collectors.toList());
            if (!epics.isEmpty()) {
                resultCards.add(epics.get(rand.nextInt(epics.size())));
            }
        }

        // 随机选取稀有卡
        for (int i = 0; i < rareCount; i++) {
            List<Card> rares = pool.stream()
                .filter(c -> "rare".equals(c.getRarity()))
                .collect(Collectors.toList());
            if (!rares.isEmpty()) {
                resultCards.add(rares.get(rand.nextInt(rares.size())));
            }
        }

        // 随机选取普通卡
        for (int i = 0; i < commonCount; i++) {
            List<Card> commons = pool.stream()
                .filter(c -> "common".equals(c.getRarity()))
                .collect(Collectors.toList());
            if (!commons.isEmpty()) {
                resultCards.add(commons.get(rand.nextInt(commons.size())));
            }
        }

        // 插入或更新 user_cards
        List<CardResponse> responses = new ArrayList<>();
        for (Card card : resultCards) {
            Optional<UserCard> existing = userCardRepository.findByUserIdAndCardId(userId, card.getId());
            if (existing.isPresent()) {
                UserCard uc = existing.get();
                uc.setQuantity(uc.getQuantity() + 1);
                userCardRepository.save(uc);
                responses.add(CardResponse.fromCard(card, uc.getQuantity(), getShowName(card.getShowId())));
            } else {
                UserCard uc = new UserCard();
                uc.setUserId(userId);
                uc.setCardId(card.getId());
                uc.setQuantity(1);
                userCardRepository.save(uc);
                responses.add(CardResponse.fromCard(card, 1, getShowName(card.getShowId())));
            }
        }

        return new CardPackResult(responses, packType);
    }

    /**
     * 保存卡组
     */
    @Transactional
    public DeckDTO saveDeck(Long userId, String name, List<Long> cardIds) {
        if (cardIds.size() > 30) {
            throw new IllegalArgumentException("卡组最多 30 张牌");
        }

        // 检查同名传说卡去重
        Map<Long, Long> cardCount = cardIds.stream()
            .collect(Collectors.groupingBy(id -> id, Collectors.counting()));
        for (Map.Entry<Long, Long> entry : cardCount.entrySet()) {
            Card card = cardRepository.findById(entry.getKey()).orElse(null);
            if (card != null && "legendary".equals(card.getRarity()) && entry.getValue() > 1) {
                throw new IllegalArgumentException("同名传说卡只能放 1 张: " + card.getNameCn());
            }
        }

        Deck deck = new Deck();
        deck.setUserId(userId);
        deck.setName(name != null ? name : "未命名卡组");
        // 存储为 JSON 数组字符串
        deck.setCardIds(cardIds.toString());
        deck = deckRepository.save(deck);

        return new DeckDTO(deck.getId(), deck.getName(), cardIds, cardIds.size());
    }

    /**
     * 获取用户所有卡组
     */
    public List<DeckDTO> getDeckList(Long userId) {
        List<Deck> decks = deckRepository.findByUserId(userId);
        return decks.stream()
            .map(d -> {
                List<Long> cardIds = parseCardIds(d.getCardIds());
                return new DeckDTO(d.getId(), d.getName(), cardIds, cardIds.size());
            })
            .collect(Collectors.toList());
    }

    private String getShowName(Long showId) {
        return showRepository.findById(showId)
            .map(Show::getName)
            .orElse("");
    }

    private List<Long> parseCardIds(String cardIdsJson) {
        if (cardIdsJson == null || cardIdsJson.isBlank()) {
            return Collections.emptyList();
        }
        // JSON 格式如 "[1, 2, 3]"
        String trimmed = cardIdsJson.trim();
        if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
            String inner = trimmed.substring(1, trimmed.length() - 1);
            if (inner.isBlank()) return Collections.emptyList();
            return Arrays.stream(inner.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::parseLong)
                .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
