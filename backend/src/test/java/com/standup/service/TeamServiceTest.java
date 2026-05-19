package com.standup.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.standup.common.BizException;
import com.standup.context.UserContext;
import com.standup.dto.TeamAddMemberRequest;
import com.standup.dto.TeamCreateRequest;
import com.standup.dto.TeamMemberVO;
import com.standup.entity.Team;
import com.standup.entity.TeamMember;
import com.standup.entity.User;
import com.standup.entity.StandupRecord;
import com.standup.mapper.StandupRecordMapper;
import com.standup.mapper.TeamMapper;
import com.standup.mapper.TeamMemberMapper;
import com.standup.mapper.UserMapper;
import com.standup.testutil.TestDataFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TeamServiceTest {

    private TeamService teamService;

    @Mock
    private TeamMapper teamMapper;

    @Mock
    private TeamMemberMapper teamMemberMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private StandupRecordMapper standupRecordMapper;

    private MockedStatic<UserContext> userContextMock;

    @BeforeEach
    void setUp() throws Exception {
        userContextMock = mockStatic(UserContext.class);

        Class<?> implClass = Class.forName("com.standup.service.impl.TeamServiceImpl");
        Object impl = implClass.getDeclaredConstructor().newInstance();

        Field baseMapperField = ServiceImpl.class.getDeclaredField("baseMapper");
        baseMapperField.setAccessible(true);
        baseMapperField.set(impl, teamMapper);

        Field teamMemberMapperField = implClass.getDeclaredField("teamMemberMapper");
        teamMemberMapperField.setAccessible(true);
        teamMemberMapperField.set(impl, teamMemberMapper);

        Field userMapperField = implClass.getDeclaredField("userMapper");
        userMapperField.setAccessible(true);
        userMapperField.set(impl, userMapper);

        Field standupRecordMapperField = implClass.getDeclaredField("standupRecordMapper");
        standupRecordMapperField.setAccessible(true);
        standupRecordMapperField.set(impl, standupRecordMapper);

        teamService = (TeamService) impl;
    }

    @AfterEach
    void tearDown() {
        userContextMock.close();
    }

    private void mockUserContext(Long userId, String role) {
        userContextMock.when(UserContext::getUserId).thenReturn(userId);
        userContextMock.when(UserContext::getRole).thenReturn(role);
    }

    @Nested
    @DisplayName("createTeam 方法测试")
    class CreateTeamTests {

        @Test
        @DisplayName("创建小组_LEADER角色_应成功创建")
        void LEADER角色_应成功创建() {
            Long leaderId = 1L;
            mockUserContext(leaderId, "LEADER");

            TeamCreateRequest request = new TeamCreateRequest();
            request.setName("研发一组");

            when(teamMapper.insert(any(Team.class))).thenReturn(1);
            when(teamMemberMapper.insert(any(TeamMember.class))).thenReturn(1);

            Team result = teamService.createTeam(request);

            assertNotNull(result);
            assertEquals("研发一组", result.getName());
            assertEquals(leaderId, result.getLeaderId());
            verify(teamMemberMapper).insert(any(TeamMember.class));
        }

        @Test
        @DisplayName("创建小组_ADMIN角色_应成功创建")
        void ADMIN角色_应成功创建() {
            Long adminId = 1L;
            mockUserContext(adminId, "ADMIN");

            TeamCreateRequest request = new TeamCreateRequest();
            request.setName("研发一组");

            when(teamMapper.insert(any(Team.class))).thenReturn(1);
            when(teamMemberMapper.insert(any(TeamMember.class))).thenReturn(1);

            Team result = teamService.createTeam(request);

            assertNotNull(result);
            assertEquals("研发一组", result.getName());
            assertEquals(adminId, result.getLeaderId());
        }

        @Test
        @DisplayName("创建小组_MEMBER角色_应抛出异常")
        void MEMBER角色_应抛出异常() {
            mockUserContext(1L, "MEMBER");

            TeamCreateRequest request = new TeamCreateRequest();
            request.setName("研发一组");

            BizException ex = assertThrows(BizException.class, () -> teamService.createTeam(request));
            assertEquals("只有组长或管理员才能创建小组", ex.getMessage());
            verify(teamMapper, never()).insert(any(Team.class));
        }
    }

    @Nested
    @DisplayName("addMember 方法测试")
    class AddMemberTests {

        @Test
        @DisplayName("添加成员_组长添加_应成功")
        void 组长添加_应成功() {
            Long leaderId = 1L;
            Long teamId = 1L;
            Long newMemberId = 2L;
            mockUserContext(leaderId, "LEADER");

            Team team = TestDataFactory.createTeam(teamId, leaderId);
            when(teamMapper.selectById(teamId)).thenReturn(team);
            when(userMapper.selectById(newMemberId)).thenReturn(TestDataFactory.createMemberUser(newMemberId));
            when(teamMemberMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
            when(teamMemberMapper.insert(any(TeamMember.class))).thenReturn(1);

            TeamAddMemberRequest request = new TeamAddMemberRequest();
            request.setTeamId(teamId);
            request.setUserId(newMemberId);

            assertDoesNotThrow(() -> teamService.addMember(request));
            verify(teamMemberMapper).insert(any(TeamMember.class));
        }

        @Test
        @DisplayName("添加成员_管理员添加_应成功")
        void 管理员添加_应成功() {
            Long adminId = 1L;
            Long teamId = 1L;
            Long newMemberId = 2L;
            mockUserContext(adminId, "ADMIN");

            Team team = TestDataFactory.createTeam(teamId, 999L);
            when(teamMapper.selectById(teamId)).thenReturn(team);
            when(userMapper.selectById(newMemberId)).thenReturn(TestDataFactory.createMemberUser(newMemberId));
            when(teamMemberMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
            when(teamMemberMapper.insert(any(TeamMember.class))).thenReturn(1);

            TeamAddMemberRequest request = new TeamAddMemberRequest();
            request.setTeamId(teamId);
            request.setUserId(newMemberId);

            assertDoesNotThrow(() -> teamService.addMember(request));
        }

        @Test
        @DisplayName("添加成员_非组长非管理员_应抛出异常")
        void 非组长非管理员_应抛出异常() {
            Long userId = 99L;
            Long leaderId = 1L;
            Long teamId = 1L;
            Long newMemberId = 2L;
            mockUserContext(userId, "MEMBER");

            Team team = TestDataFactory.createTeam(teamId, leaderId);
            when(teamMapper.selectById(teamId)).thenReturn(team);

            TeamAddMemberRequest request = new TeamAddMemberRequest();
            request.setTeamId(teamId);
            request.setUserId(newMemberId);

            BizException ex = assertThrows(BizException.class, () -> teamService.addMember(request));
            assertEquals("只有组长或管理员才能添加成员", ex.getMessage());
            verify(teamMemberMapper, never()).insert(any(TeamMember.class));
        }

        @Test
        @DisplayName("添加成员_用户不存在_应抛出异常")
        void 用户不存在_应抛出异常() {
            Long leaderId = 1L;
            Long teamId = 1L;
            Long notExistUserId = 999L;
            mockUserContext(leaderId, "LEADER");

            Team team = TestDataFactory.createTeam(teamId, leaderId);
            when(teamMapper.selectById(teamId)).thenReturn(team);
            when(userMapper.selectById(notExistUserId)).thenReturn(null);

            TeamAddMemberRequest request = new TeamAddMemberRequest();
            request.setTeamId(teamId);
            request.setUserId(notExistUserId);

            BizException ex = assertThrows(BizException.class, () -> teamService.addMember(request));
            assertEquals("用户不存在", ex.getMessage());
        }

        @Test
        @DisplayName("添加成员_重复添加_应抛出异常")
        void 重复添加_应抛出异常() {
            Long leaderId = 1L;
            Long teamId = 1L;
            Long memberId = 2L;
            mockUserContext(leaderId, "LEADER");

            Team team = TestDataFactory.createTeam(teamId, leaderId);
            when(teamMapper.selectById(teamId)).thenReturn(team);
            when(userMapper.selectById(memberId)).thenReturn(TestDataFactory.createMemberUser(memberId));
            when(teamMemberMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

            TeamAddMemberRequest request = new TeamAddMemberRequest();
            request.setTeamId(teamId);
            request.setUserId(memberId);

            BizException ex = assertThrows(BizException.class, () -> teamService.addMember(request));
            assertEquals("该用户已在小组中", ex.getMessage());
            verify(teamMemberMapper, never()).insert(any(TeamMember.class));
        }

        @Test
        @DisplayName("添加成员_小组不存在_应抛出异常")
        void 小组不存在_应抛出异常() {
            Long leaderId = 1L;
            Long teamId = 1L;
            mockUserContext(leaderId, "LEADER");

            when(teamMapper.selectById(teamId)).thenReturn(null);

            TeamAddMemberRequest request = new TeamAddMemberRequest();
            request.setTeamId(teamId);
            request.setUserId(2L);

            BizException ex = assertThrows(BizException.class, () -> teamService.addMember(request));
            assertEquals("小组不存在", ex.getMessage());
        }
    }

    @Nested
    @DisplayName("removeMember 方法测试")
    class RemoveMemberTests {

        @Test
        @DisplayName("移除成员_组长移除_应成功")
        void 组长移除_应成功() {
            Long leaderId = 1L;
            Long teamId = 1L;
            Long memberId = 2L;
            mockUserContext(leaderId, "LEADER");

            Team team = TestDataFactory.createTeam(teamId, leaderId);
            when(teamMapper.selectById(teamId)).thenReturn(team);
            when(teamMemberMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(1);

            assertDoesNotThrow(() -> teamService.removeMember(teamId, memberId));
            verify(teamMemberMapper).delete(any(LambdaQueryWrapper.class));
        }

        @Test
        @DisplayName("移除成员_管理员移除_应成功")
        void 管理员移除_应成功() {
            Long adminId = 1L;
            Long teamId = 1L;
            Long memberId = 2L;
            mockUserContext(adminId, "ADMIN");

            Team team = TestDataFactory.createTeam(teamId, 999L);
            when(teamMapper.selectById(teamId)).thenReturn(team);
            when(teamMemberMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(1);

            assertDoesNotThrow(() -> teamService.removeMember(teamId, memberId));
        }

        @Test
        @DisplayName("移除成员_非组长非管理员_应抛出异常")
        void 非组长非管理员_应抛出异常() {
            Long userId = 99L;
            Long leaderId = 1L;
            Long teamId = 1L;
            Long memberId = 2L;
            mockUserContext(userId, "MEMBER");

            Team team = TestDataFactory.createTeam(teamId, leaderId);
            when(teamMapper.selectById(teamId)).thenReturn(team);

            BizException ex = assertThrows(BizException.class, () -> teamService.removeMember(teamId, memberId));
            assertEquals("只有组长或管理员才能移除成员", ex.getMessage());
            verify(teamMemberMapper, never()).delete(any(LambdaQueryWrapper.class));
        }

        @Test
        @DisplayName("移除成员_小组不存在_应抛出异常")
        void 小组不存在_应抛出异常() {
            Long leaderId = 1L;
            Long teamId = 1L;
            mockUserContext(leaderId, "LEADER");

            when(teamMapper.selectById(teamId)).thenReturn(null);

            BizException ex = assertThrows(BizException.class, () -> teamService.removeMember(teamId, 2L));
            assertEquals("小组不存在", ex.getMessage());
        }
    }

    @Nested
    @DisplayName("getTeamMembers 方法测试")
    class GetTeamMembersTests {

        @Test
        @DisplayName("获取团队成员_已提交站会_submitted应为true")
        void 已提交站会_submitted应为true() {
            Long teamId = 1L;
            Long userId = 1L;

            TeamMember member = TestDataFactory.createTeamMember(teamId, userId);
            when(teamMemberMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(member));

            User user = TestDataFactory.createMemberUser(userId);
            when(userMapper.selectBatchIds(anyList())).thenReturn(List.of(user));

            StandupRecord record = TestDataFactory.createRecord(1L, userId, teamId, LocalDate.now());
            when(standupRecordMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(record));

            List<TeamMemberVO> result = teamService.getTeamMembers(teamId);

            assertEquals(1, result.size());
            assertEquals(userId, result.get(0).getUserId());
            assertEquals(user.getNickname(), result.get(0).getNickname());
            assertTrue(result.get(0).isSubmitted());
        }

        @Test
        @DisplayName("获取团队成员_未提交站会_submitted应为false")
        void 未提交站会_submitted应为false() {
            Long teamId = 1L;
            Long userId = 1L;

            TeamMember member = TestDataFactory.createTeamMember(teamId, userId);
            when(teamMemberMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(member));

            User user = TestDataFactory.createMemberUser(userId);
            when(userMapper.selectBatchIds(anyList())).thenReturn(List.of(user));

            when(standupRecordMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

            List<TeamMemberVO> result = teamService.getTeamMembers(teamId);

            assertEquals(1, result.size());
            assertFalse(result.get(0).isSubmitted());
        }

        @Test
        @DisplayName("获取团队成员_空列表_应返回空列表")
        void 空列表_应返回空列表() {
            Long teamId = 1L;
            when(teamMemberMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

            List<TeamMemberVO> result = teamService.getTeamMembers(teamId);

            assertTrue(result.isEmpty());
        }
    }
}
