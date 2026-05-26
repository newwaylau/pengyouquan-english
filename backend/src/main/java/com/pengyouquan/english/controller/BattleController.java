package com.pengyouquan.english.controller;

import com.pengyouquan.english.dto.*;
import com.pengyouquan.english.security.CurrentUserId;
import com.pengyouquan.english.service.BattleService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/battle")
public class BattleController {

    private final BattleService battleService;

    public BattleController(BattleService battleService) {
        this.battleService = battleService;
    }

    /** 发起挑战 */
    @PostMapping("/challenge")
    public ApiResponse<BattleHistoryDTO> challenge(
            @CurrentUserId Long userId,
            @RequestBody ChallengeRequest request) {
        if (userId == null) return ApiResponse.unauthorized("未登录");
        try {
            return ApiResponse.success(battleService.challengePlayer(userId, request.getDefenderId(), request.getDeckId()));
        } catch (IllegalStateException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    /** 获取待处理的挑战 */
    @GetMapping("/pending")
    public ApiResponse<List<BattleHistoryDTO>> getPending(@CurrentUserId Long userId) {
        if (userId == null) return ApiResponse.unauthorized("未登录");
        return ApiResponse.success(battleService.getPendingBattles(userId));
    }

    /** 接受挑战并完成对战 */
    @PostMapping("/{id}/accept")
    public ApiResponse<BattleHistoryDTO> accept(
            @CurrentUserId Long userId,
            @PathVariable Long id,
            @RequestBody BattleResultRequest request) {
        if (userId == null) return ApiResponse.unauthorized("未登录");
        try {
            return ApiResponse.success(battleService.acceptBattle(id, userId, request));
        } catch (IllegalStateException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    /** 获取战报 */
    @GetMapping("/history")
    public ApiResponse<List<BattleHistoryDTO>> getHistory(@CurrentUserId Long userId) {
        if (userId == null) return ApiResponse.unauthorized("未登录");
        return ApiResponse.success(battleService.getBattleHistory(userId));
    }

    /** 获取段位信息 */
    @GetMapping("/rank")
    public ApiResponse<RankInfoDTO> getRank(@CurrentUserId Long userId) {
        if (userId == null) return ApiResponse.unauthorized("未登录");
        return ApiResponse.success(battleService.getRankInfo(userId));
    }

    /** 排行榜 */
    @GetMapping("/leaderboard")
    public ApiResponse<List<RankInfoDTO>> getLeaderboard() {
        return ApiResponse.success(battleService.getLeaderboard());
    }
}
