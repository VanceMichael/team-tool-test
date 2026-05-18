package com.standup.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TeamAddMemberRequest {

    @NotNull(message = "小组ID不能为空")
    private Long teamId;

    @NotNull(message = "用户ID不能为空")
    private Long userId;
}
