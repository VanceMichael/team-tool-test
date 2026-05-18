package com.standup.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.standup.common.BizException;
import com.standup.context.UserContext;
import com.standup.dto.StandupRecordVO;
import com.standup.dto.StandupSubmitRequest;
import com.standup.entity.StandupRecord;
import com.standup.entity.User;
import com.standup.mapper.StandupRecordMapper;
import com.standup.mapper.UserMapper;
import com.standup.service.StandupRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class StandupRecordServiceImpl extends ServiceImpl<StandupRecordMapper, StandupRecord> implements StandupRecordService {

    @Autowired
    private UserMapper userMapper;

    private static final long EDIT_WINDOW_HOURS = 1;

    @Override
    public StandupRecord submit(StandupSubmitRequest request) {
        Long currentUserId = UserContext.getUserId();
        LocalDate today = LocalDate.now();

        StandupRecord existing = getOne(
                new LambdaQueryWrapper<StandupRecord>()
                        .eq(StandupRecord::getUserId, currentUserId)
                        .eq(StandupRecord::getTeamId, request.getTeamId())
                        .eq(StandupRecord::getRecordDate, today)
        );

        if (existing != null) {
            throw new BizException("今天已经提交过站会记录");
        }

        StandupRecord record = new StandupRecord();
        record.setUserId(currentUserId);
        record.setTeamId(request.getTeamId());
        record.setRecordDate(today);
        record.setYesterday(request.getYesterday());
        record.setToday(request.getToday());
        record.setBlocker(request.getBlocker());
        save(record);

        return record;
    }

    @Override
    public StandupRecord updateRecord(Long recordId, StandupSubmitRequest request) {
        StandupRecord record = getById(recordId);
        if (record == null) {
            throw new BizException("站会记录不存在");
        }

        Long currentUserId = UserContext.getUserId();
        String currentRole = UserContext.getRole();

        boolean isOwner = record.getUserId().equals(currentUserId);
        boolean isAdmin = "ADMIN".equals(currentRole);

        if (!isOwner && !isAdmin) {
            throw new BizException("只能编辑自己的站会记录");
        }

        if (isOwner && !isAdmin) {
            long hoursSinceCreation = ChronoUnit.HOURS.between(record.getCreateTime(), LocalDateTime.now());
            if (hoursSinceCreation >= EDIT_WINDOW_HOURS) {
                throw new BizException("提交超过1小时，不能再编辑");
            }
        }

        record.setYesterday(request.getYesterday());
        record.setToday(request.getToday());
        record.setBlocker(request.getBlocker());
        updateById(record);

        return record;
    }

    @Override
    public List<StandupRecordVO> getTeamRecords(Long teamId, LocalDate date) {
        List<StandupRecord> records = list(
                new LambdaQueryWrapper<StandupRecord>()
                        .eq(StandupRecord::getTeamId, teamId)
                        .eq(StandupRecord::getRecordDate, date)
                        .orderByAsc(StandupRecord::getId)
        );

        return convertToVO(records);
    }

    @Override
    public List<StandupRecordVO> getTeamRecordsInRange(Long teamId, LocalDate startDate, LocalDate endDate) {
        List<StandupRecord> records = list(
                new LambdaQueryWrapper<StandupRecord>()
                        .eq(StandupRecord::getTeamId, teamId)
                        .between(StandupRecord::getRecordDate, startDate, endDate)
                        .orderByDesc(StandupRecord::getRecordDate)
                        .orderByAsc(StandupRecord::getId)
        );

        return convertToVO(records);
    }

    @Override
    public List<LocalDate> getSubmittedDates(Long teamId, LocalDate startDate, LocalDate endDate) {
        List<StandupRecord> records = list(
                new LambdaQueryWrapper<StandupRecord>()
                        .eq(StandupRecord::getTeamId, teamId)
                        .between(StandupRecord::getRecordDate, startDate, endDate)
                        .select(StandupRecord::getRecordDate)
        );

        return records.stream()
                .map(StandupRecord::getRecordDate)
                .distinct()
                .toList();
    }

    private List<StandupRecordVO> convertToVO(List<StandupRecord> records) {
        if (records.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> userIds = records.stream().map(StandupRecord::getUserId).distinct().toList();
        List<User> users = userMapper.selectBatchIds(userIds);
        Map<Long, User> userMap = users.stream().collect(Collectors.toMap(User::getId, u -> u));

        Long currentUserId = UserContext.getUserId();
        String currentRole = UserContext.getRole();
        boolean isAdmin = "ADMIN".equals(currentRole);

        return records.stream().map(record -> {
            StandupRecordVO vo = new StandupRecordVO();
            vo.setId(record.getId());
            vo.setUserId(record.getUserId());
            vo.setTeamId(record.getTeamId());
            vo.setRecordDate(record.getRecordDate());
            vo.setYesterday(record.getYesterday());
            vo.setToday(record.getToday());
            vo.setBlocker(record.getBlocker());

            User user = userMap.get(record.getUserId());
            if (user != null) {
                vo.setNickname(user.getNickname());
                vo.setAvatar(user.getAvatar());
            }

            if (isAdmin) {
                vo.setEditable(true);
            } else if (record.getUserId().equals(currentUserId)) {
                long hoursSinceCreation = ChronoUnit.HOURS.between(record.getCreateTime(), LocalDateTime.now());
                vo.setEditable(hoursSinceCreation < EDIT_WINDOW_HOURS);
            } else {
                vo.setEditable(false);
            }

            return vo;
        }).toList();
    }
}
