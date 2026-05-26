package com.pengyouquan.english.service;

import com.pengyouquan.english.model.*;
import com.pengyouquan.english.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

/**
 * 公会换卡服务
 * 公会成员之间可以互相交换卡牌，每天限制3次
 */
@Service
public class CardTradeService {

    private final CardTradeRequestRepository tradeRequestRepository;
    private final CardTradeDailyLimitRepository dailyLimitRepository;
    private final UserCardRepository userCardRepository;
    private final CardRepository cardRepository;
    private final UserRepository userRepository;
    private final GuildMemberRepository guildMemberRepository;
    private final FriendRepository friendRepository;

    private static final int DAILY_TRADE_LIMIT = 3;

    public CardTradeService(CardTradeRequestRepository tradeRequestRepository,
                            CardTradeDailyLimitRepository dailyLimitRepository,
                            UserCardRepository userCardRepository,
                            CardRepository cardRepository,
                            UserRepository userRepository,
                            GuildMemberRepository guildMemberRepository,
                            FriendRepository friendRepository) {
        this.tradeRequestRepository = tradeRequestRepository;
        this.dailyLimitRepository = dailyLimitRepository;
        this.userCardRepository = userCardRepository;
        this.cardRepository = cardRepository;
        this.userRepository = userRepository;
        this.guildMemberRepository = guildMemberRepository;
        this.friendRepository = friendRepository;
    }

    // ==================== 发送换卡请求 ====================

    @Transactional
    public Map<String, Object> sendTradeRequest(Long requesterId, Long receiverId, Long requestedCardId, Long offeredCardId) {
        // 检查是否同公会
        GuildMember requesterGuild = guildMemberRepository.findByUserId(requesterId)
                .orElseThrow(() -> new IllegalStateException("你未加入公会"));
        GuildMember receiverGuild = guildMemberRepository.findByUserId(receiverId)
                .orElseThrow(() -> new IllegalStateException("对方未加入公会"));
        if (!requesterGuild.getGuildId().equals(receiverGuild.getGuildId())) {
            throw new IllegalStateException("只能与本公会成员换卡");
        }

        // 检查自己是否有足够的卡牌（如果提供了交换卡）
        if (offeredCardId != null) {
            UserCard myCard = userCardRepository.findByUserIdAndCardId(requesterId, offeredCardId)
                    .orElseThrow(() -> new IllegalStateException("你没有这张可提供的卡牌"));
            if (myCard.getQuantity() < 1) {
                throw new IllegalStateException("你没有足够的卡牌数量用于交换");
            }
        }

        // 检查对方是否有被请求的卡牌
        UserCard receiverCard = userCardRepository.findByUserIdAndCardId(receiverId, requestedCardId)
                .orElseThrow(() -> new IllegalStateException("对方没有你想要的卡牌"));
        if (receiverCard.getQuantity() < 1) {
            throw new IllegalStateException("对方没有足够的卡牌数量");
        }

        // 检查是否已有待处理的请求
        List<CardTradeRequest> existing = tradeRequestRepository
                .findByRequesterIdAndStatus(requesterId, "pending");
        boolean hasPending = existing.stream()
                .anyMatch(r -> r.getReceiverId().equals(receiverId)
                        && r.getRequestedCardId().equals(requestedCardId));
        if (hasPending) {
            throw new IllegalStateException("已向该玩家发送过相同卡牌的请求");
        }

        CardTradeRequest request = new CardTradeRequest();
        request.setRequesterId(requesterId);
        request.setReceiverId(receiverId);
        request.setRequestedCardId(requestedCardId);
        request.setOfferedCardId(offeredCardId);
        request.setStatus("pending");
        tradeRequestRepository.save(request);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("tradeId", request.getId());
        return result;
    }

    // ==================== 接受换卡请求 ====================

    @Transactional
    public Map<String, Object> acceptTradeRequest(Long tradeId, Long userId) {
        CardTradeRequest request = tradeRequestRepository.findById(tradeId)
                .orElseThrow(() -> new IllegalStateException("换卡请求不存在"));

        if (!request.getReceiverId().equals(userId)) {
            throw new IllegalStateException("无权操作此请求");
        }

        if (!"pending".equals(request.getStatus())) {
            throw new IllegalStateException("该请求已处理");
        }

        // 检查每日限制
        checkDailyLimit(userId);
        checkDailyLimit(request.getRequesterId());

        // 执行卡牌转移
        // 1. 被请求的卡牌：接收方→请求方
        transferCard(request.getReceiverId(), request.getRequesterId(), request.getRequestedCardId());

        // 2. 提供的卡牌：请求方→接收方（如果有）
        if (request.getOfferedCardId() != null) {
            transferCard(request.getRequesterId(), request.getReceiverId(), request.getOfferedCardId());
        }

        // 更新状态
        request.setStatus("accepted");
        tradeRequestRepository.save(request);

        // 增加每日计数
        incrementDailyCount(userId);
        incrementDailyCount(request.getRequesterId());

        // 获取卡牌名称用于返回
        Card requestedCard = cardRepository.findById(request.getRequestedCardId()).orElse(null);
        Card offeredCard = request.getOfferedCardId() != null
                ? cardRepository.findById(request.getOfferedCardId()).orElse(null)
                : null;

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "换卡成功！获得 " + (requestedCard != null ? requestedCard.getNameCn() : "卡牌"));
        if (offeredCard != null) {
            result.put("gaveAway", offeredCard.getNameCn());
        }
        return result;
    }

    // ==================== 拒绝/取消换卡请求 ====================

    @Transactional
    public Map<String, Object> rejectTradeRequest(Long tradeId, Long userId) {
        CardTradeRequest request = tradeRequestRepository.findById(tradeId)
                .orElseThrow(() -> new IllegalStateException("换卡请求不存在"));

        if (!request.getReceiverId().equals(userId) && !request.getRequesterId().equals(userId)) {
            throw new IllegalStateException("无权操作此请求");
        }

        if (!"pending".equals(request.getStatus())) {
            throw new IllegalStateException("该请求已处理");
        }

        request.setStatus("rejected");
        tradeRequestRepository.save(request);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        return result;
    }

    // ==================== 查询接口 ====================

    /** 获取收到的换卡请求 */
    public List<Map<String, Object>> getReceivedRequests(Long userId) {
        List<CardTradeRequest> requests = tradeRequestRepository
                .findByReceiverIdAndStatus(userId, "pending");
        return requests.stream().map(this::toTradeDTO).toList();
    }

    /** 获取发送的换卡请求 */
    public List<Map<String, Object>> getSentRequests(Long userId) {
        List<CardTradeRequest> requests = tradeRequestRepository
                .findByRequesterIdAndStatus(userId, "pending");
        return requests.stream().map(this::toTradeDTO).toList();
    }

    /** 获取交易历史 */
    public List<Map<String, Object>> getTradeHistory(Long userId) {
        List<CardTradeRequest> requests = tradeRequestRepository
                .findByReceiverIdOrRequesterIdOrderByCreatedAtDesc(userId, userId);
        return requests.stream().map(this::toTradeDTO).toList();
    }

    /** 获取剩余换卡次数 */
    public Map<String, Object> getDailyLimit(Long userId) {
        CardTradeDailyLimit limit = dailyLimitRepository
                .findByUserIdAndTradeDate(userId, LocalDate.now())
                .orElse(null);
        int used = limit != null ? limit.getTradeCount() : 0;
        int remaining = Math.max(0, DAILY_TRADE_LIMIT - used);

        Map<String, Object> result = new HashMap<>();
        result.put("used", used);
        result.put("remaining", remaining);
        result.put("limit", DAILY_TRADE_LIMIT);
        return result;
    }

    // ==================== 私用方法 ====================

    private void transferCard(Long fromUserId, Long toUserId, Long cardId) {
        // 减少发送方数量
        UserCard fromCard = userCardRepository.findByUserIdAndCardId(fromUserId, cardId)
                .orElseThrow(() -> new IllegalStateException("卡片不存在"));
        fromCard.setQuantity(fromCard.getQuantity() - 1);
        if (fromCard.getQuantity() <= 0) {
            userCardRepository.delete(fromCard);
        } else {
            userCardRepository.save(fromCard);
        }

        // 增加接收方数量
        UserCard toCard = userCardRepository.findByUserIdAndCardId(toUserId, cardId)
                .orElseGet(() -> {
                    UserCard newCard = new UserCard();
                    newCard.setUserId(toUserId);
                    newCard.setCardId(cardId);
                    newCard.setQuantity(0);
                    newCard.setIsGolden(false);
                    return newCard;
                });
        toCard.setQuantity(toCard.getQuantity() + 1);
        userCardRepository.save(toCard);
    }

    private void checkDailyLimit(Long userId) {
        CardTradeDailyLimit limit = dailyLimitRepository
                .findByUserIdAndTradeDate(userId, LocalDate.now())
                .orElse(null);
        int used = limit != null ? limit.getTradeCount() : 0;
        if (used >= DAILY_TRADE_LIMIT) {
            throw new IllegalStateException("今日换卡次数已达上限（" + DAILY_TRADE_LIMIT + "次/天）");
        }
    }

    private void incrementDailyCount(Long userId) {
        CardTradeDailyLimit limit = dailyLimitRepository
                .findByUserIdAndTradeDate(userId, LocalDate.now())
                .orElseGet(() -> {
                    CardTradeDailyLimit newLimit = new CardTradeDailyLimit();
                    newLimit.setUserId(userId);
                    newLimit.setTradeDate(LocalDate.now());
                    newLimit.setTradeCount(0);
                    return newLimit;
                });
        limit.setTradeCount(limit.getTradeCount() + 1);
        dailyLimitRepository.save(limit);
    }

    private Map<String, Object> toTradeDTO(CardTradeRequest request) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", request.getId());
        dto.put("requesterId", request.getRequesterId());
        dto.put("receiverId", request.getReceiverId());
        dto.put("requestedCardId", request.getRequestedCardId());
        dto.put("offeredCardId", request.getOfferedCardId());
        dto.put("status", request.getStatus());
        dto.put("createdAt", request.getCreatedAt());

        // 卡牌名称
        Card reqCard = cardRepository.findById(request.getRequestedCardId()).orElse(null);
        dto.put("requestedCardName", reqCard != null ? reqCard.getNameCn() : "未知");

        if (request.getOfferedCardId() != null) {
            Card offerCard = cardRepository.findById(request.getOfferedCardId()).orElse(null);
            dto.put("offeredCardName", offerCard != null ? offerCard.getNameCn() : "未知");
        }

        // 用户昵称
        User requester = userRepository.findById(request.getRequesterId()).orElse(null);
        dto.put("requesterName", requester != null ? requester.getNickname() : "未知");
        User receiver = userRepository.findById(request.getReceiverId()).orElse(null);
        dto.put("receiverName", receiver != null ? receiver.getNickname() : "未知");

        return dto;
    }
}
