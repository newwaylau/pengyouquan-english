package com.pengyouquan.english.controller;

import com.pengyouquan.english.dto.ApiResponse;
import com.pengyouquan.english.dto.FriendDTO;
import com.pengyouquan.english.security.CurrentUserId;
import com.pengyouquan.english.service.FriendService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/friends")
public class FriendController {

    private final FriendService friendService;

    public FriendController(FriendService friendService) {
        this.friendService = friendService;
    }

    /** 搜索用户 */
    @GetMapping("/search")
    public ApiResponse<List<FriendDTO>> searchUsers(@CurrentUserId Long userId, @RequestParam String q) {
        if (userId == null) return ApiResponse.unauthorized("未登录");
        return ApiResponse.success(friendService.searchUsers(userId, q));
    }

    /** 发送好友请求 */
    @PostMapping("/request")
    public ApiResponse<Void> sendRequest(@CurrentUserId Long userId, @RequestBody Map<String, Long> body) {
        if (userId == null) return ApiResponse.unauthorized("未登录");
        try {
            friendService.sendRequest(userId, body.get("friendId"));
            return ApiResponse.success();
        } catch (IllegalStateException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    /** 接受好友请求 */
    @PostMapping("/accept")
    public ApiResponse<Void> acceptRequest(@CurrentUserId Long userId, @RequestBody Map<String, Long> body) {
        if (userId == null) return ApiResponse.unauthorized("未登录");
        try {
            friendService.acceptRequest(userId, body.get("friendId"));
            return ApiResponse.success();
        } catch (IllegalStateException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    /** 获取好友列表 */
    @GetMapping
    public ApiResponse<List<FriendDTO>> getFriends(@CurrentUserId Long userId) {
        if (userId == null) return ApiResponse.unauthorized("未登录");
        return ApiResponse.success(friendService.getFriends(userId));
    }

    /** 获取待处理的好友请求 */
    @GetMapping("/pending")
    public ApiResponse<List<FriendDTO>> getPendingRequests(@CurrentUserId Long userId) {
        if (userId == null) return ApiResponse.unauthorized("未登录");
        return ApiResponse.success(friendService.getPendingRequests(userId));
    }
}
