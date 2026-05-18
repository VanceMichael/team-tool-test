package com.standup.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.standup.entity.Team;
import com.standup.entity.TeamMember;
import com.standup.entity.User;
import com.standup.mapper.TeamMapper;
import com.standup.mapper.TeamMemberMapper;
import com.standup.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private TeamMapper teamMapper;

    @Autowired
    private TeamMemberMapper teamMemberMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        initUsers();
        initTeam();
    }

    private void initUsers() {
        createUserIfNotExists("admin", "123456", "管理员", "ADMIN");
        createUserIfNotExists("leader1", "123456", "组长张三", "LEADER");
        createUserIfNotExists("member1", "123456", "成员李四", "MEMBER");
        createUserIfNotExists("member2", "123456", "成员王五", "MEMBER");
    }

    private void createUserIfNotExists(String username, String rawPassword, String nickname, String role) {
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getUsername, username)
        );
        if (count == 0) {
            User user = new User();
            user.setUsername(username);
            user.setPassword(passwordEncoder.encode(rawPassword));
            user.setNickname(nickname);
            user.setRole(role);
            userMapper.insert(user);
        }
    }

    private void initTeam() {
        Long teamCount = teamMapper.selectCount(null);
        if (teamCount > 0) return;

        User leader = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, "leader1")
        );
        if (leader == null) return;

        Team team = new Team();
        team.setName("研发一组");
        team.setLeaderId(leader.getId());
        teamMapper.insert(team);

        User member1 = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, "member1")
        );
        User member2 = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, "member2")
        );

        addMemberToTeam(team.getId(), leader.getId());
        if (member1 != null) addMemberToTeam(team.getId(), member1.getId());
        if (member2 != null) addMemberToTeam(team.getId(), member2.getId());
    }

    private void addMemberToTeam(Long teamId, Long userId) {
        Long existing = teamMemberMapper.selectCount(
                new LambdaQueryWrapper<TeamMember>()
                        .eq(TeamMember::getTeamId, teamId)
                        .eq(TeamMember::getUserId, userId)
        );
        if (existing == 0) {
            TeamMember member = new TeamMember();
            member.setTeamId(teamId);
            member.setUserId(userId);
            teamMemberMapper.insert(member);
        }
    }
}
