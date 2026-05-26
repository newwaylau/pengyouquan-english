package com.pengyouquan.english.repository;

import com.pengyouquan.english.model.CardTradeRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CardTradeRequestRepository extends JpaRepository<CardTradeRequest, Long> {
    List<CardTradeRequest> findByReceiverIdAndStatus(Long receiverId, String status);
    List<CardTradeRequest> findByRequesterIdAndStatus(Long requesterId, String status);
    List<CardTradeRequest> findByReceiverIdOrRequesterIdOrderByCreatedAtDesc(Long receiverId, Long requesterId);
}
