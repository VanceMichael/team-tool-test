import { useState, useEffect } from 'react';
import { useAuth } from '../hooks/useAuth';
import { teamApi, standupApi } from '../api';
import type { Team, TeamMemberVO, StandupRecordVO } from '../types';
import StandupModal from '../components/StandupModal';
import CalendarView from '../components/CalendarView';
import TeamManagement from '../components/TeamManagement';

export default function StandupPage() {
  const { userInfo, logout, isAdmin, isLeader } = useAuth();
  const [teams, setTeams] = useState<Team[]>([]);
  const [selectedTeam, setSelectedTeam] = useState<Team | null>(null);
  const [members, setMembers] = useState<TeamMemberVO[]>([]);
  const [todayRecords, setTodayRecords] = useState<StandupRecordVO[]>([]);
  const [modalOpen, setModalOpen] = useState(false);
  const [editingRecord, setEditingRecord] = useState<StandupRecordVO | null>(null);
  const [viewMode, setViewMode] = useState<'today' | 'calendar'>('today');
  const [showTeamMgmt, setShowTeamMgmt] = useState(false);

  const today = new Date().toISOString().split('T')[0];

  const loadTeams = async () => {
    try {
      const data = await teamApi.getMyTeams();
      setTeams(data);
      if (data.length > 0) {
        setSelectedTeam((prev) => {
          if (!prev) return data[0];
          const stillExists = data.find((t) => t.id === prev.id);
          return stillExists || data[0];
        });
      }
    } catch (err) {
      console.error(err);
    }
  };

  const loadTodayData = async () => {
    if (!selectedTeam) return;
    try {
      const [memberList, records] = await Promise.all([
        teamApi.getTeamMembers(selectedTeam.id),
        standupApi.getTeamRecordsByDate(selectedTeam.id, today),
      ]);
      setMembers(memberList);
      setTodayRecords(records);
    } catch (err) {
      console.error(err);
    }
  };

  useEffect(() => {
    loadTeams();
  }, []);

  useEffect(() => {
    loadTodayData();
  }, [selectedTeam]);

  const getMyTodayRecord = () => {
    return todayRecords.find((r) => r.userId === userInfo?.userId) || null;
  };

  const handleAvatarClick = (userId: number) => {
    if (userId !== userInfo?.userId) return;
    const existing = getMyTodayRecord();
    if (existing) {
      if (existing.editable || isAdmin) {
        setEditingRecord(existing);
        setModalOpen(true);
      }
      return;
    }
    setEditingRecord(null);
    setModalOpen(true);
  };

  const handleModalClose = () => {
    setModalOpen(false);
    setEditingRecord(null);
  };

  const handleSubmitSuccess = () => {
    setModalOpen(false);
    setEditingRecord(null);
    loadTodayData();
  };

  return (
    <div className="min-h-screen bg-gray-50">
      <header className="bg-white shadow-sm border-b">
        <div className="max-w-5xl mx-auto px-4 py-3 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="w-8 h-8 bg-indigo-500 rounded-lg flex items-center justify-center">
              <svg className="w-5 h-5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0z" />
              </svg>
            </div>
            <h1 className="text-lg font-bold text-gray-800">站会打卡</h1>
          </div>

          <div className="flex items-center gap-4">
            <div className="text-sm text-gray-600">
              {userInfo?.nickname}
              <span className="ml-1 px-1.5 py-0.5 text-xs rounded bg-indigo-100 text-indigo-600">
                {userInfo?.role === 'ADMIN' ? '管理员' : userInfo?.role === 'LEADER' ? '组长' : '成员'}
              </span>
            </div>
            {(isAdmin || isLeader) && (
              <button
                onClick={() => setShowTeamMgmt(!showTeamMgmt)}
                className="px-3 py-1.5 text-sm bg-indigo-50 text-indigo-600 rounded-lg hover:bg-indigo-100"
              >
                小组管理
              </button>
            )}
            <button
              onClick={logout}
              className="px-3 py-1.5 text-sm text-gray-500 hover:text-gray-700"
            >
              退出
            </button>
          </div>
        </div>
      </header>

      <main className="max-w-5xl mx-auto px-4 py-6">
        {teams.length === 0 ? (
          <div className="text-center py-20">
            <p className="text-gray-500 text-lg mb-4">你还没有加入任何小组</p>
            {(isAdmin || isLeader) && (
              <button
                onClick={() => setShowTeamMgmt(true)}
                className="px-4 py-2 bg-indigo-500 text-white rounded-lg hover:bg-indigo-600"
              >
                创建小组
              </button>
            )}
          </div>
        ) : (
          <>
            <div className="flex items-center gap-4 mb-6">
              <select
                value={selectedTeam?.id || ''}
                onChange={(e) => {
                  const team = teams.find((t) => t.id === Number(e.target.value));
                  setSelectedTeam(team || null);
                }}
                className="px-4 py-2 border rounded-lg bg-white"
              >
                {teams.map((t) => (
                  <option key={t.id} value={t.id}>{t.name}</option>
                ))}
              </select>

              <div className="flex bg-white rounded-lg border overflow-hidden">
                <button
                  onClick={() => setViewMode('today')}
                  className={`px-4 py-2 text-sm ${viewMode === 'today' ? 'bg-indigo-500 text-white' : 'text-gray-600 hover:bg-gray-50'}`}
                >
                  今日站会
                </button>
                <button
                  onClick={() => setViewMode('calendar')}
                  className={`px-4 py-2 text-sm ${viewMode === 'calendar' ? 'bg-indigo-500 text-white' : 'text-gray-600 hover:bg-gray-50'}`}
                >
                  日历回看
                </button>
              </div>
            </div>

            {viewMode === 'today' && selectedTeam && (
              <div>
                <h2 className="text-lg font-semibold text-gray-800 mb-4">
                  今日站会 · {today}
                </h2>

                <div className="flex flex-wrap gap-4 mb-6">
                  {members.map((member) => {
                    const submitted = member.submitted;
                    const isMe = member.userId === userInfo?.userId;

                    return (
                      <div
                        key={member.userId}
                        role="button"
                        tabIndex={0}
                        onClick={() => handleAvatarClick(member.userId)}
                        onKeyDown={(e) => { if (e.key === 'Enter') handleAvatarClick(member.userId); }}
                        aria-label={isMe ? (submitted ? '编辑我的站会记录' : '提交站会记录') : member.nickname}
                        className={`relative flex flex-col items-center p-4 rounded-xl bg-white shadow-sm border-2 transition-all ${
                          isMe ? 'cursor-pointer hover:shadow-md border-indigo-200 hover:border-indigo-400' : 'border-gray-100'
                        }`}
                      >
                        <div className={`w-16 h-16 rounded-full flex items-center justify-center text-xl font-bold ${
                          submitted ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-500'
                        }`}>
                          {member.nickname.charAt(0)}
                        </div>
                        {submitted && (
                          <div className="absolute -top-1 -right-1 w-6 h-6 bg-green-500 rounded-full flex items-center justify-center">
                            <svg className="w-4 h-4 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={3} d="M5 13l4 4L19 7" />
                            </svg>
                          </div>
                        )}
                        <span className="mt-2 text-sm text-gray-700 font-medium">{member.nickname}</span>
                        <span className="text-xs text-gray-400">
                          {submitted ? '已提交' : isMe ? '点击提交' : '未提交'}
                        </span>
                      </div>
                    );
                  })}
                </div>

                {todayRecords.length > 0 && (
                  <div className="space-y-4">
                    <h3 className="font-semibold text-gray-800">今日站会内容</h3>
                    {todayRecords.map((record) => (
                      <div key={record.id} className="bg-white rounded-xl p-5 shadow-sm border">
                        <div className="flex items-center justify-between mb-3">
                          <div className="flex items-center gap-2">
                            <div className="w-8 h-8 rounded-full bg-indigo-100 text-indigo-700 flex items-center justify-center text-sm font-bold">
                              {record.nickname.charAt(0)}
                            </div>
                            <span className="font-medium text-gray-800">{record.nickname}</span>
                          </div>
                          {record.editable && (
                            <button
                              onClick={() => { setEditingRecord(record); setModalOpen(true); }}
                              className="text-xs px-2 py-1 text-indigo-500 bg-indigo-50 rounded hover:bg-indigo-100"
                            >
                              编辑
                            </button>
                          )}
                        </div>
                        <div className="space-y-2 text-sm">
                          <div>
                            <span className="text-gray-500">昨天：</span>
                            <span className="text-gray-800">{record.yesterday || '无'}</span>
                          </div>
                          <div>
                            <span className="text-gray-500">今天：</span>
                            <span className="text-gray-800">{record.today || '无'}</span>
                          </div>
                          <div>
                            <span className="text-gray-500">阻塞：</span>
                            <span className={record.blocker ? 'text-red-600' : 'text-gray-800'}>{record.blocker || '无'}</span>
                          </div>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            )}

            {viewMode === 'calendar' && selectedTeam && (
              <CalendarView teamId={selectedTeam.id} isAdmin={isAdmin} />
            )}
          </>
        )}

        {showTeamMgmt && (
          <TeamManagement
            team={selectedTeam}
            onClose={() => { setShowTeamMgmt(false); loadTeams(); }}
            isAdmin={isAdmin}
            isLeader={isLeader}
          />
        )}
      </main>

      {modalOpen && selectedTeam && (
        <StandupModal
          teamId={selectedTeam.id}
          record={editingRecord}
          onClose={handleModalClose}
          onSuccess={handleSubmitSuccess}
        />
      )}
    </div>
  );
}
