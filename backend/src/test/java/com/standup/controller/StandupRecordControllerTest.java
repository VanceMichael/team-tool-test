package com.standup.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.standup.config.JwtUtil;
import com.standup.config.DataInitializer;
import com.standup.dto.StandupSubmitRequest;
import com.standup.entity.Team;
import com.standup.entity.TeamMember;
import com.standup.entity.User;
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

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("StandupRecordController 集成测试")
class StandupRecordControllerTest {

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

    private User testUser;
    private Team testTeam;
    private String validToken;

    @BeforeEach
    void setUp() {
        testUser = TestDataFactory.createMemberUser(null);
        testUser.setUsername("testuser_" + System.currentTimeMillis());
        userMapper.insert(testUser);

        testTeam = TestDataFactory.createTeam(null, "测试团队", testUser.getId());
        teamMapper.insert(testTeam);

        TeamMember member = TestDataFactory.createTeamMember(null, testTeam.getId(), testUser.getId());
        teamMemberMapper.insert(member);

        validToken = jwtUtil.generateToken(testUser.getId(), testUser.getUsername(), testUser.getRole());
    }

    @Test
    @DisplayName("提交站会_带合法Token_应返回200")
    void 提交站会_带合法Token_应返回200() throws Exception {
        StandupSubmitRequest request = TestDataFactory.createSubmitRequest(testTeam.getId());

        mockMvc.perform(MockMvcRequestBuilders.post("/api/standup/submit")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.userId").value(testUser.getId()));
    }

    @Test
    @DisplayName("提交站会_无Token_应返回401")
    void 提交站会_无Token_应返回401() throws Exception {
        StandupSubmitRequest request = TestDataFactory.createSubmitRequest(testTeam.getId());

        mockMvc.perform(MockMvcRequestBuilders.post("/api/standup/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("提交站会_重复提交_应返回业务错误")
    void 提交站会_重复提交_应返回业务错误() throws Exception {
        StandupSubmitRequest request = TestDataFactory.createSubmitRequest(testTeam.getId());

        mockMvc.perform(MockMvcRequestBuilders.post("/api/standup/submit")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(MockMvcRequestBuilders.post("/api/standup/submit")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.msg").value("今天已经提交过站会记录"));
    }

    @Test
    @DisplayName("提交站会_Token格式错误_应返回401")
    void 提交站会_Token格式错误_应返回401() throws Exception {
        StandupSubmitRequest request = TestDataFactory.createSubmitRequest(testTeam.getId());

        mockMvc.perform(MockMvcRequestBuilders.post("/api/standup/submit")
                        .header("Authorization", "InvalidToken")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}
