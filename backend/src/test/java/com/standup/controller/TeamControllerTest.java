package com.standup.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.standup.config.DataInitializer;
import com.standup.config.JwtUtil;
import com.standup.dto.StandupSubmitRequest;
import com.standup.entity.StandupRecord;
import com.standup.entity.Team;
import com.standup.entity.TeamMember;
import com.standup.entity.User;
import com.standup.mapper.StandupRecordMapper;
import com.standup.mapper.TeamMapper;
import com.standup.mapper.TeamMemberMapper;
import com.standup.mapper.UserMapper;
import com.standup.testutil.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("TeamController 集成测试")
class TeamControllerTest {

    @MockBean
    private DataInitializer dataInitializer;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private TeamMapper teamMapper;

    @Autowired
    private TeamMemberMapper teamMemberMapper;

    @Autowired
    private StandupRecordMapper standupRecordMapper;

    private User leaderUser;
    private User memberUser;
    private Team testTeam;
    private String leaderToken;
    private String memberToken;

    @BeforeEach
    void setUp() {
        leaderUser = TestDataFactory.createLeaderUser(null);
        leaderUser.setUsername("leader_" + System.currentTimeMillis());
        userMapper.insert(leaderUser);

        memberUser = TestDataFactory.createMemberUser(null);
        memberUser.setUsername("member_" + System.currentTimeMillis());
        userMapper.insert(memberUser);

        testTeam = TestDataFactory.createTeam(null, "测试团队", leaderUser.getId());
        teamMapper.insert(testTeam);

        TeamMember leaderMember = TestDataFactory.createTeamMember(null, testTeam.getId(), leaderUser.getId());
        teamMemberMapper.insert(leaderMember);

        TeamMember member = TestDataFactory.createTeamMember(null, testTeam.getId(), memberUser.getId());
        teamMemberMapper.insert(member);

        leaderToken = jwtUtil.generateToken(leaderUser.getId(), leaderUser.getUsername(), leaderUser.getRole());
        memberToken = jwtUtil.generateToken(memberUser.getId(), memberUser.getUsername(), memberUser.getRole());
    }

    @Test
    @DisplayName("获取团队成员_含提交状态_应正确返回")
    void 获取团队成员_含提交状态_应正确返回() throws Exception {
        StandupSubmitRequest request = TestDataFactory.createSubmitRequest(testTeam.getId());
        StandupRecord record = new StandupRecord();
        record.setUserId(leaderUser.getId());
        record.setTeamId(testTeam.getId());
        record.setRecordDate(LocalDate.now());
        record.setYesterday(request.getYesterday());
        record.setToday(request.getToday());
        record.setBlocker(request.getBlocker());
        standupRecordMapper.insert(record);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/teams/{teamId}/members", testTeam.getId())
                        .header("Authorization", "Bearer " + leaderToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].submitted").value(true))
                .andExpect(jsonPath("$.data[1].submitted").value(false));
    }

    @Test
    @DisplayName("获取团队成员_无Token_应返回401")
    void 获取团队成员_无Token_应返回401() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/teams/{teamId}/members", testTeam.getId()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("创建团队_LEADER角色_应成功")
    void 创建团队_LEADER角色_应成功() throws Exception {
        String requestBody = "{\"name\":\"新团队\"}";

        mockMvc.perform(MockMvcRequestBuilders.post("/api/teams")
                        .header("Authorization", "Bearer " + leaderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.name").value("新团队"))
                .andExpect(jsonPath("$.data.leaderId").value(leaderUser.getId()));
    }

    @Test
    @DisplayName("创建团队_MEMBER角色_应返回业务错误")
    void 创建团队_MEMBER角色_应返回业务错误() throws Exception {
        String requestBody = "{\"name\":\"新团队\"}";

        mockMvc.perform(MockMvcRequestBuilders.post("/api/teams")
                        .header("Authorization", "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.msg").value("只有组长或管理员才能创建小组"));
    }

    @Test
    @DisplayName("获取我的团队_应返回列表")
    void 获取我的团队_应返回列表() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/teams/my")
                        .header("Authorization", "Bearer " + leaderToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(1));
    }
}
