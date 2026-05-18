package com.standup.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StandupSubmitRequest {

    @NotNull(message = "小组ID不能为空")
    private Long teamId;

    private String yesterday;

    private String today;

    private String blocker;
}
