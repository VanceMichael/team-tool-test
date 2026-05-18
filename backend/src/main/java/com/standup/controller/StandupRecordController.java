package com.standup.controller;

import com.standup.common.Result;
import com.standup.dto.StandupRecordVO;
import com.standup.dto.StandupSubmitRequest;
import com.standup.entity.StandupRecord;
import com.standup.service.StandupRecordService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/standup")
public class StandupRecordController {

    @Autowired
    private StandupRecordService standupRecordService;

    @PostMapping("/submit")
    public Result<StandupRecord> submit(@Valid @RequestBody StandupSubmitRequest request) {
        return Result.ok(standupRecordService.submit(request));
    }

    @PutMapping("/{recordId}")
    public Result<StandupRecord> updateRecord(@PathVariable Long recordId,
                                              @Valid @RequestBody StandupSubmitRequest request) {
        return Result.ok(standupRecordService.updateRecord(recordId, request));
    }

    @GetMapping("/team/{teamId}/date/{date}")
    public Result<List<StandupRecordVO>> getTeamRecordsByDate(
            @PathVariable Long teamId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return Result.ok(standupRecordService.getTeamRecords(teamId, date));
    }

    @GetMapping("/team/{teamId}/range")
    public Result<List<StandupRecordVO>> getTeamRecordsInRange(
            @PathVariable Long teamId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return Result.ok(standupRecordService.getTeamRecordsInRange(teamId, startDate, endDate));
    }

    @GetMapping("/team/{teamId}/submitted-dates")
    public Result<List<LocalDate>> getSubmittedDates(
            @PathVariable Long teamId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return Result.ok(standupRecordService.getSubmittedDates(teamId, startDate, endDate));
    }
}
