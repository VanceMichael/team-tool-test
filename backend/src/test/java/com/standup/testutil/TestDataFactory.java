package com.standup.testutil;

import com.standup.entity.StandupRecord;
import com.standup.entity.Team;
import com.standup.entity.TeamMember;
import com.standup.entity.User;
import com.standup.dto.StandupSubmitRequest;
import com.standup.dto.TeamCreateRequest;
import com.standup.dto.TeamAddMemberRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class TestDataFactory {

    public static User createUser(Long id, String username, String nickname, String role) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setPassword("encoded_password");
        user.setNickname(nickname);
        user.setAvatar("");
        user.setRole(role);
        user.setDeleted(0);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        return user;
    }

    public static User createMember(Long id, String nickname) {
        return createUser(id, "user" + id, nickname, "MEMBER");
    }

    public static User createLeader(Long id, String nickname) {
        return createUser(id, "leader" + id, nickname, "LEADER");
    }

    public static User createAdmin(Long id, String nickname) {
        return createUser(id, "admin" + id, nickname, "ADMIN");
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

    public static StandupRecord createRecord(Long id, Long userId, Long teamId, LocalDate recordDate) {
        StandupRecord record = new StandupRecord();
        record.setId(id);
        record.setUserId(userId);
        record.setTeamId(teamId);
        record.setRecordDate(recordDate);
        record.setYesterday("昨天的工作");
        record.setToday("今天的工作");
        record.setBlocker("无阻塞");
        record.setDeleted(0);
        record.setCreateTime(LocalDateTime.now());
        record.setUpdateTime(LocalDateTime.now());
        return record;
    }

    public static StandupRecord createRecord(Long id, Long userId, Long teamId, LocalDate recordDate, LocalDateTime createTime) {
        StandupRecord record = createRecord(id, userId, teamId, recordDate);
        record.setCreateTime(createTime);
        return record;
    }

    public static StandupSubmitRequest createSubmitRequest(Long teamId) {
        StandupSubmitRequest request = new StandupSubmitRequest();
        request.setTeamId(teamId);
        request.setYesterday("昨天完成了需求分析");
        request.setToday("今天开始编码");
        request.setBlocker("暂无");
        return request;
    }

    public static TeamCreateRequest createTeamRequest(String name) {
        TeamCreateRequest request = new TeamCreateRequest();
        request.setName(name);
        return request;
    }

    public static TeamAddMemberRequest createAddMemberRequest(Long teamId, Long userId) {
        TeamAddMemberRequest request = new TeamAddMemberRequest();
        request.setTeamId(teamId);
        request.setUserId(userId);
        return request;
    }
}
