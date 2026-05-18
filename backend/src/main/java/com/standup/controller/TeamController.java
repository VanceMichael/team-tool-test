package com.standup.controller;

import com.standup.common.Result;
import com.standup.context.UserContext;
import com.standup.dto.TeamAddMemberRequest;
import com.standup.dto.TeamCreateRequest;
import com.standup.dto.TeamMemberVO;
import com.standup.entity.Team;
import com.standup.service.TeamService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/teams")
public class TeamController {

    @Autowired
    private TeamService teamService;

    @GetMapping("/my")
    public Result<List<Team>> getMyTeams() {
        return Result.ok(teamService.getMyTeams());
    }

    @PostMapping
    public Result<Team> createTeam(@Valid @RequestBody TeamCreateRequest request) {
        return Result.ok(teamService.createTeam(request));
    }

    @PostMapping("/addMember")
    public Result<Void> addMember(@Valid @RequestBody TeamAddMemberRequest request) {
        teamService.addMember(request);
        return Result.ok();
    }

    @DeleteMapping("/{teamId}/members/{userId}")
    public Result<Void> removeMember(@PathVariable Long teamId, @PathVariable Long userId) {
        teamService.removeMember(teamId, userId);
        return Result.ok();
    }

    @GetMapping("/{teamId}/members")
    public Result<List<TeamMemberVO>> getTeamMembers(@PathVariable Long teamId) {
        return Result.ok(teamService.getTeamMembers(teamId));
    }
}
