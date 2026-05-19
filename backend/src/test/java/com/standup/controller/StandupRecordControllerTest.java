package com.standup.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.standup.config.JwtUtil;
import com.standup.dto.StandupSubmitRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("站会记录Controller集成测试")
class StandupRecordControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtUtil jwtUtil;

    private String generateToken(Long userId, String username, String role) {
        return jwtUtil.generateToken(userId, username, role);
    }

    @Nested
    @DisplayName("POST /api/standup/submit")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class SubmitTest {

        @Test
        @Order(1)
        @DisplayName("带合法JWT正常提交返回200")
        void 提交站会_合法token_应返回200() throws Exception {
            String token = generateToken(1L, "leader1", "LEADER");

            StandupSubmitRequest request = new StandupSubmitRequest();
            request.setTeamId(100L);
            request.setYesterday("完成了接口设计");
            request.setToday("开始编码实现");
            request.setBlocker("暂无");

            mockMvc.perform(post("/api/standup/submit")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.teamId").value(100))
                    .andExpect(jsonPath("$.data.yesterday").value("完成了接口设计"));
        }

        @Test
        @DisplayName("无token返回401")
        void 提交站会_无token_应返回401() throws Exception {
            StandupSubmitRequest request = new StandupSubmitRequest();
            request.setTeamId(100L);
            request.setYesterday("工作内容");
            request.setToday("今天计划");
            request.setBlocker("无");

            mockMvc.perform(post("/api/standup/submit")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("重复提交返回业务错误")
        void 提交站会_重复提交_应返回业务错误() throws Exception {
            String token = generateToken(2L, "user2", "MEMBER");

            StandupSubmitRequest request = new StandupSubmitRequest();
            request.setTeamId(200L);
            request.setYesterday("昨天工作");
            request.setToday("今天工作");
            request.setBlocker("无");

            mockMvc.perform(post("/api/standup/submit")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));

            mockMvc.perform(post("/api/standup/submit")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(1))
                    .andExpect(jsonPath("$.msg").value("今天已经提交过站会记录"));
        }
    }

    @Nested
    @DisplayName("GET /api/standup/team/{teamId}/date/{date}")
    class GetTeamRecordsTest {

        @Test
        @DisplayName("按日期查询团队站会记录")
        void 查询团队记录_有数据_应返回记录列表() throws Exception {
            String token = generateToken(3L, "member1", "MEMBER");

            StandupSubmitRequest request = new StandupSubmitRequest();
            request.setTeamId(300L);
            request.setYesterday("工作内容");
            request.setToday("今天计划");
            request.setBlocker("无");

            mockMvc.perform(post("/api/standup/submit")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());

            String today = java.time.LocalDate.now().toString();
            mockMvc.perform(get("/api/standup/team/300/date/" + today)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data").isArray());
        }
    }
}
