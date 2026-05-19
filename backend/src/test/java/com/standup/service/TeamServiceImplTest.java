package com.standup.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("团队服务测试")
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

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(teamService, "baseMapper", teamMapper);
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        TableInfoHelper.initTableInfo(assistant, Team.class);
        TableInfoHelper.initTableInfo(assistant, TeamMember.class);
        TableInfoHelper.initTableInfo(assistant, StandupRecord.class);
        TableInfoHelper.initTableInfo(assistant, User.class);
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Nested
    @DisplayName("创建团队")
    class CreateTeamTest {

        @Test
        @DisplayName("LEADER角色可创建团队")
        void 创建团队_LEADER角色_应创建成功() {
            UserContext.set(1L, "LEADER");
            TeamCreateRequest request = TestDataFactory.createTeamRequest("新团队");

            when(teamMapper.insert(any(Team.class))).thenAnswer(invocation -> {
                Team team = invocation.getArgument(0);
                team.setId(1L);
                return 1;
            });
            when(teamMemberMapper.insert(any(TeamMember.class))).thenReturn(1);

            Team result = teamService.createTeam(request);

            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("新团队");
            assertThat(result.getLeaderId()).isEqualTo(1L);
            verify(teamMemberMapper).insert(argThat(member ->
                    member.getTeamId().equals(1L) && member.getUserId().equals(1L)
            ));
        }

        @Test
        @DisplayName("ADMIN角色可创建团队")
        void 创建团队_ADMIN角色_应创建成功() {
            UserContext.set(1L, "ADMIN");
            TeamCreateRequest request = TestDataFactory.createTeamRequest("管理员团队");

            when(teamMapper.insert(any(Team.class))).thenAnswer(invocation -> {
                Team team = invocation.getArgument(0);
                team.setId(2L);
                return 1;
            });
            when(teamMemberMapper.insert(any(TeamMember.class))).thenReturn(1);

            Team result = teamService.createTeam(request);

            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("管理员团队");
            verify(teamMemberMapper).insert(any(TeamMember.class));
        }

        @Test
        @DisplayName("普通MEMBER角色不可创建团队")
        void 创建团队_MEMBER角色_应抛出异常() {
            UserContext.set(1L, "MEMBER");
            TeamCreateRequest request = TestDataFactory.createTeamRequest("非法团队");

            assertThatThrownBy(() -> teamService.createTeam(request))
                    .isInstanceOf(BizException.class)
                    .hasMessage("只有组长或管理员才能创建小组");

            verify(teamMapper, never()).insert(any(Team.class));
            verify(teamMemberMapper, never()).insert(any(TeamMember.class));
        }

        @Test
        @DisplayName("创建后自动把创建者加入成员")
        void 创建团队_应自动加入创建者为成员() {
            UserContext.set(10L, "LEADER");
            TeamCreateRequest request = TestDataFactory.createTeamRequest("测试团队");

            when(teamMapper.insert(any(Team.class))).thenAnswer(invocation -> {
                Team team = invocation.getArgument(0);
                team.setId(5L);
                return 1;
            });
            when(teamMemberMapper.insert(any(TeamMember.class))).thenReturn(1);

            teamService.createTeam(request);

            verify(teamMemberMapper).insert(argThat(member ->
                    member.getTeamId().equals(5L) && member.getUserId().equals(10L)
            ));
        }
    }

    @Nested
    @DisplayName("添加成员")
    class AddMemberTest {

        @Test
        @DisplayName("组长可添加成员")
        void 添加成员_组长操作_应添加成功() {
            UserContext.set(1L, "LEADER");
            Team team = TestDataFactory.createTeam(100L, "研发组", 1L);
            User user = TestDataFactory.createMember(2L, "新成员");

            when(teamMapper.selectById(100L)).thenReturn(team);
            when(userMapper.selectById(2L)).thenReturn(user);
            when(teamMemberMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
            when(teamMemberMapper.insert(any(TeamMember.class))).thenReturn(1);

            TeamAddMemberRequest request = TestDataFactory.createAddMemberRequest(100L, 2L);
            assertThatCode(() -> teamService.addMember(request)).doesNotThrowAnyException();

            verify(teamMemberMapper).insert(any(TeamMember.class));
        }

        @Test
        @DisplayName("管理员可添加成员")
        void 添加成员_管理员操作_应添加成功() {
            UserContext.set(99L, "ADMIN");
            Team team = TestDataFactory.createTeam(100L, "研发组", 1L);
            User user = TestDataFactory.createMember(2L, "新成员");

            when(teamMapper.selectById(100L)).thenReturn(team);
            when(userMapper.selectById(2L)).thenReturn(user);
            when(teamMemberMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
            when(teamMemberMapper.insert(any(TeamMember.class))).thenReturn(1);

            TeamAddMemberRequest request = TestDataFactory.createAddMemberRequest(100L, 2L);
            assertThatCode(() -> teamService.addMember(request)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("非组长非管理员不可添加成员")
        void 添加成员_非组长非管理员_应抛出异常() {
            UserContext.set(2L, "MEMBER");
            Team team = TestDataFactory.createTeam(100L, "研发组", 1L);

            when(teamMapper.selectById(100L)).thenReturn(team);

            TeamAddMemberRequest request = TestDataFactory.createAddMemberRequest(100L, 3L);
            assertThatThrownBy(() -> teamService.addMember(request))
                    .isInstanceOf(BizException.class)
                    .hasMessage("只有组长或管理员才能添加成员");

            verify(teamMemberMapper, never()).insert(any(TeamMember.class));
        }

        @Test
        @DisplayName("重复添加成员应抛出异常")
        void 添加成员_重复添加_应抛出异常() {
            UserContext.set(1L, "LEADER");
            Team team = TestDataFactory.createTeam(100L, "研发组", 1L);
            User user = TestDataFactory.createMember(2L, "已有成员");

            when(teamMapper.selectById(100L)).thenReturn(team);
            when(userMapper.selectById(2L)).thenReturn(user);
            when(teamMemberMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

            TeamAddMemberRequest request = TestDataFactory.createAddMemberRequest(100L, 2L);
            assertThatThrownBy(() -> teamService.addMember(request))
                    .isInstanceOf(BizException.class)
                    .hasMessage("该用户已在小组中");

            verify(teamMemberMapper, never()).insert(any(TeamMember.class));
        }

        @Test
        @DisplayName("用户不存在应抛出异常")
        void 添加成员_用户不存在_应抛出异常() {
            UserContext.set(1L, "LEADER");
            Team team = TestDataFactory.createTeam(100L, "研发组", 1L);

            when(teamMapper.selectById(100L)).thenReturn(team);
            when(userMapper.selectById(999L)).thenReturn(null);

            TeamAddMemberRequest request = TestDataFactory.createAddMemberRequest(100L, 999L);
            assertThatThrownBy(() -> teamService.addMember(request))
                    .isInstanceOf(BizException.class)
                    .hasMessage("用户不存在");

            verify(teamMemberMapper, never()).insert(any(TeamMember.class));
        }
    }

    @Nested
    @DisplayName("移除成员")
    class RemoveMemberTest {

        @Test
        @DisplayName("组长可移除成员")
        void 移除成员_组长操作_应移除成功() {
            UserContext.set(1L, "LEADER");
            Team team = TestDataFactory.createTeam(100L, "研发组", 1L);

            when(teamMapper.selectById(100L)).thenReturn(team);
            when(teamMemberMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(1);

            assertThatCode(() -> teamService.removeMember(100L, 2L)).doesNotThrowAnyException();
            verify(teamMemberMapper).delete(any(LambdaQueryWrapper.class));
        }

        @Test
        @DisplayName("管理员可移除成员")
        void 移除成员_管理员操作_应移除成功() {
            UserContext.set(99L, "ADMIN");
            Team team = TestDataFactory.createTeam(100L, "研发组", 1L);

            when(teamMapper.selectById(100L)).thenReturn(team);
            when(teamMemberMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(1);

            assertThatCode(() -> teamService.removeMember(100L, 2L)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("非组长非管理员不可移除成员")
        void 移除成员_非组长非管理员_应抛出异常() {
            UserContext.set(2L, "MEMBER");
            Team team = TestDataFactory.createTeam(100L, "研发组", 1L);

            when(teamMapper.selectById(100L)).thenReturn(team);

            assertThatThrownBy(() -> teamService.removeMember(100L, 3L))
                    .isInstanceOf(BizException.class)
                    .hasMessage("只有组长或管理员才能移除成员");

            verify(teamMemberMapper, never()).delete(any(LambdaQueryWrapper.class));
        }

        @Test
        @DisplayName("小组不存在应抛出异常")
        void 移除成员_小组不存在_应抛出异常() {
            UserContext.set(1L, "LEADER");

            when(teamMapper.selectById(999L)).thenReturn(null);

            assertThatThrownBy(() -> teamService.removeMember(999L, 2L))
                    .isInstanceOf(BizException.class)
                    .hasMessage("小组不存在");
        }
    }

    @Nested
    @DisplayName("获取团队成员")
    class GetTeamMembersTest {

        @Test
        @DisplayName("返回成员列表且submitted字段正确反映今日是否已提交站会")
        void 获取团队成员_有成员_应返回列表含submitted状态() {
            Long teamId = 100L;
            LocalDate today = LocalDate.now();

            TeamMember m1 = TestDataFactory.createTeamMember(1L, teamId, 1L);
            TeamMember m2 = TestDataFactory.createTeamMember(2L, teamId, 2L);
            when(teamMemberMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(m1, m2));

            User user1 = TestDataFactory.createMember(1L, "成员A");
            User user2 = TestDataFactory.createMember(2L, "成员B");
            when(userMapper.selectBatchIds(List.of(1L, 2L))).thenReturn(List.of(user1, user2));

            StandupRecord record = new StandupRecord();
            record.setUserId(1L);
            record.setRecordDate(today);
            when(standupRecordMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(record));

            List<TeamMemberVO> result = teamService.getTeamMembers(teamId);

            assertThat(result).hasSize(2);
            assertThat(result.stream().filter(vo -> vo.getUserId().equals(1L)).findFirst().get().isSubmitted()).isTrue();
            assertThat(result.stream().filter(vo -> vo.getUserId().equals(2L)).findFirst().get().isSubmitted()).isFalse();
        }

        @Test
        @DisplayName("无成员时返回空列表")
        void 获取团队成员_无成员_应返回空列表() {
            when(teamMemberMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

            List<TeamMemberVO> result = teamService.getTeamMembers(100L);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("获取我的团队")
    class GetMyTeamsTest {

        @Test
        @DisplayName("返回用户加入的团队列表")
        void 获取我的团队_有团队_应返回列表() {
            UserContext.set(1L, "MEMBER");

            TeamMember m1 = TestDataFactory.createTeamMember(1L, 10L, 1L);
            TeamMember m2 = TestDataFactory.createTeamMember(2L, 20L, 1L);
            when(teamMemberMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(m1, m2));

            Team t1 = TestDataFactory.createTeam(10L, "团队A", 1L);
            Team t2 = TestDataFactory.createTeam(20L, "团队B", 2L);
            when(teamMapper.selectBatchIds(List.of(10L, 20L))).thenReturn(List.of(t1, t2));

            List<Team> result = teamService.getMyTeams();

            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("未加入任何团队返回空列表")
        void 获取我的团队_无团队_应返回空列表() {
            UserContext.set(1L, "MEMBER");

            when(teamMemberMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

            List<Team> result = teamService.getMyTeams();

            assertThat(result).isEmpty();
        }
    }
}
