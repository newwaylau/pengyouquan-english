package com.pengyouquan.english.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ClanMemberVO {
    private Long userId;
    private String nickname;
    private int prestige;
    private int rankTier;
    private String titleCn;
    private String joinDate;
}
