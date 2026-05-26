package com.pengyouquan.english.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class FriendDTO {
    private Long id;
    private Long friendId;
    private String friendNickname;
    private String friendEmail;
    private String friendAvatar;
    private String status;
    private LocalDateTime createdAt;
}
