import { useState } from 'react';
import { standupApi } from '../api';
import type { StandupRecordVO } from '../types';

interface Props {
  teamId: number;
  record: StandupRecordVO | null;
  onClose: () => void;
  onSuccess: () => void;
}

export default function StandupModal({ teamId, record, onClose, onSuccess }: Props) {
  const isEdit = !!record;
  const [yesterday, setYesterday] = useState(record?.yesterday || '');
  const [today, setToday] = useState(record?.today || '');
  const [blocker, setBlocker] = useState(record?.blocker || '');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleSubmit = async () => {
    setLoading(true);
    setError('');

    try {
      if (isEdit && record) {
        await standupApi.update(record.id, {
          teamId,
          yesterday,
          today,
          blocker,
        });
      } else {
        await standupApi.submit({
          teamId,
          yesterday,
          today,
          blocker,
        });
      }
      onSuccess();
    } catch (err: any) {
      setError(err.message || '提交失败');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50" onClick={onClose}>
      <div
        className="bg-white rounded-2xl shadow-xl p-6 w-full max-w-lg mx-4"
        onClick={(e) => e.stopPropagation()}
      >
        <h2 className="text-lg font-bold text-gray-800 mb-4">
          {isEdit ? '编辑站会记录' : '提交站会记录'}
        </h2>

        <div className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              📋 昨天干了啥
            </label>
            <textarea
              value={yesterday}
              onChange={(e) => setYesterday(e.target.value)}
              rows={3}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-transparent outline-none resize-none"
              placeholder="描述昨天完成的工作..."
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              🎯 今天准备干啥
            </label>
            <textarea
              value={today}
              onChange={(e) => setToday(e.target.value)}
              rows={3}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-transparent outline-none resize-none"
              placeholder="描述今天计划的工作..."
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              🚧 有没有卡住的地方
            </label>
            <textarea
              value={blocker}
              onChange={(e) => setBlocker(e.target.value)}
              rows={2}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-transparent outline-none resize-none"
              placeholder="描述遇到的阻碍，没有可不填..."
            />
          </div>
        </div>

        {error && (
          <div className="mt-3 text-sm text-red-500 bg-red-50 p-2 rounded-lg">{error}</div>
        )}

        <div className="mt-6 flex gap-3 justify-end">
          <button
            onClick={onClose}
            className="px-4 py-2 text-gray-600 bg-gray-100 rounded-lg hover:bg-gray-200"
          >
            取消
          </button>
          <button
            onClick={handleSubmit}
            disabled={loading}
            className="px-4 py-2 bg-indigo-500 text-white rounded-lg hover:bg-indigo-600 disabled:opacity-50"
          >
            {loading ? '提交中...' : isEdit ? '保存修改' : '提交'}
          </button>
        </div>
      </div>
    </div>
  );
}
