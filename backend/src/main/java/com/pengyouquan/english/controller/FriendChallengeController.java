package com.pengyouquan.english.controller;

import com.pengyouquan.english.dto.ApiResponse;
import com.pengyouquan.english.security.CurrentUserId;
import com.pengyouquan.english.service.FriendService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 好友切磋控制器
 * 好友之间的实时对战邀请
 */
@RestController
@RequestMapping("/api/friends")
public class FriendChallengeController {

    private final FriendService friendService;

    public FriendChallengeController(FriendService friendService) {
        this.friendService = friendService;
    }

    /** 获取好友列表（含段位、在线状态） */
    @GetMapping("/list")
    public ApiResponse<List<Map<String, Object>>> getFriendList(@CurrentUserId Long userId) {
        if (userId == null) return ApiResponse.unauthorized("未登录");
        return ApiResponse.success(friendService.getFriendListWithDetails(userId));
    }

    /** 发起好友切磋 */
    @PostMapping("/challenge/{friendId}")
    public ApiResponse<Map<String, Object>> sendChallenge(
            @CurrentUserId Long userId,
            @PathVariable Long friendId) {
        if (userId == null) return ApiResponse.unauthorized("未登录");
        try {
            return ApiResponse.success(friendService.sendFriendChallenge(userId, friendId));
        } catch (IllegalStateException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    /** 接受好友切磋 */
    @PostMapping("/challenge/{challengeId}/accept")
    public ApiResponse<Map<String, Object>> acceptChallenge(
            @CurrentUserId Long userId,
            @PathVariable Long challengeId) {
        if (userId == null) return ApiResponse.unauthorized("未登录");
        try {
            return ApiResponse.success(friendService.acceptFriendChallenge(challengeId, userId));
        } catch (IllegalStateException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    /** 拒绝好友切磋 */
    @PostMapping("/challenge/{challengeId}/reject")
    public ApiResponse<Map<String, Object>> rejectChallenge(
            @CurrentUserId Long userId,
            @PathVariable Long challengeId) {
        if (userId == null) return ApiResponse.unauthorized("未登录");
        try {
            return ApiResponse.success(friendService.rejectFriendChallenge(challengeId, userId));
        } catch (IllegalStateException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    /** 获取待处理的切磋邀请（我收到的） */
    @GetMapping("/challenges/pending")
    public ApiResponse<List<Map<String, Object>>> getPendingChallenges(@CurrentUserId Long userId) {
        if (userId == null) return ApiResponse.unauthorized("未登录");
        return ApiResponse.success(friendService.getPendingFriendChallenges(userId));
    }

    /** 获取已接受的切磋（可进入对战） */
    @GetMapping("/challenges/accepted")
    public ApiResponse<List<Map<String, Object>>> getAcceptedChallenges(@CurrentUserId Long userId) {
        if (userId == null) return ApiResponse.unauthorized("未登录");
        return ApiResponse.success(friendService.getAcceptedFriendChallenges(userId));
    }
}
