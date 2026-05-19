package com.standup.testutil;

import com.standup.dto.StandupSubmitRequest;
import com.standup.dto.TeamAddMemberRequest;
import com.standup.dto.TeamCreateRequest;
import com.standup.entity.StandupRecord;
import com.standup.entity.Team;
import com.standup.entity.TeamMember;
import com.standup.entity.User;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class TestDataFactory {

    public static User createUser(Long id, String role) {
        User user = new User();
        user.setId(id);
        user.setUsername("user" + id);
        user.setPassword("password");
        user.setNickname("用户" + id);
        user.setAvatar("");
        user.setRole(role);
        user.setDeleted(0);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        return user;
    }

    public static User createMemberUser(Long id) {
        return createUser(id, "MEMBER");
    }

    public static User createLeaderUser(Long id) {
        return createUser(id, "LEADER");
    }

    public static User createAdminUser(Long id) {
        return createUser(id, "ADMIN");
    }

    public static Team createTeam(Long id, String name, Long leaderId) {
        Team team = new Team();
        team.setId(id);
        team.setName(name);
        team.setLeaderId(leaderId);
        team.setDeleted(0);
        team.setCreateTime(LocalDateTime.now());
        team.setUpdateTime(LocalDateTime.now());
        return team;
    }

    public static TeamMember createTeamMember(Long id, Long teamId, Long userId) {
        TeamMember member = new TeamMember();
        member.setId(id);
        member.setTeamId(teamId);
        member.setUserId(userId);
        member.setDeleted(0);
        member.setCreateTime(LocalDateTime.now());
        member.setUpdateTime(LocalDateTime.now());
        return member;
    }

    public static StandupRecord createRecord(Long id, Long userId, Long teamId, LocalDate recordDate, LocalDateTime createTime) {
        StandupRecord record = new StandupRecord();
        record.setId(id);
        record.setUserId(userId);
        record.setTeamId(teamId);
        record.setRecordDate(recordDate);
        record.setYesterday("昨天完成了A功能");
        record.setToday("今天要做B功能");
        record.setBlocker("没有阻塞");
        record.setDeleted(0);
        record.setCreateTime(createTime);
        record.setUpdateTime(createTime);
        return record;
    }

    public static StandupRecord createRecord(Long id, Long userId, Long teamId, LocalDate recordDate) {
        return createRecord(id, userId, teamId, recordDate, LocalDateTime.now());
    }

    public static StandupSubmitRequest createSubmitRequest(Long teamId) {
        StandupSubmitRequest request = new StandupSubmitRequest();
        request.setTeamId(teamId);
        request.setYesterday("昨天完成了A功能");
        request.setToday("今天要做B功能");
        request.setBlocker("没有阻塞");
        return request;
    }

    public static TeamCreateRequest createTeamCreateRequest(String name) {
        TeamCreateRequest request = new TeamCreateRequest();
        request.setName(name);
        return request;
    }

    public static TeamAddMemberRequest createTeamAddMemberRequest(Long teamId, Long userId) {
        TeamAddMemberRequest request = new TeamAddMemberRequest();
        request.setTeamId(teamId);
        request.setUserId(userId);
        return request;
    }
}
