import { get, post, put, del } from '../utils/request';
import type {
  LoginRequest,
  LoginResponse,
  RegisterRequest,
  UserInfo,
  Team,
  TeamMemberVO,
  StandupRecordVO,
  StandupSubmitRequest,
  TeamCreateRequest,
  TeamAddMemberRequest,
} from '../types';

export const authApi = {
  login: (data: LoginRequest) => post<LoginResponse>('/auth/login', data),
  register: (data: RegisterRequest) => post<void>('/auth/register', data),
};

export const userApi = {
  getMe: () => get<UserInfo>('/user/me'),
};

export const teamApi = {
  getMyTeams: () => get<Team[]>('/teams/my'),
  createTeam: (data: TeamCreateRequest) => post<Team>('/teams', data),
  addMember: (data: TeamAddMemberRequest) => post<void>('/teams/addMember', data),
  removeMember: (teamId: number, userId: number) => del<void>(`/teams/${teamId}/members/${userId}`),
  getTeamMembers: (teamId: number) => get<TeamMemberVO[]>(`/teams/${teamId}/members`),
};

export const standupApi = {
  submit: (data: StandupSubmitRequest) => post<StandupRecordVO>('/standup/submit', data),
  update: (recordId: number, data: StandupSubmitRequest) => put<StandupRecordVO>(`/standup/${recordId}`, data),
  getTeamRecordsByDate: (teamId: number, date: string) =>
    get<StandupRecordVO[]>(`/standup/team/${teamId}/date/${date}`),
  getTeamRecordsInRange: (teamId: number, startDate: string, endDate: string) =>
    get<StandupRecordVO[]>(`/standup/team/${teamId}/range?startDate=${startDate}&endDate=${endDate}`),
  getSubmittedDates: (teamId: number, startDate: string, endDate: string) =>
    get<string[]>(`/standup/team/${teamId}/submitted-dates?startDate=${startDate}&endDate=${endDate}`),
};
