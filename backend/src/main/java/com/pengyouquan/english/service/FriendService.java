package com.pengyouquan.english.service;

import com.pengyouquan.english.dto.FriendDTO;
import com.pengyouquan.english.model.Friend;
import com.pengyouquan.english.model.User;
import com.pengyouquan.english.repository.FriendRepository;
import com.pengyouquan.english.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FriendService {

    private final FriendRepository friendRepository;
    private final UserRepository userRepository;

    public FriendService(FriendRepository friendRepository, UserRepository userRepository) {
        this.friendRepository = friendRepository;
        this.userRepository = userRepository;
    }

    /** 搜索用户（非管理员，供添加好友用） */
    public List<FriendDTO> searchUsers(Long userId, String query) {
        if (query == null || query.isBlank()) return List.of();
        List<User> users = userRepository.searchByKeyword(query.trim());
        return users.stream()
                .filter(u -> !u.getId().equals(userId))
                .map(u -> new FriendDTO(null, u.getId(),
                        u.getNickname() != null ? u.getNickname() : "",
                        u.getEmail() != null ? u.getEmail() : "",
                        u.getAvatar() != null ? u.getAvatar() : "",
                        null, null))
                .limit(20)
                .collect(Collectors.toList());
    }

    /** 发送好友请求 */
    @Transactional
    public void sendRequest(Long userId, Long friendId) {
        if (userId.equals(friendId)) {
            throw new IllegalStateException("不能添加自己为好友");
        }
        // 验证对方存在
        userRepository.findById(friendId).orElseThrow(() -> new IllegalStateException("用户不存在"));

        // 检查是否已是好友
        var existing = friendRepository.findByUserIdAndFriendId(userId, friendId);
        if (existing.isPresent()) {
            Friend f = existing.get();
            if ("accepted".equals(f.getStatus())) {
                throw new IllegalStateException("已是好友");
            }
            if ("pending".equals(f.getStatus())) {
                throw new IllegalStateException("已发送过好友请求");
            }
            // blocked 的情况，重新发送
            f.setStatus("pending");
            friendRepository.save(f);
            return;
        }

        // 检查对方是否已向自己发送请求
        var reverse = friendRepository.findByUserIdAndFriendId(friendId, userId);
        if (reverse.isPresent() && "pending".equals(reverse.get().getStatus())) {
            // 互相为好友，自动接受
            reverse.get().setStatus("accepted");
            friendRepository.save(reverse.get());

            Friend f = new Friend();
            f.setUserId(userId);
            f.setFriendId(friendId);
            f.setStatus("accepted");
            friendRepository.save(f);
            return;
        }

        Friend f = new Friend();
        f.setUserId(userId);
        f.setFriendId(friendId);
        f.setStatus("pending");
        friendRepository.save(f);
    }

    /** 接受好友请求 */
    @Transactional
    public void acceptRequest(Long userId, Long requesterId) {
        Friend f = friendRepository.findByUserIdAndFriendIdAndStatus(requesterId, userId, "pending")
                .orElseThrow(() -> new IllegalStateException("没有待处理的好友请求"));
        f.setStatus("accepted");
        friendRepository.save(f);

        // 建立双向关系
        var reverse = friendRepository.findByUserIdAndFriendId(userId, requesterId);
        if (reverse.isEmpty()) {
            Friend rev = new Friend();
            rev.setUserId(userId);
            rev.setFriendId(requesterId);
            rev.setStatus("accepted");
            friendRepository.save(rev);
        } else {
            reverse.get().setStatus("accepted");
            friendRepository.save(reverse.get());
        }
    }

    /** 获取好友列表 */
    public List<FriendDTO> getFriends(Long userId) {
        List<Friend> outgoing = friendRepository.findByUserId(userId);
        return outgoing.stream()
                .filter(f -> "accepted".equals(f.getStatus()))
                .map(f -> {
                    User friend = userRepository.findById(f.getFriendId()).orElse(null);
                    return new FriendDTO(
                            f.getId(),
                            f.getFriendId(),
                            friend != null ? friend.getNickname() : "已注销",
                            friend != null ? friend.getEmail() : "",
                            friend != null ? friend.getAvatar() : "",
                            f.getStatus(),
                            f.getCreatedAt()
                    );
                })
                .collect(Collectors.toList());
    }

    /** 获取待处理的好友请求（别人向我发起的） */
    public List<FriendDTO> getPendingRequests(Long userId) {
        List<Friend> pending = friendRepository.findByFriendIdAndStatus(userId, "pending");
        return pending.stream().map(f -> {
            User requester = userRepository.findById(f.getUserId()).orElse(null);
            return new FriendDTO(
                    f.getId(),
                    f.getUserId(),
                    requester != null ? requester.getNickname() : "已注销",
                    requester != null ? requester.getEmail() : "",
                    requester != null ? requester.getAvatar() : "",
                    f.getStatus(),
                    f.getCreatedAt()
            );
        }).collect(Collectors.toList());
    }
}
