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

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Import(TestUserContextInterceptor.class)
class StandupRecordControllerTest {

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

    private Long userId;
    private Long teamId;

    @BeforeEach
    void setUp() {
        standupRecordMapper.delete(null);
        teamMemberMapper.delete(null);
        teamMapper.delete(null);
        userMapper.delete(null);

        User user = new User();
        user.setUsername("testuser" + System.nanoTime());
        user.setNickname("测试用户");
        user.setRole("MEMBER");
        user.setAvatar("");
        user.setPassword("test123");
        user.setDeleted(0);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        userMapper.insert(user);
        userId = user.getId();

        Team team = new Team();
        team.setName("测试团队");
        team.setLeaderId(userId);
        team.setDeleted(0);
        team.setCreateTime(LocalDateTime.now());
        team.setUpdateTime(LocalDateTime.now());
        teamMapper.insert(team);
        teamId = team.getId();

        TeamMember member = new TeamMember();
        member.setTeamId(teamId);
        member.setUserId(userId);
        member.setDeleted(0);
        member.setCreateTime(LocalDateTime.now());
        member.setUpdateTime(LocalDateTime.now());
        teamMemberMapper.insert(member);
    }

    private String authHeader(Long uid, String role) {
        return "Bearer Test " + uid + " " + role;
    }

    @Nested
    @DisplayName("POST /api/standup/submit 测试")
    class SubmitTests {

        @Test
        @DisplayName("提交站会_带合法JWT_应返回200")
        void 带合法JWT_应返回200() throws Exception {
            String json = "{\"teamId\":" + teamId + ",\"yesterday\":\"昨天完成\",\"today\":\"今天计划\",\"blocker\":\"无\"}";

            mockMvc.perform(post("/api/standup/submit")
                            .header("Authorization", authHeader(userId, "MEMBER"))
                            .contentType("application/json")
                            .content(json))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.userId").value(userId.intValue()))
                    .andExpect(jsonPath("$.data.teamId").value(teamId.intValue()));
        }

        @Test
        @DisplayName("提交站会_无token_应返回401")
        void 无token_应返回401() throws Exception {
            String json = "{\"teamId\":" + teamId + "}";

            mockMvc.perform(post("/api/standup/submit")
                            .contentType("application/json")
                            .content(json))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("提交站会_今天已提交_应返回业务错误")
        void 今天已提交_应返回业务错误() throws Exception {
            String json = "{\"teamId\":" + teamId + ",\"yesterday\":\"昨天完成\",\"today\":\"今天计划\"}";

            mockMvc.perform(post("/api/standup/submit")
                            .header("Authorization", authHeader(userId, "MEMBER"))
                            .contentType("application/json")
                            .content(json))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));

            mockMvc.perform(post("/api/standup/submit")
                            .header("Authorization", authHeader(userId, "MEMBER"))
                            .contentType("application/json")
                            .content(json))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(1))
                    .andExpect(jsonPath("$.msg").value("今天已经提交过站会记录"));
        }
    }

    @Nested
    @DisplayName("GET /api/standup/team/{teamId}/date/{date} 测试")
    class GetTeamRecordsTests {

        @Test
        @DisplayName("获取团队记录_带token_应返回列表")
        void 带token_应返回列表() throws Exception {
            String json = "{\"teamId\":" + teamId + ",\"yesterday\":\"昨天完成\",\"today\":\"今天计划\"}";

            mockMvc.perform(post("/api/standup/submit")
                            .header("Authorization", authHeader(userId, "MEMBER"))
                            .contentType("application/json")
                            .content(json))
                    .andExpect(status().isOk());

            String date = LocalDate.now().toString();
            mockMvc.perform(get("/api/standup/team/" + teamId + "/date/" + date)
                            .header("Authorization", authHeader(userId, "MEMBER")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))));
        }

        @Test
        @DisplayName("获取团队记录_无token_应返回401")
        void 无token_应返回401() throws Exception {
            String date = LocalDate.now().toString();
            mockMvc.perform(get("/api/standup/team/" + teamId + "/date/" + date))
                    .andExpect(status().isUnauthorized());
        }
    }
}
