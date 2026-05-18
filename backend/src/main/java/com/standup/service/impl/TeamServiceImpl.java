package com.standup.service.impl;

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
import com.standup.mapper.TeamMapper;
import com.standup.mapper.TeamMemberMapper;
import com.standup.mapper.UserMapper;
import com.standup.service.TeamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.standup.entity.StandupRecord;
import com.standup.mapper.StandupRecordMapper;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class TeamServiceImpl extends ServiceImpl<TeamMapper, Team> implements TeamService {

    @Autowired
    private TeamMemberMapper teamMemberMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private StandupRecordMapper standupRecordMapper;

    @Override
    public Team createTeam(TeamCreateRequest request) {
        Long currentUserId = UserContext.getUserId();
        String currentRole = UserContext.getRole();

        if (!"LEADER".equals(currentRole) && !"ADMIN".equals(currentRole)) {
            throw new BizException("只有组长或管理员才能创建小组");
        }

        Team team = new Team();
        team.setName(request.getName());
        team.setLeaderId(currentUserId);
        save(team);

        TeamMember member = new TeamMember();
        member.setTeamId(team.getId());
        member.setUserId(currentUserId);
        teamMemberMapper.insert(member);

        return team;
    }

    @Override
    public void addMember(TeamAddMemberRequest request) {
        Team team = getById(request.getTeamId());
        if (team == null) {
            throw new BizException("小组不存在");
        }

        Long currentUserId = UserContext.getUserId();
        String currentRole = UserContext.getRole();

        if (!"ADMIN".equals(currentRole) && !team.getLeaderId().equals(currentUserId)) {
            throw new BizException("只有组长或管理员才能添加成员");
        }

        User user = userMapper.selectById(request.getUserId());
        if (user == null) {
            throw new BizException("用户不存在");
        }

        Long existingCount = teamMemberMapper.selectCount(
                new LambdaQueryWrapper<TeamMember>()
                        .eq(TeamMember::getTeamId, request.getTeamId())
                        .eq(TeamMember::getUserId, request.getUserId())
        );
        if (existingCount > 0) {
            throw new BizException("该用户已在小组中");
        }

        TeamMember member = new TeamMember();
        member.setTeamId(request.getTeamId());
        member.setUserId(request.getUserId());
        teamMemberMapper.insert(member);
    }

    @Override
    public void removeMember(Long teamId, Long userId) {
        Team team = getById(teamId);
        if (team == null) {
            throw new BizException("小组不存在");
        }

        Long currentUserId = UserContext.getUserId();
        String currentRole = UserContext.getRole();

        if (!"ADMIN".equals(currentRole) && !team.getLeaderId().equals(currentUserId)) {
            throw new BizException("只有组长或管理员才能移除成员");
        }

        teamMemberMapper.delete(
                new LambdaQueryWrapper<TeamMember>()
                        .eq(TeamMember::getTeamId, teamId)
                        .eq(TeamMember::getUserId, userId)
        );
    }

    @Override
    public List<Team> getMyTeams() {
        Long currentUserId = UserContext.getUserId();
        List<TeamMember> memberships = teamMemberMapper.selectList(
                new LambdaQueryWrapper<TeamMember>().eq(TeamMember::getUserId, currentUserId)
        );

        if (memberships.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> teamIds = memberships.stream().map(TeamMember::getTeamId).toList();
        return listByIds(teamIds);
    }

    @Override
    public List<TeamMemberVO> getTeamMembers(Long teamId) {
        List<TeamMember> memberships = teamMemberMapper.selectList(
                new LambdaQueryWrapper<TeamMember>().eq(TeamMember::getTeamId, teamId)
        );

        if (memberships.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> userIds = memberships.stream().map(TeamMember::getUserId).toList();
        List<User> users = userMapper.selectBatchIds(userIds);

        LocalDate today = LocalDate.now();

        List<StandupRecord> todayRecords = standupRecordMapper.selectList(
                new LambdaQueryWrapper<StandupRecord>()
                        .eq(StandupRecord::getTeamId, teamId)
                        .eq(StandupRecord::getRecordDate, today)
        );
        Set<Long> submittedUserIds = todayRecords.stream()
                .map(StandupRecord::getUserId)
                .collect(Collectors.toSet());

        return users.stream().map(user -> {
            TeamMemberVO vo = new TeamMemberVO();
            vo.setUserId(user.getId());
            vo.setNickname(user.getNickname());
            vo.setAvatar(user.getAvatar());
            vo.setRole(user.getRole());
            vo.setSubmitted(submittedUserIds.contains(user.getId()));
            return vo;
        }).toList();
    }
}
