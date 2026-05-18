package com.standup.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.standup.dto.TeamAddMemberRequest;
import com.standup.dto.TeamCreateRequest;
import com.standup.dto.TeamMemberVO;
import com.standup.entity.Team;

import java.util.List;

public interface TeamService extends IService<Team> {

    Team createTeam(TeamCreateRequest request);

    void addMember(TeamAddMemberRequest request);

    void removeMember(Long teamId, Long userId);

    List<Team> getMyTeams();

    List<TeamMemberVO> getTeamMembers(Long teamId);
}
