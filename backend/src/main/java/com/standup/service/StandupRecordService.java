package com.standup.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.standup.dto.StandupRecordVO;
import com.standup.dto.StandupSubmitRequest;
import com.standup.entity.StandupRecord;

import java.time.LocalDate;
import java.util.List;

public interface StandupRecordService extends IService<StandupRecord> {

    StandupRecord submit(StandupSubmitRequest request);

    StandupRecord updateRecord(Long recordId, StandupSubmitRequest request);

    List<StandupRecordVO> getTeamRecords(Long teamId, LocalDate date);

    List<StandupRecordVO> getTeamRecordsInRange(Long teamId, LocalDate startDate, LocalDate endDate);

    List<LocalDate> getSubmittedDates(Long teamId, LocalDate startDate, LocalDate endDate);
}
