package com.standup.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.standup.config.JwtUtil;
import com.standup.dto.TeamAddMemberRequest;
import com.standup.dto.TeamCreateRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("团队Controller集成测试")
class TeamControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtUtil jwtUtil;

    private String generateToken(Long userId, String username, String role) {
        return jwtUtil.generateToken(userId, username, role);
    }

    private Long createTeam(String token, String name) throws Exception {
        TeamCreateRequest req = new TeamCreateRequest();
        req.setName(name);
        MvcResult result = mockMvc.perform(post("/api/teams")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        String response = result.getResponse().getContentAsString();
        return objectMapper.readTree(response).path("data").path("id").asLong();
    }

    @Nested
    @DisplayName("POST /api/teams")
    class CreateTeamTest {

        @Test
        @DisplayName("LEADER角色可创建团队")
        void 创建团队_LEADER角色_应返回200() throws Exception {
            String token = generateToken(1L, "leader1", "LEADER");

            TeamCreateRequest request = new TeamCreateRequest();
            request.setName("新团队");

            mockMvc.perform(post("/api/teams")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.name").value("新团队"));
        }

        @Test
        @DisplayName("MEMBER角色不可创建团队")
        void 创建团队_MEMBER角色_应返回业务错误() throws Exception {
            String token = generateToken(3L, "member1", "MEMBER");

            TeamCreateRequest request = new TeamCreateRequest();
            request.setName("非法团队");

            mockMvc.perform(post("/api/teams")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(1))
                    .andExpect(jsonPath("$.msg").value("只有组长或管理员才能创建小组"));
        }
    }

    @Nested
    @DisplayName("GET /api/teams/{teamId}/members")
    class GetTeamMembersTest {

        @Test
        @DisplayName("返回成员列表含submitted状态")
        void 获取团队成员_有成员_应返回含submitted状态() throws Exception {
            String token = generateToken(1L, "leader1", "LEADER");

            Long teamId = createTeam(token, "成员测试团队");

            mockMvc.perform(get("/api/teams/" + teamId + "/members")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data[0].userId").value(1))
                    .andExpect(jsonPath("$.data[0].submitted").value(false));
        }
    }

    @Nested
    @DisplayName("POST /api/teams/addMember")
    class AddMemberTest {

        @Test
        @DisplayName("组长可添加成员")
        void 添加成员_组长操作_应返回200() throws Exception {
            String token = generateToken(1L, "leader1", "LEADER");

            Long teamId = createTeam(token, "添加成员测试组");

            TeamAddMemberRequest addReq = new TeamAddMemberRequest();
            addReq.setTeamId(teamId);
            addReq.setUserId(2L);

            mockMvc.perform(post("/api/teams/addMember")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(addReq)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));
        }
    }
}
