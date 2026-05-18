import { useState, useEffect, useCallback } from 'react';
import { standupApi } from '../api';
import type { StandupRecordVO } from '../types';

interface Props {
  teamId: number;
  isAdmin: boolean;
}

export default function CalendarView({ teamId, isAdmin }: Props) {
  const [submittedDates, setSubmittedDates] = useState<string[]>([]);
  const [selectedDate, setSelectedDate] = useState<string | null>(null);
  const [records, setRecords] = useState<StandupRecordVO[]>([]);
  const [editRecord, setEditRecord] = useState<StandupRecordVO | null>(null);

  const today = new Date();
  const twoWeeksAgo = new Date(today);
  twoWeeksAgo.setDate(twoWeeksAgo.getDate() - 13);

  const startDate = twoWeeksAgo.toISOString().split('T')[0];
  const endDate = today.toISOString().split('T')[0];

  const loadSubmittedDates = useCallback(async () => {
    try {
      const dates = await standupApi.getSubmittedDates(teamId, startDate, endDate);
      setSubmittedDates(dates);
    } catch (err) {
      console.error(err);
    }
  }, [teamId, startDate, endDate]);

  useEffect(() => {
    loadSubmittedDates();
  }, [loadSubmittedDates]);

  const handleDateClick = async (dateStr: string) => {
    setSelectedDate(dateStr);
    try {
      const data = await standupApi.getTeamRecordsByDate(teamId, dateStr);
      setRecords(data);
    } catch (err) {
      console.error(err);
    }
  };

  const getDaysInRange = () => {
    const days: { date: string; label: string; dayOfWeek: string; isToday: boolean }[] = [];
    const current = new Date(twoWeeksAgo);
    const weekDays = ['日', '一', '二', '三', '四', '五', '六'];

    while (current <= today) {
      const dateStr = current.toISOString().split('T')[0];
      days.push({
        date: dateStr,
        label: `${current.getMonth() + 1}/${current.getDate()}`,
        dayOfWeek: weekDays[current.getDay()],
        isToday: dateStr === today.toISOString().split('T')[0],
      });
      current.setDate(current.getDate() + 1);
    }
    return days;
  };

  const days = getDaysInRange();

  return (
    <div>
      <h2 className="text-lg font-semibold text-gray-800 mb-4">日历回看（近两周）</h2>

      <div className="bg-white rounded-xl p-4 shadow-sm border mb-6">
        <div className="grid grid-cols-7 gap-2">
          {['日', '一', '二', '三', '四', '五', '六'].map((d) => (
            <div key={d} className="text-center text-xs text-gray-400 font-medium py-1">{d}</div>
          ))}
          {days.map((day) => {
            const hasRecord = submittedDates.includes(day.date);
            const isSelected = selectedDate === day.date;

            return (
              <button
                key={day.date}
                onClick={() => handleDateClick(day.date)}
                className={`relative p-2 rounded-lg text-center transition-all ${
                  isSelected
                    ? 'bg-indigo-500 text-white'
                    : day.isToday
                    ? 'bg-indigo-50 text-indigo-700'
                    : 'hover:bg-gray-50 text-gray-700'
                }`}
              >
                <div className="text-sm">{day.label}</div>
                {hasRecord && (
                  <div className={`w-1.5 h-1.5 rounded-full mx-auto mt-0.5 ${
                    isSelected ? 'bg-white' : 'bg-green-500'
                  }`} />
                )}
              </button>
            );
          })}
        </div>
      </div>

      {selectedDate && (
        <div>
          <h3 className="font-semibold text-gray-800 mb-3">{selectedDate} 的站会记录</h3>
          {records.length === 0 ? (
            <div className="bg-white rounded-xl p-8 text-center text-gray-400 shadow-sm border">
              当天没有站会记录
            </div>
          ) : (
            <div className="space-y-3">
              {records.map((record) => (
                <div key={record.id} className="bg-white rounded-xl p-4 shadow-sm border">
                  <div className="flex items-center justify-between mb-2">
                    <div className="flex items-center gap-2">
                      <div className="w-8 h-8 rounded-full bg-indigo-100 text-indigo-700 flex items-center justify-center text-sm font-bold">
                        {record.nickname.charAt(0)}
                      </div>
                      <span className="font-medium text-gray-800">{record.nickname}</span>
                    </div>
                    {record.editable && isAdmin && (
                      <button
                        onClick={() => setEditRecord(record)}
                        className="text-xs px-2 py-1 text-orange-500 bg-orange-50 rounded hover:bg-orange-100"
                      >
                        管理员编辑
                      </button>
                    )}
                  </div>
                  <div className="space-y-1.5 text-sm">
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
                      <span className={record.blocker ? 'text-red-600' : 'text-gray-800'}>
                        {record.blocker || '无'}
                      </span>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {editRecord && (
        <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50"
          onClick={() => setEditRecord(null)}>
          <div className="bg-white rounded-2xl shadow-xl p-6 w-full max-w-lg mx-4"
            onClick={(e) => e.stopPropagation()}>
            <AdminEditModal record={editRecord} onClose={() => setEditRecord(null)} />
          </div>
        </div>
      )}
    </div>
  );
}

function AdminEditModal({ record, onClose }: { record: StandupRecordVO; onClose: () => void }) {
  const [yesterday, setYesterday] = useState(record.yesterday);
  const [today, setToday] = useState(record.today);
  const [blocker, setBlocker] = useState(record.blocker);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleSave = async () => {
    setLoading(true);
    setError('');
    try {
      await standupApi.update(record.id, {
        teamId: record.teamId,
        yesterday,
        today,
        blocker,
      });
      onClose();
    } catch (err: any) {
      setError(err.message || '保存失败');
    } finally {
      setLoading(false);
    }
  };

  return (
    <>
      <h2 className="text-lg font-bold text-gray-800 mb-4">管理员编辑记录</h2>
      <div className="space-y-4">
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">昨天</label>
          <textarea value={yesterday} onChange={(e) => setYesterday(e.target.value)} rows={3}
            className="w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-indigo-500 outline-none resize-none" />
        </div>
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">今天</label>
          <textarea value={today} onChange={(e) => setToday(e.target.value)} rows={3}
            className="w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-indigo-500 outline-none resize-none" />
        </div>
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">阻塞</label>
          <textarea value={blocker} onChange={(e) => setBlocker(e.target.value)} rows={2}
            className="w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-indigo-500 outline-none resize-none" />
        </div>
      </div>
      {error && <div className="mt-3 text-sm text-red-500 bg-red-50 p-2 rounded-lg">{error}</div>}
      <div className="mt-6 flex gap-3 justify-end">
        <button onClick={onClose} className="px-4 py-2 text-gray-600 bg-gray-100 rounded-lg hover:bg-gray-200">取消</button>
        <button onClick={handleSave} disabled={loading}
          className="px-4 py-2 bg-indigo-500 text-white rounded-lg hover:bg-indigo-600 disabled:opacity-50">
          {loading ? '保存中...' : '保存'}
        </button>
      </div>
    </>
  );
}
