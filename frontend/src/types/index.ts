export interface LoginRequest {
  username: string;
  password: string;
}

export interface RegisterRequest {
  username: string;
  password: string;
  nickname: string;
}

export interface LoginResponse {
  token: string;
  userId: number;
  username: string;
  nickname: string;
  role: string;
  avatar: string;
}

export interface UserInfo {
  id: number;
  username: string;
  nickname: string;
  avatar: string;
  role: string;
}

export interface Team {
  id: number;
  name: string;
  leaderId: number;
}

export interface TeamMemberVO {
  userId: number;
  nickname: string;
  avatar: string;
  role: string;
  submitted: boolean;
}

export interface StandupRecordVO {
  id: number;
  userId: number;
  teamId: number;
  recordDate: string;
  yesterday: string;
  today: string;
  blocker: string;
  nickname: string;
  avatar: string;
  editable: boolean;
}

export interface StandupSubmitRequest {
  teamId: number;
  yesterday: string;
  today: string;
  blocker: string;
}

export interface TeamCreateRequest {
  name: string;
}

export interface TeamAddMemberRequest {
  teamId: number;
  userId: number;
}
