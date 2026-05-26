package com.pengyouquan.english.controller;

import com.pengyouquan.english.dto.ApiResponse;
import com.pengyouquan.english.security.CurrentUserId;
import com.pengyouquan.english.service.HeroService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/heroes")
public class HeroController {

    private final HeroService heroService;

    public HeroController(HeroService heroService) {
        this.heroService = heroService;
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> getHeroes(@CurrentUserId Long userId) {
        if (userId == null) return ApiResponse.unauthorized("未登录");
        return ApiResponse.success(heroService.getAvailableHeroes(userId));
    }

    @PostMapping("/select/{heroId}")
    public ApiResponse<Map<String, Object>> selectHero(
            @CurrentUserId Long userId,
            @PathVariable Long heroId) {
        if (userId == null) return ApiResponse.unauthorized("未登录");
        try {
            return ApiResponse.success(heroService.selectHero(userId, heroId));
        } catch (IllegalStateException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @GetMapping("/active")
    public ApiResponse<Map<String, Object>> getActiveHero(@CurrentUserId Long userId) {
        if (userId == null) return ApiResponse.unauthorized("未登录");
        return ApiResponse.success(heroService.getActiveHero(userId));
    }

    @PostMapping("/upgrade-skill")
    public ApiResponse<Map<String, Object>> upgradeSkill(@CurrentUserId Long userId) {
        if (userId == null) return ApiResponse.unauthorized("未登录");
        try {
            return ApiResponse.success(heroService.upgradeSkill(userId));
        } catch (IllegalStateException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }
}
