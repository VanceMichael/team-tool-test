package com.standup.dto;

import lombok.Data;

@Data
public class TeamMemberVO {
    private Long userId;
    private String nickname;
    private String avatar;
    private String role;
    private boolean submitted;
}
