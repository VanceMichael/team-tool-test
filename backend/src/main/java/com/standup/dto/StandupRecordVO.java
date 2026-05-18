package com.standup.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class StandupRecordVO {
    private Long id;
    private Long userId;
    private Long teamId;
    private LocalDate recordDate;
    private String yesterday;
    private String today;
    private String blocker;
    private String nickname;
    private String avatar;
    private boolean editable;
}
