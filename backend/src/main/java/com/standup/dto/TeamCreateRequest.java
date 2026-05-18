package com.standup.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TeamCreateRequest {

    @NotBlank(message = "小组名称不能为空")
    private String name;
}
