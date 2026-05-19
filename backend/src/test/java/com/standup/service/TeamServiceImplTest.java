package com.standup.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.standup.common.BizException;
import com.standup.context.UserContext;
import com.standup.dto.TeamAddMemberRequest;
import com.standup.dto.TeamCreateRequest;
import com.standup.dto.TeamMemberVO;
import com.standup.entity.StandupRecord;
import com.standup.entity.Team;
import com.standup.entity.TeamMember;
import com.standup.entity.User;
import com.standup.mapper.StandupRecordMapper;
import com.standup.mapper.TeamMapper;
import com.standup.mapper.TeamMemberMapper;
import com.standup.mapper.UserMapper;
import com.standup.service.impl.TeamServiceImpl;
import com.standup.testutil.TestDataFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("TeamServiceImpl 单元测试")
class TeamServiceImplTest {

    @Mock
    private TeamMapper teamMapper;

    @Mock
    private TeamMemberMapper teamMemberMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private StandupRecordMapper standupRecordMapper;

    @InjectMocks
    private TeamServiceImpl teamService;

    private static final Long LEADER_ID = 1L;
    private static final Long MEMBER_ID = 2L;
    private static final Long ADMIN_ID = 99L;
    private static final Long TEAM_ID = 1L;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        ReflectionTestUtils.setField(teamService, "baseMapper", teamMapper);
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    @DisplayName("创建团队_LEADER角色_应成功")
    void 创建团队_LEADER角色_应成功() {
        UserContext.set(LEADER_ID, "LEADER");
        TeamCreateRequest request = TestDataFactory.createTeamCreateRequest("测试团队");

        when(teamMapper.insert(any(Team.class))).thenAnswer(invocation -> {
            Team team = invocation.getArgument(0);
            team.setId(TEAM_ID);
            return 1;
        });
        when(teamMemberMapper.insert(any(TeamMember.class))).thenReturn(1);

        Team result = teamService.createTeam(request);

        assertNotNull(result);
        assertEquals(TEAM_ID, result.getId());
        assertEquals("测试团队", result.getName());
        assertEquals(LEADER_ID, result.getLeaderId());
        verify(teamMapper).insert(any(Team.class));
        verify(teamMemberMapper).insert(any(TeamMember.class));
    }

    @Test
    @DisplayName("创建团队_MEMBER角色_应抛出异常")
    void 创建团队_MEMBER角色_应抛出异常() {
        UserContext.set(MEMBER_ID, "MEMBER");
        TeamCreateRequest request = TestDataFactory.createTeamCreateRequest("测试团队");

        BizException exception = assertThrows(BizException.class, () -> teamService.createTeam(request));
        assertEquals("只有组长或管理员才能创建小组", exception.getMessage());
        verify(teamMapper, never()).insert(any(Team.class));
    }

    @Test
    @DisplayName("创建团队_ADMIN角色_应成功")
    void 创建团队_ADMIN角色_应成功() {
        UserContext.set(ADMIN_ID, "ADMIN");
        TeamCreateRequest request = TestDataFactory.createTeamCreateRequest("管理员团队");

        when(teamMapper.insert(any(Team.class))).thenAnswer(invocation -> {
            Team team = invocation.getArgument(0);
            team.setId(TEAM_ID);
            return 1;
        });
        when(teamMemberMapper.insert(any(TeamMember.class))).thenReturn(1);

        Team result = teamService.createTeam(request);

        assertNotNull(result);
        verify(teamMemberMapper).insert(any(TeamMember.class));
    }

    @Test
    @DisplayName("添加成员_组长操作_应成功")
    void 添加成员_组长操作_应成功() {
        UserContext.set(LEADER_ID, "LEADER");
        Team team = TestDataFactory.createTeam(TEAM_ID, "测试团队", LEADER_ID);
        User user = TestDataFactory.createMemberUser(MEMBER_ID);
        TeamAddMemberRequest request = TestDataFactory.createTeamAddMemberRequest(TEAM_ID, MEMBER_ID);

        when(teamMapper.selectById(TEAM_ID)).thenReturn(team);
        when(userMapper.selectById(MEMBER_ID)).thenReturn(user);
        when(teamMemberMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(teamMemberMapper.insert(any(TeamMember.class))).thenReturn(1);

        assertDoesNotThrow(() -> teamService.addMember(request));
        verify(teamMemberMapper).insert(any(TeamMember.class));
    }

    @Test
    @DisplayName("添加成员_管理员操作_应成功")
    void 添加成员_管理员操作_应成功() {
        UserContext.set(ADMIN_ID, "ADMIN");
        Team team = TestDataFactory.createTeam(TEAM_ID, "测试团队", LEADER_ID);
        User user = TestDataFactory.createMemberUser(MEMBER_ID);
        TeamAddMemberRequest request = TestDataFactory.createTeamAddMemberRequest(TEAM_ID, MEMBER_ID);

        when(teamMapper.selectById(TEAM_ID)).thenReturn(team);
        when(userMapper.selectById(MEMBER_ID)).thenReturn(user);
        when(teamMemberMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(teamMemberMapper.insert(any(TeamMember.class))).thenReturn(1);

        assertDoesNotThrow(() -> teamService.addMember(request));
        verify(teamMemberMapper).insert(any(TeamMember.class));
    }

    @Test
    @DisplayName("添加成员_普通成员操作_应抛出异常")
    void 添加成员_普通成员操作_应抛出异常() {
        UserContext.set(MEMBER_ID, "MEMBER");
        Team team = TestDataFactory.createTeam(TEAM_ID, "测试团队", LEADER_ID);
        TeamAddMemberRequest request = TestDataFactory.createTeamAddMemberRequest(TEAM_ID, 3L);

        when(teamMapper.selectById(TEAM_ID)).thenReturn(team);

        BizException exception = assertThrows(BizException.class, () -> teamService.addMember(request));
        assertEquals("只有组长或管理员才能添加成员", exception.getMessage());
        verify(teamMemberMapper, never()).insert(any(TeamMember.class));
    }

    @Test
    @DisplayName("添加成员_用户已存在_应抛出异常")
    void 添加成员_用户已存在_应抛出异常() {
        UserContext.set(LEADER_ID, "LEADER");
        Team team = TestDataFactory.createTeam(TEAM_ID, "测试团队", LEADER_ID);
        User user = TestDataFactory.createMemberUser(MEMBER_ID);
        TeamAddMemberRequest request = TestDataFactory.createTeamAddMemberRequest(TEAM_ID, MEMBER_ID);

        when(teamMapper.selectById(TEAM_ID)).thenReturn(team);
        when(userMapper.selectById(MEMBER_ID)).thenReturn(user);
        when(teamMemberMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        BizException exception = assertThrows(BizException.class, () -> teamService.addMember(request));
        assertEquals("该用户已在小组中", exception.getMessage());
        verify(teamMemberMapper, never()).insert(any(TeamMember.class));
    }

    @Test
    @DisplayName("添加成员_用户不存在_应抛出异常")
    void 添加成员_用户不存在_应抛出异常() {
        UserContext.set(LEADER_ID, "LEADER");
        Team team = TestDataFactory.createTeam(TEAM_ID, "测试团队", LEADER_ID);
        TeamAddMemberRequest request = TestDataFactory.createTeamAddMemberRequest(TEAM_ID, 999L);

        when(teamMapper.selectById(TEAM_ID)).thenReturn(team);
        when(userMapper.selectById(999L)).thenReturn(null);

        BizException exception = assertThrows(BizException.class, () -> teamService.addMember(request));
        assertEquals("用户不存在", exception.getMessage());
        verify(teamMemberMapper, never()).insert(any(TeamMember.class));
    }

    @Test
    @DisplayName("移除成员_组长操作_应成功")
    void 移除成员_组长操作_应成功() {
        UserContext.set(LEADER_ID, "LEADER");
        Team team = TestDataFactory.createTeam(TEAM_ID, "测试团队", LEADER_ID);

        when(teamMapper.selectById(TEAM_ID)).thenReturn(team);
        when(teamMemberMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(1);

        assertDoesNotThrow(() -> teamService.removeMember(TEAM_ID, MEMBER_ID));
        verify(teamMemberMapper).delete(any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("移除成员_管理员操作_应成功")
    void 移除成员_管理员操作_应成功() {
        UserContext.set(ADMIN_ID, "ADMIN");
        Team team = TestDataFactory.createTeam(TEAM_ID, "测试团队", LEADER_ID);

        when(teamMapper.selectById(TEAM_ID)).thenReturn(team);
        when(teamMemberMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(1);

        assertDoesNotThrow(() -> teamService.removeMember(TEAM_ID, MEMBER_ID));
    }

    @Test
    @DisplayName("移除成员_普通成员操作_应抛出异常")
    void 移除成员_普通成员操作_应抛出异常() {
        UserContext.set(MEMBER_ID, "MEMBER");
        Team team = TestDataFactory.createTeam(TEAM_ID, "测试团队", LEADER_ID);

        when(teamMapper.selectById(TEAM_ID)).thenReturn(team);

        BizException exception = assertThrows(BizException.class, () -> teamService.removeMember(TEAM_ID, 3L));
        assertEquals("只有组长或管理员才能移除成员", exception.getMessage());
        verify(teamMemberMapper, never()).delete(any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("获取团队成员_含提交状态_应正确返回")
    void 获取团队成员_含提交状态_应正确返回() {
        UserContext.set(LEADER_ID, "LEADER");
        TeamMember member1 = TestDataFactory.createTeamMember(1L, TEAM_ID, LEADER_ID);
        TeamMember member2 = TestDataFactory.createTeamMember(2L, TEAM_ID, MEMBER_ID);
        User user1 = TestDataFactory.createLeaderUser(LEADER_ID);
        User user2 = TestDataFactory.createMemberUser(MEMBER_ID);
        StandupRecord record = TestDataFactory.createRecord(1L, LEADER_ID, TEAM_ID, LocalDate.now());

        when(teamMemberMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(member1, member2));
        when(userMapper.selectBatchIds(anyList())).thenReturn(List.of(user1, user2));
        when(standupRecordMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(record));

        List<TeamMemberVO> result = teamService.getTeamMembers(TEAM_ID);

        assertEquals(2, result.size());
        assertTrue(result.get(0).isSubmitted());
        assertFalse(result.get(1).isSubmitted());
    }

    @Test
    @DisplayName("获取团队成员_空团队_应返回空列表")
    void 获取团队成员_空团队_应返回空列表() {
        when(teamMemberMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

        List<TeamMemberVO> result = teamService.getTeamMembers(TEAM_ID);

        assertTrue(result.isEmpty());
        verify(userMapper, never()).selectBatchIds(anyList());
    }

    @Test
    @DisplayName("获取我的团队_有团队_应返回列表")
    void 获取我的团队_有团队_应返回列表() {
        UserContext.set(LEADER_ID, "LEADER");
        TeamMember membership = TestDataFactory.createTeamMember(1L, TEAM_ID, LEADER_ID);
        Team team = TestDataFactory.createTeam(TEAM_ID, "测试团队", LEADER_ID);

        when(teamMemberMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(membership));
        when(teamMapper.selectBatchIds(anyList())).thenReturn(List.of(team));

        List<Team> result = teamService.getMyTeams();

        assertEquals(1, result.size());
        assertEquals("测试团队", result.get(0).getName());
    }

    @Test
    @DisplayName("获取我的团队_无团队_应返回空列表")
    void 获取我的团队_无团队_应返回空列表() {
        UserContext.set(MEMBER_ID, "MEMBER");

        when(teamMemberMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

        List<Team> result = teamService.getMyTeams();

        assertTrue(result.isEmpty());
    }
}
