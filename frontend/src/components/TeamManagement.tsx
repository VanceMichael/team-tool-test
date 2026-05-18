import { useState } from 'react';
import { teamApi } from '../api';
import type { Team, TeamMemberVO } from '../types';

interface Props {
  team?: Team | null;
  onClose: () => void;
  isAdmin: boolean;
  isLeader: boolean;
}

export default function TeamManagement({ team, onClose, isAdmin, isLeader }: Props) {
  const [members, setMembers] = useState<TeamMemberVO[]>([]);
  const [addUsername, setAddUsername] = useState('');
  const [newTeamName, setNewTeamName] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [membersLoaded, setMembersLoaded] = useState(false);

  const loadMembers = async () => {
    if (!team) return;
    try {
      const data = await teamApi.getTeamMembers(team.id);
      setMembers(data);
      setMembersLoaded(true);
    } catch (err) {
      console.error(err);
    }
  };

  if (team && !membersLoaded) {
    loadMembers();
  }

  const handleAddMember = async () => {
    if (!team || !addUsername.trim()) return;
    setLoading(true);
    setError('');
    try {
      const userId = Number(addUsername.trim());
      if (isNaN(userId)) {
        setError('请输入用户ID（数字）');
        return;
      }
      await teamApi.addMember({ teamId: team.id, userId });
      setAddUsername('');
      loadMembers();
    } catch (err: any) {
      setError(err.message || '添加失败');
    } finally {
      setLoading(false);
    }
  };

  const handleRemoveMember = async (userId: number) => {
    if (!team) return;
    if (!confirm('确定要移除该成员吗？')) return;
    try {
      await teamApi.removeMember(team.id, userId);
      loadMembers();
    } catch (err: any) {
      setError(err.message || '移除失败');
    }
  };

  const handleCreateTeam = async () => {
    if (!newTeamName.trim()) return;
    setLoading(true);
    setError('');
    try {
      await teamApi.createTeam({ name: newTeamName.trim() });
      setNewTeamName('');
      onClose();
    } catch (err: any) {
      setError(err.message || '创建失败');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50" onClick={onClose}>
      <div className="bg-white rounded-2xl shadow-xl p-6 w-full max-w-md mx-4" onClick={(e) => e.stopPropagation()}>
        <h2 className="text-lg font-bold text-gray-800 mb-4">小组管理</h2>

        {error && <div className="mb-3 text-sm text-red-500 bg-red-50 p-2 rounded-lg">{error}</div>}

        {team && (
          <div className="mb-6">
            <h3 className="font-medium text-gray-700 mb-2">当前小组：{team.name}</h3>
            <div className="space-y-2">
              {members.map((member) => (
                <div key={member.userId} className="flex items-center justify-between p-2 bg-gray-50 rounded-lg">
                  <div className="flex items-center gap-2">
                    <div className="w-8 h-8 rounded-full bg-indigo-100 text-indigo-700 flex items-center justify-center text-sm font-bold">
                      {member.nickname.charAt(0)}
                    </div>
                    <span className="text-sm text-gray-800">{member.nickname}</span>
                    <span className="text-xs text-gray-400">
                      {member.role === 'ADMIN' ? '管理员' : member.role === 'LEADER' ? '组长' : '成员'}
                    </span>
                  </div>
                  {member.role !== 'LEADER' && member.role !== 'ADMIN' && (
                    <button
                      onClick={() => handleRemoveMember(member.userId)}
                      className="text-xs text-red-500 hover:text-red-700"
                    >
                      移除
                    </button>
                  )}
                </div>
              ))}
            </div>

            <div className="mt-3 flex gap-2">
              <input
                type="text"
                value={addUsername}
                onChange={(e) => setAddUsername(e.target.value)}
                placeholder="输入用户ID"
                className="flex-1 px-3 py-2 border rounded-lg text-sm focus:ring-2 focus:ring-indigo-500 outline-none"
              />
              <button
                onClick={handleAddMember}
                disabled={loading}
                className="px-3 py-2 bg-indigo-500 text-white rounded-lg text-sm hover:bg-indigo-600 disabled:opacity-50"
              >
                添加
              </button>
            </div>
          </div>
        )}

        {(isAdmin || isLeader) && (
          <div className={team ? 'border-t pt-4' : ''}>
            <h3 className="font-medium text-gray-700 mb-2">创建新小组</h3>
            <div className="flex gap-2">
              <input
                type="text"
                value={newTeamName}
                onChange={(e) => setNewTeamName(e.target.value)}
                placeholder="小组名称"
                className="flex-1 px-3 py-2 border rounded-lg text-sm focus:ring-2 focus:ring-indigo-500 outline-none"
              />
              <button
                onClick={handleCreateTeam}
                disabled={loading}
                className="px-3 py-2 bg-green-500 text-white rounded-lg text-sm hover:bg-green-600 disabled:opacity-50"
              >
                创建
              </button>
            </div>
          </div>
        )}

        <div className="mt-4 flex justify-end">
          <button onClick={onClose} className="px-4 py-2 text-gray-600 bg-gray-100 rounded-lg hover:bg-gray-200">
            关闭
          </button>
        </div>
      </div>
    </div>
  );
}
