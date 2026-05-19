package com.standup.controller;

import com.standup.entity.StandupRecord;
import com.standup.entity.Team;
import com.standup.entity.TeamMember;
import com.standup.entity.User;
import com.standup.mapper.StandupRecordMapper;
import com.standup.mapper.TeamMapper;
import com.standup.mapper.TeamMemberMapper;
import com.standup.mapper.UserMapper;
import com.standup.testutil.TestUserContextInterceptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Import(TestUserContextInterceptor.class)
class TeamControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private TeamMapper teamMapper;

    @Autowired
    private TeamMemberMapper teamMemberMapper;

    @Autowired
    private StandupRecordMapper standupRecordMapper;

    private Long leaderId;
    private Long memberId;
    private Long teamId;

    @BeforeEach
    void setUp() {
        standupRecordMapper.delete(null);
        teamMemberMapper.delete(null);
        teamMapper.delete(null);
        userMapper.delete(null);

        User user = new User();
        user.setUsername("leader" + System.nanoTime());
        user.setNickname("组长张三");
        user.setRole("LEADER");
        user.setAvatar("");
        user.setPassword("test123");
        user.setDeleted(0);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        userMapper.insert(user);
        leaderId = user.getId();

        User member = new User();
        member.setUsername("member" + System.nanoTime());
        member.setNickname("成员李四");
        member.setRole("MEMBER");
        member.setAvatar("");
        member.setPassword("test123");
        member.setDeleted(0);
        member.setCreateTime(LocalDateTime.now());
        member.setUpdateTime(LocalDateTime.now());
        userMapper.insert(member);
        memberId = member.getId();

        Team team = new Team();
        team.setName("研发一组");
        team.setLeaderId(leaderId);
        team.setDeleted(0);
        team.setCreateTime(LocalDateTime.now());
        team.setUpdateTime(LocalDateTime.now());
        teamMapper.insert(team);
        teamId = team.getId();

        TeamMember m1 = new TeamMember();
        m1.setTeamId(teamId);
        m1.setUserId(leaderId);
        m1.setDeleted(0);
        m1.setCreateTime(LocalDateTime.now());
        m1.setUpdateTime(LocalDateTime.now());
        teamMemberMapper.insert(m1);

        TeamMember m2 = new TeamMember();
        m2.setTeamId(teamId);
        m2.setUserId(memberId);
        m2.setDeleted(0);
        m2.setCreateTime(LocalDateTime.now());
        m2.setUpdateTime(LocalDateTime.now());
        teamMemberMapper.insert(m2);
    }

    private String authHeader(Long uid, String role) {
        return "Bearer Test " + uid + " " + role;
    }

    @Nested
    @DisplayName("GET /api/teams/{teamId}/members 测试")
    class GetTeamMembersTests {

        @Test
        @DisplayName("获取团队成员_带token_应返回成员列表含submitted状态")
        void 带token_应返回成员列表含submitted状态() throws Exception {
            mockMvc.perform(get("/api/teams/" + teamId + "/members")
                            .header("Authorization", authHeader(leaderId, "LEADER")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data", hasSize(2)))
                    .andExpect(jsonPath("$.data[0].userId").exists())
                    .andExpect(jsonPath("$.data[0].submitted").value(false))
                    .andExpect(jsonPath("$.data[1].submitted").value(false));
        }

        @Test
        @DisplayName("获取团队成员_有用户已提交_submitted应为true")
        void 有用户已提交_submitted应为true() throws Exception {
            String submitJson = "{\"teamId\":" + teamId + ",\"yesterday\":\"y\",\"today\":\"t\",\"blocker\":\"\"}";
            mockMvc.perform(post("/api/standup/submit")
                            .header("Authorization", authHeader(leaderId, "LEADER"))
                            .contentType("application/json")
                            .content(submitJson))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/api/teams/" + teamId + "/members")
                            .header("Authorization", authHeader(leaderId, "LEADER")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data", hasSize(2)));
        }

        @Test
        @DisplayName("获取团队成员_无token_应返回401")
        void 无token_应返回401() throws Exception {
            mockMvc.perform(get("/api/teams/" + teamId + "/members"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("POST /api/teams 测试")
    class CreateTeamTests {

        @Test
        @DisplayName("创建小组_LEADER角色_应成功")
        void LEADER角色_应成功() throws Exception {
            mockMvc.perform(post("/api/teams")
                            .header("Authorization", authHeader(leaderId, "LEADER"))
                            .contentType("application/json")
                            .content("{\"name\":\"新团队\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.name").value("新团队"));
        }

        @Test
        @DisplayName("创建小组_MEMBER角色_应返回业务错误")
        void MEMBER角色_应返回业务错误() throws Exception {
            mockMvc.perform(post("/api/teams")
                            .header("Authorization", authHeader(memberId, "MEMBER"))
                            .contentType("application/json")
                            .content("{\"name\":\"新团队\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(1))
                    .andExpect(jsonPath("$.msg").value("只有组长或管理员才能创建小组"));
        }
    }
}
