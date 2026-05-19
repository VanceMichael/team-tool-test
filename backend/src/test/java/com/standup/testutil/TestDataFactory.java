package com.standup.testutil;

import com.standup.entity.StandupRecord;
import com.standup.entity.Team;
import com.standup.entity.TeamMember;
import com.standup.entity.User;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class TestDataFactory {

    public static User createUser(Long id, String username, String nickname, String role) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setNickname(nickname);
        user.setRole(role);
        user.setAvatar("");
        user.setDeleted(0);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        return user;
    }

    public static User createAdminUser(Long id) {
        return createUser(id, "admin" + id, "管理员" + id, "ADMIN");
    }

    public static User createLeaderUser(Long id) {
        return createUser(id, "leader" + id, "组长" + id, "LEADER");
    }

    public static User createMemberUser(Long id) {
        return createUser(id, "member" + id, "成员" + id, "MEMBER");
    }

    public static Team createTeam(Long id, Long leaderId) {
        Team team = new Team();
        team.setId(id);
        team.setName("测试小组" + id);
        team.setLeaderId(leaderId);
        team.setDeleted(0);
        team.setCreateTime(LocalDateTime.now());
        team.setUpdateTime(LocalDateTime.now());
        return team;
    }

    public static TeamMember createTeamMember(Long teamId, Long userId) {
        TeamMember member = new TeamMember();
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
        record.setYesterday("昨天完成了测试");
        record.setToday("今天计划开发");
        record.setBlocker("无");
        record.setDeleted(0);
        record.setCreateTime(LocalDateTime.now());
        record.setUpdateTime(LocalDateTime.now());
        return record;
    }

    public static StandupRecord createRecordWithPastCreateTime(Long id, Long userId, Long teamId, LocalDate recordDate, int hoursAgo) {
        StandupRecord record = createRecord(id, userId, teamId, recordDate);
        record.setCreateTime(LocalDateTime.now().minusHours(hoursAgo));
        return record;
    }
}
