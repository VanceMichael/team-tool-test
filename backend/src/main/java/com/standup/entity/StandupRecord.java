package com.standup.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("standup_record")
public class StandupRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long teamId;

    private LocalDate recordDate;

    private String yesterday;

    private String today;

    private String blocker;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
