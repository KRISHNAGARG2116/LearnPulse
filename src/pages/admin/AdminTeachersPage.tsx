import React, { useState, useEffect, useCallback } from 'react';
import {
  getAdminTeachers,
  approveTeacher,
  rejectTeacher
} from '../../api/lmsApi';
import { TeacherVerificationDTO } from '../../types/lms';
import {
  UserCheck,
  UserX,
  Clock,
  CheckCircle2,
  AlertCircle,
  Loader2,
  X,
  Filter,
  GraduationCap
} from 'lucide-react';

export const AdminTeachersPage: React.FC = () => {
  const [teachers, setTeachers] = useState<TeacherVerificationDTO[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [successMsg, setSuccessMsg] = useState<string | null>(null);

  const [statusFilter, setStatusFilter] = useState<string>('ALL');

  // Rejection modal
  const [rejectingTeacher, setRejectingTeacher] = useState<TeacherVerificationDTO | null>(null);
  const [rejectReason, setRejectReason] = useState('');
  const [actionPending, setActionPending] = useState(false);

  const fetchTeachers = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const filter = statusFilter !== 'ALL' ? statusFilter : undefined;
      const data = await getAdminTeachers(filter);
      setTeachers(data || []);
    } catch (err: any) {
      setError(err?.response?.data?.message || 'Failed to load teacher verification directory');
    } finally {
      setLoading(false);
    }
  }, [statusFilter]);

  useEffect(() => {
    fetchTeachers();
  }, [fetchTeachers]);

  const handleApprove = async (teacher: TeacherVerificationDTO) => {
    try {
      setActionPending(true);
      setError(null);
      await approveTeacher(teacher.userId);
      setSuccessMsg(`Faculty credentials for ${teacher.email} approved successfully.`);
      fetchTeachers();
    } catch (err: any) {
      setError(err?.response?.data?.message || 'Failed to approve teacher verification');
    } finally {
      setActionPending(false);
    }
  };

  const handleRejectConfirm = async () => {
    if (!rejectingTeacher) return;
    try {
      setActionPending(true);
      setError(null);
      await rejectTeacher(rejectingTeacher.userId, rejectReason.trim() || undefined);
      setSuccessMsg(`Faculty verification for ${rejectingTeacher.email} marked as REJECTED.`);
      setRejectingTeacher(null);
      setRejectReason('');
      fetchTeachers();
    } catch (err: any) {
      setError(err?.response?.data?.message || 'Failed to reject teacher verification');
    } finally {
      setActionPending(false);
    }
  };

  const pendingCount = teachers.filter((t) => t.status === 'PENDING').length;

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-black text-white tracking-tight flex items-center gap-3">
            <GraduationCap className="w-7 h-7 text-amber-400" />
            Teacher Credential Verification & Governance
          </h1>
          <p className="text-xs text-slate-400 mt-1">
            Review faculty documentation, credential applications, and grant or revoke curriculum management authority
          </p>
        </div>
        {pendingCount > 0 && (
          <div className="flex items-center gap-2 px-3 py-1.5 rounded-xl bg-amber-500/10 border border-amber-500/30 text-amber-400 text-xs font-semibold">
            <Clock className="w-4 h-4 animate-pulse" />
            <span>{pendingCount} Pending Verification{pendingCount > 1 ? 's' : ''}</span>
          </div>
        )}
      </div>

      {/* Alerts */}
      {error && (
        <div className="p-4 rounded-xl bg-red-500/10 border border-red-500/30 text-red-400 flex items-center justify-between text-xs">
          <div className="flex items-center gap-2">
            <AlertCircle className="w-4 h-4 shrink-0" />
            <span>{error}</span>
          </div>
          <button onClick={() => setError(null)} className="text-red-400 hover:text-white">
            <X className="w-4 h-4" />
          </button>
        </div>
      )}

      {successMsg && (
        <div className="p-4 rounded-xl bg-emerald-500/10 border border-emerald-500/30 text-emerald-400 flex items-center justify-between text-xs">
          <div className="flex items-center gap-2">
            <CheckCircle2 className="w-4 h-4 shrink-0" />
            <span>{successMsg}</span>
          </div>
          <button onClick={() => setSuccessMsg(null)} className="text-emerald-400 hover:text-white">
            <X className="w-4 h-4" />
          </button>
        </div>
      )}

      {/* Toolbar */}
      <div className="glass-panel p-4 rounded-2xl border border-slate-800 flex items-center justify-between">
        <div className="flex items-center gap-2">
          <Filter className="w-4 h-4 text-slate-400" />
          <span className="text-xs text-slate-400">Filter Status:</span>
          <select
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
            className="bg-slate-900 border border-slate-800 rounded-xl px-3 py-1.5 text-xs text-slate-200 focus:outline-none focus:border-amber-500"
          >
            <option value="ALL">All Verification States</option>
            <option value="PENDING">Pending Review</option>
            <option value="APPROVED">Approved Faculty</option>
            <option value="REJECTED">Rejected</option>
          </select>
        </div>
      </div>

      {/* Table */}
      <div className="glass-panel rounded-2xl border border-slate-800 overflow-hidden">
        {loading ? (
          <div className="p-12 flex flex-col items-center justify-center gap-3">
            <Loader2 className="w-6 h-6 text-amber-400 animate-spin" />
            <p className="text-xs text-slate-400">Loading faculty verification queue...</p>
          </div>
        ) : teachers.length === 0 ? (
          <div className="p-12 text-center text-slate-500 text-xs">
            No teacher verification records found.
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="bg-slate-900/60 border-b border-slate-800 text-slate-400 uppercase tracking-wider font-semibold">
                <tr>
                  <th className="py-3 px-4">Faculty Member</th>
                  <th className="py-3 px-4">Department</th>
                  <th className="py-3 px-4">Status</th>
                  <th className="py-3 px-4">Rejection Reason / Notes</th>
                  <th className="py-3 px-4">Registration Date</th>
                  <th className="py-3 px-4 text-right">Moderation Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/60">
                {teachers.map((t) => {
                  let statusBadge = (
                    <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-amber-500/10 text-amber-400 border border-amber-500/30">
                      <Clock className="w-3 h-3" />
                      Pending Review
                    </span>
                  );
                  if (t.status === 'APPROVED') {
                    statusBadge = (
                      <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-emerald-500/10 text-emerald-400 border border-emerald-500/30">
                        <CheckCircle2 className="w-3 h-3" />
                        Approved
                      </span>
                    );
                  } else if (t.status === 'REJECTED') {
                    statusBadge = (
                      <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-red-500/10 text-red-400 border border-red-500/30">
                        <UserX className="w-3 h-3" />
                        Rejected
                      </span>
                    );
                  }

                  return (
                    <tr key={t.userId} className="hover:bg-slate-900/40 transition-colors">
                      <td className="py-3 px-4">
                        <p className="font-semibold text-white">{t.firstName} {t.lastName}</p>
                        <p className="text-[10px] text-slate-400">{t.email}</p>
                      </td>
                      <td className="py-3 px-4 text-slate-300">
                        {t.department || 'General Faculty'}
                      </td>
                      <td className="py-3 px-4">{statusBadge}</td>
                      <td className="py-3 px-4 text-slate-400 max-w-xs truncate" title={t.rejectionReason || ''}>
                        {t.rejectionReason || '—'}
                      </td>
                      <td className="py-3 px-4 text-slate-400">
                        {t.registeredAt ? new Date(t.registeredAt).toLocaleDateString() : '—'}
                      </td>
                      <td className="py-3 px-4 text-right">
                        <div className="inline-flex items-center gap-2">
                          {t.status !== 'APPROVED' && (
                            <button
                              disabled={actionPending}
                              onClick={() => handleApprove(t)}
                              className="px-2.5 py-1 rounded-lg bg-emerald-600 hover:bg-emerald-500 text-white font-semibold text-[11px] flex items-center gap-1 transition-colors"
                            >
                              <UserCheck className="w-3 h-3" />
                              Approve
                            </button>
                          )}
                          {t.status !== 'REJECTED' && (
                            <button
                              disabled={actionPending}
                              onClick={() => {
                                setRejectingTeacher(t);
                                setRejectReason('');
                              }}
                              className="px-2.5 py-1 rounded-lg bg-red-600/20 text-red-400 hover:bg-red-600 hover:text-white border border-red-500/30 font-semibold text-[11px] flex items-center gap-1 transition-colors"
                            >
                              <UserX className="w-3 h-3" />
                              Reject
                            </button>
                          )}
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Reject Reason Modal */}
      {rejectingTeacher && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 backdrop-blur-sm p-4">
          <div className="glass-card max-w-md w-full p-6 rounded-2xl border border-red-500/30 bg-slate-900 shadow-2xl space-y-4">
            <h2 className="text-sm font-bold text-red-400 flex items-center gap-2">
              <UserX className="w-4 h-4" />
              Reject Faculty Verification
            </h2>
            <p className="text-xs text-slate-300">
              Provide a rationale for rejecting <strong className="text-white">{rejectingTeacher.email}</strong>:
            </p>

            <textarea
              rows={3}
              placeholder="e.g. Incomplete institutional documentation or unverified department affiliation..."
              value={rejectReason}
              onChange={(e) => setRejectReason(e.target.value)}
              className="w-full bg-slate-950 border border-slate-800 rounded-xl p-3 text-xs text-white placeholder-slate-500 focus:outline-none focus:border-red-500"
            />

            <div className="flex justify-end gap-2 pt-2">
              <button
                disabled={actionPending}
                onClick={() => setRejectingTeacher(null)}
                className="px-3 py-1.5 rounded-xl bg-slate-800 text-slate-300 hover:bg-slate-700 text-xs"
              >
                Cancel
              </button>
              <button
                disabled={actionPending}
                onClick={handleRejectConfirm}
                className="px-4 py-1.5 rounded-xl bg-red-600 hover:bg-red-500 text-white font-semibold text-xs flex items-center gap-1.5"
              >
                {actionPending && <Loader2 className="w-3.5 h-3.5 animate-spin" />}
                Confirm Rejection
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
