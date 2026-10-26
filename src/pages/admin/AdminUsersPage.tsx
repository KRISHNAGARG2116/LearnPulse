import React, { useState, useEffect, useCallback } from 'react';
import {
  getAdminUsers,
  getAdminUserDetail,
  updateAdminUserRole,
  deactivateAdminUser,
  reactivateAdminUser,
  initiatePasswordReset
} from '../../api/lmsApi';
import { AdminUserDTO, AdminUserDetailDTO, RoleType } from '../../types/lms';
import {
  Users,
  Search,
  Filter,
  Shield,
  KeyRound,
  UserX,
  UserCheck,
  Eye,
  Edit,
  Loader2,
  AlertCircle,
  CheckCircle2,
  X,
  ChevronLeft,
  ChevronRight
} from 'lucide-react';

export const AdminUsersPage: React.FC = () => {
  const [users, setUsers] = useState<AdminUserDTO[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [successMsg, setSuccessMsg] = useState<string | null>(null);

  // Filters & Pagination
  const [searchTerm, setSearchTerm] = useState('');
  const [roleFilter, setRoleFilter] = useState<string>('ALL');
  const [statusFilter, setStatusFilter] = useState<string>('ALL');
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [totalUsers, setTotalUsers] = useState(0);
  const pageSize = 10;

  // Modals state
  const [selectedUserDetail, setSelectedUserDetail] = useState<AdminUserDetailDTO | null>(null);

  const [roleModalUser, setRoleModalUser] = useState<AdminUserDTO | null>(null);
  const [newRole, setNewRole] = useState<RoleType>('STUDENT');

  const [confirmDeactivateUser, setConfirmDeactivateUser] = useState<AdminUserDTO | null>(null);
  const [confirmResetUser, setConfirmResetUser] = useState<AdminUserDTO | null>(null);
  const [actionPending, setActionPending] = useState(false);

  const fetchUsers = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);

      const params: any = {
        page: currentPage,
        size: pageSize,
      };

      if (roleFilter !== 'ALL') params.role = roleFilter;
      if (statusFilter === 'ACTIVE') params.enabled = true;
      if (statusFilter === 'DEACTIVATED') params.enabled = false;
      if (searchTerm.trim()) params.search = searchTerm.trim();

      const res = await getAdminUsers(params);
      setUsers(res.content || []);
      setTotalPages(res.totalPages || 1);
      setTotalUsers(res.totalElements || 0);
    } catch (err: any) {
      setError(err?.response?.data?.message || 'Failed to load user directory');
    } finally {
      setLoading(false);
    }
  }, [currentPage, roleFilter, statusFilter, searchTerm]);

  useEffect(() => {
    fetchUsers();
  }, [fetchUsers]);

  const handleOpenDetail = async (userId: string) => {
    try {
      const detail = await getAdminUserDetail(userId);
      setSelectedUserDetail(detail);
    } catch (err: any) {
      setError(err?.response?.data?.message || 'Failed to fetch user details');
    }
  };

  const handleRoleChange = async () => {
    if (!roleModalUser) return;
    try {
      setActionPending(true);
      setError(null);
      await updateAdminUserRole(roleModalUser.id, newRole);
      setSuccessMsg(`Successfully updated role for ${roleModalUser.email} to ${newRole}`);
      setRoleModalUser(null);
      fetchUsers();
    } catch (err: any) {
      setError(err?.response?.data?.message || 'Failed to update user role');
    } finally {
      setActionPending(false);
    }
  };

  const handleToggleActive = async (user: AdminUserDTO) => {
    try {
      setActionPending(true);
      setError(null);
      if (user.enabled) {
        await deactivateAdminUser(user.id);
        setSuccessMsg(`Account for ${user.email} deactivated. Active sessions revoked.`);
      } else {
        await reactivateAdminUser(user.id);
        setSuccessMsg(`Account for ${user.email} reactivated.`);
      }
      setConfirmDeactivateUser(null);
      fetchUsers();
    } catch (err: any) {
      setError(err?.response?.data?.message || 'Failed to change account status');
    } finally {
      setActionPending(false);
    }
  };

  const handlePasswordReset = async () => {
    if (!confirmResetUser) return;
    try {
      setActionPending(true);
      setError(null);
      const res = await initiatePasswordReset(confirmResetUser.id);
      setSuccessMsg(res.message);
      setConfirmResetUser(null);
    } catch (err: any) {
      setError(err?.response?.data?.message || 'Failed to initiate password reset');
    } finally {
      setActionPending(false);
    }
  };

  return (
    <div className="space-y-6">
      {/* Page Title */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-black text-white tracking-tight flex items-center gap-3">
            <Users className="w-7 h-7 text-indigo-400" />
            User Account Governance
          </h1>
          <p className="text-xs text-slate-400 mt-1">
            Directory control, role escalation enforcement, account lifecycle deactivation, and password resets
          </p>
        </div>
        <div className="flex items-center gap-2">
          <span className="text-xs font-semibold px-3 py-1.5 rounded-xl bg-slate-800/80 text-slate-300 border border-slate-700">
            Total Users: <strong className="text-white">{totalUsers}</strong>
          </span>
        </div>
      </div>

      {/* Notifications */}
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

      {/* Search & Filter Toolbar */}
      <div className="glass-panel p-4 rounded-2xl border border-slate-800 flex flex-col md:flex-row gap-4 items-center justify-between">
        <div className="relative w-full md:w-80">
          <Search className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
          <input
            type="text"
            placeholder="Search by email address..."
            value={searchTerm}
            onChange={(e) => {
              setSearchTerm(e.target.value);
              setCurrentPage(0);
            }}
            className="w-full bg-slate-900/80 border border-slate-800 rounded-xl pl-9 pr-4 py-2 text-xs text-white placeholder-slate-500 focus:outline-none focus:border-indigo-500"
          />
        </div>

        <div className="flex flex-wrap items-center gap-3 w-full md:w-auto">
          <div className="flex items-center gap-2">
            <Filter className="w-3.5 h-3.5 text-slate-400" />
            <span className="text-xs text-slate-400">Role:</span>
            <select
              value={roleFilter}
              onChange={(e) => {
                setRoleFilter(e.target.value);
                setCurrentPage(0);
              }}
              className="bg-slate-900 border border-slate-800 rounded-xl px-3 py-1.5 text-xs text-slate-200 focus:outline-none focus:border-indigo-500"
            >
              <option value="ALL">All Roles</option>
              <option value="STUDENT">Students</option>
              <option value="TEACHER">Teachers</option>
              <option value="ADMIN">Administrators</option>
            </select>
          </div>

          <div className="flex items-center gap-2">
            <span className="text-xs text-slate-400">Status:</span>
            <select
              value={statusFilter}
              onChange={(e) => {
                setStatusFilter(e.target.value);
                setCurrentPage(0);
              }}
              className="bg-slate-900 border border-slate-800 rounded-xl px-3 py-1.5 text-xs text-slate-200 focus:outline-none focus:border-indigo-500"
            >
              <option value="ALL">All Status</option>
              <option value="ACTIVE">Active Only</option>
              <option value="DEACTIVATED">Deactivated Only</option>
            </select>
          </div>
        </div>
      </div>

      {/* Users Table */}
      <div className="glass-panel rounded-2xl border border-slate-800 overflow-hidden">
        {loading ? (
          <div className="p-12 flex flex-col items-center justify-center gap-3">
            <Loader2 className="w-6 h-6 text-indigo-400 animate-spin" />
            <p className="text-xs text-slate-400">Loading user accounts...</p>
          </div>
        ) : users.length === 0 ? (
          <div className="p-12 text-center text-slate-500 text-xs">
            No user accounts found matching current search and filter criteria.
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="bg-slate-900/60 border-b border-slate-800 text-slate-400 uppercase tracking-wider font-semibold">
                <tr>
                  <th className="py-3 px-4">User / Email</th>
                  <th className="py-3 px-4">Name & Department</th>
                  <th className="py-3 px-4">Security Role</th>
                  <th className="py-3 px-4">Account Status</th>
                  <th className="py-3 px-4">Registered Date</th>
                  <th className="py-3 px-4 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/60">
                {users.map((u) => {
                  const roleBadgeColor =
                    u.role === 'ADMIN'
                      ? 'bg-purple-500/20 text-purple-300 border-purple-500/40'
                      : u.role === 'TEACHER'
                      ? 'bg-amber-500/20 text-amber-300 border-amber-500/40'
                      : 'bg-indigo-500/20 text-indigo-300 border-indigo-500/40';

                  return (
                    <tr key={u.id} className="hover:bg-slate-900/40 transition-colors">
                      <td className="py-3 px-4 font-medium text-white">
                        <div className="flex items-center gap-2">
                          <div className="w-7 h-7 rounded-lg bg-slate-800 border border-slate-700 flex items-center justify-center text-xs font-bold text-slate-300 uppercase">
                            {u.email.substring(0, 2)}
                          </div>
                          <span className="truncate max-w-[200px]" title={u.email}>
                            {u.email}
                          </span>
                        </div>
                      </td>
                      <td className="py-3 px-4 text-slate-300">
                        {u.firstName || u.lastName ? `${u.firstName || ''} ${u.lastName || ''}`.trim() : '—'}
                        {u.department && (
                          <span className="block text-[10px] text-slate-500">{u.department}</span>
                        )}
                      </td>
                      <td className="py-3 px-4">
                        <span className={`inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[10px] font-bold border ${roleBadgeColor}`}>
                          <Shield className="w-3 h-3" />
                          {u.role}
                        </span>
                      </td>
                      <td className="py-3 px-4">
                        {u.enabled ? (
                          <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-semibold bg-emerald-500/10 text-emerald-400 border border-emerald-500/30">
                            Active
                          </span>
                        ) : (
                          <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-semibold bg-red-500/10 text-red-400 border border-red-500/30">
                            Deactivated
                          </span>
                        )}
                      </td>
                      <td className="py-3 px-4 text-slate-400">
                        {u.createdAt ? new Date(u.createdAt).toLocaleDateString() : '—'}
                      </td>
                      <td className="py-3 px-4 text-right">
                        <div className="inline-flex items-center gap-1.5">
                          <button
                            onClick={() => handleOpenDetail(u.id)}
                            className="p-1.5 rounded-lg bg-slate-800 text-slate-300 hover:text-white hover:bg-slate-700 transition-colors"
                            title="View User Details"
                          >
                            <Eye className="w-3.5 h-3.5" />
                          </button>
                          <button
                            onClick={() => {
                              setRoleModalUser(u);
                              setNewRole(u.role);
                            }}
                            className="p-1.5 rounded-lg bg-indigo-500/10 text-indigo-400 hover:text-indigo-200 hover:bg-indigo-500/20 border border-indigo-500/20 transition-colors"
                            title="Modify Role"
                          >
                            <Edit className="w-3.5 h-3.5" />
                          </button>
                          <button
                            onClick={() => setConfirmResetUser(u)}
                            className="p-1.5 rounded-lg bg-amber-500/10 text-amber-400 hover:text-amber-200 hover:bg-amber-500/20 border border-amber-500/20 transition-colors"
                            title="Initiate Password Reset"
                          >
                            <KeyRound className="w-3.5 h-3.5" />
                          </button>
                          {u.enabled ? (
                            <button
                              onClick={() => setConfirmDeactivateUser(u)}
                              className="p-1.5 rounded-lg bg-red-500/10 text-red-400 hover:text-red-200 hover:bg-red-500/20 border border-red-500/20 transition-colors"
                              title="Deactivate Account"
                            >
                              <UserX className="w-3.5 h-3.5" />
                            </button>
                          ) : (
                            <button
                              onClick={() => handleToggleActive(u)}
                              className="p-1.5 rounded-lg bg-emerald-500/10 text-emerald-400 hover:text-emerald-200 hover:bg-emerald-500/20 border border-emerald-500/20 transition-colors"
                              title="Reactivate Account"
                            >
                              <UserCheck className="w-3.5 h-3.5" />
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

        {/* Pagination Bar */}
        {totalPages > 1 && (
          <div className="p-3 bg-slate-900/60 border-t border-slate-800 flex items-center justify-between text-xs">
            <span className="text-slate-400">
              Page <strong className="text-white">{currentPage + 1}</strong> of <strong className="text-white">{totalPages}</strong>
            </span>
            <div className="flex items-center gap-1">
              <button
                disabled={currentPage === 0}
                onClick={() => setCurrentPage((p) => Math.max(0, p - 1))}
                className="p-1.5 rounded-lg bg-slate-800 text-slate-300 hover:text-white disabled:opacity-40 disabled:cursor-not-allowed"
              >
                <ChevronLeft className="w-4 h-4" />
              </button>
              <button
                disabled={currentPage >= totalPages - 1}
                onClick={() => setCurrentPage((p) => Math.min(totalPages - 1, p + 1))}
                className="p-1.5 rounded-lg bg-slate-800 text-slate-300 hover:text-white disabled:opacity-40 disabled:cursor-not-allowed"
              >
                <ChevronRight className="w-4 h-4" />
              </button>
            </div>
          </div>
        )}
      </div>

      {/* User Details Modal */}
      {selectedUserDetail && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 backdrop-blur-sm p-4">
          <div className="glass-card max-w-lg w-full p-6 rounded-2xl border border-slate-700 bg-slate-900 shadow-2xl relative space-y-4">
            <div className="flex items-center justify-between border-b border-slate-800 pb-3">
              <h2 className="text-base font-bold text-white flex items-center gap-2">
                <Users className="w-4 h-4 text-indigo-400" />
                Administrative User Profile
              </h2>
              <button
                onClick={() => setSelectedUserDetail(null)}
                className="text-slate-400 hover:text-white p-1 rounded-lg"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            <div className="grid grid-cols-2 gap-3 text-xs">
              <div>
                <p className="text-[10px] text-slate-400 uppercase font-semibold">Email Address</p>
                <p className="font-medium text-white truncate">{selectedUserDetail.email}</p>
              </div>
              <div>
                <p className="text-[10px] text-slate-400 uppercase font-semibold">User Role</p>
                <p className="font-semibold text-indigo-400">{selectedUserDetail.role}</p>
              </div>
              <div>
                <p className="text-[10px] text-slate-400 uppercase font-semibold">Full Name</p>
                <p className="text-slate-200">
                  {selectedUserDetail.firstName || selectedUserDetail.lastName
                    ? `${selectedUserDetail.firstName || ''} ${selectedUserDetail.lastName || ''}`.trim()
                    : 'N/A'}
                </p>
              </div>
              <div>
                <p className="text-[10px] text-slate-400 uppercase font-semibold">Department</p>
                <p className="text-slate-200">{selectedUserDetail.department || 'N/A'}</p>
              </div>
              <div>
                <p className="text-[10px] text-slate-400 uppercase font-semibold">Account Status</p>
                <p className={selectedUserDetail.enabled ? 'text-emerald-400' : 'text-red-400'}>
                  {selectedUserDetail.enabled ? 'Enabled / Active' : 'Deactivated'}
                </p>
              </div>
              <div>
                <p className="text-[10px] text-slate-400 uppercase font-semibold">Registered</p>
                <p className="text-slate-300">
                  {selectedUserDetail.createdAt ? new Date(selectedUserDetail.createdAt).toLocaleString() : 'N/A'}
                </p>
              </div>
            </div>

            {/* Academic Activity Stats */}
            <div className="bg-slate-950/60 p-3 rounded-xl border border-slate-800 space-y-2">
              <p className="text-[10px] font-bold text-slate-400 uppercase">Platform Activity Counts</p>
              <div className="grid grid-cols-3 gap-2 text-center text-xs">
                <div className="p-2 rounded-lg bg-slate-900 border border-slate-800">
                  <p className="text-[10px] text-slate-500">Enrolled</p>
                  <p className="font-bold text-white mt-0.5">{selectedUserDetail.enrolledSubjectsCount}</p>
                </div>
                <div className="p-2 rounded-lg bg-slate-900 border border-slate-800">
                  <p className="text-[10px] text-slate-500">Quizzes Taken</p>
                  <p className="font-bold text-white mt-0.5">{selectedUserDetail.completedQuizzesCount}</p>
                </div>
                <div className="p-2 rounded-lg bg-slate-900 border border-slate-800">
                  <p className="text-[10px] text-slate-500">Docs Uploaded</p>
                  <p className="font-bold text-white mt-0.5">{selectedUserDetail.uploadedDocumentsCount}</p>
                </div>
              </div>
            </div>

            <div className="flex justify-end pt-2">
              <button
                onClick={() => setSelectedUserDetail(null)}
                className="px-4 py-1.5 rounded-xl bg-slate-800 text-slate-200 hover:bg-slate-700 text-xs font-semibold"
              >
                Close
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Role Modification Modal */}
      {roleModalUser && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 backdrop-blur-sm p-4">
          <div className="glass-card max-w-md w-full p-6 rounded-2xl border border-slate-700 bg-slate-900 shadow-2xl space-y-4">
            <h2 className="text-sm font-bold text-white flex items-center gap-2">
              <Shield className="w-4 h-4 text-purple-400" />
              Modify Role Governance
            </h2>
            <p className="text-xs text-slate-400">
              Update access level for <strong className="text-white">{roleModalUser.email}</strong>.
              Protected by self-demotion and last-administrator invariants.
            </p>

            <div>
              <label className="text-[10px] font-semibold text-slate-400 uppercase">Select Target Role</label>
              <select
                value={newRole}
                onChange={(e) => setNewRole(e.target.value as RoleType)}
                className="w-full mt-1 bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-xs text-white focus:outline-none focus:border-indigo-500"
              >
                <option value="STUDENT">STUDENT (Learning Tools & Assessments)</option>
                <option value="TEACHER">TEACHER (Curriculum, Quizzes & Grading)</option>
                <option value="ADMIN">ADMIN (Full Platform Governance)</option>
              </select>
            </div>

            <div className="flex justify-end gap-2 pt-2">
              <button
                disabled={actionPending}
                onClick={() => setRoleModalUser(null)}
                className="px-3 py-1.5 rounded-xl bg-slate-800 text-slate-300 hover:bg-slate-700 text-xs"
              >
                Cancel
              </button>
              <button
                disabled={actionPending}
                onClick={handleRoleChange}
                className="px-4 py-1.5 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white font-semibold text-xs flex items-center gap-1.5"
              >
                {actionPending && <Loader2 className="w-3.5 h-3.5 animate-spin" />}
                Confirm Role Change
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Deactivate Confirmation Modal */}
      {confirmDeactivateUser && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 backdrop-blur-sm p-4">
          <div className="glass-card max-w-md w-full p-6 rounded-2xl border border-red-500/30 bg-slate-900 shadow-2xl space-y-4">
            <h2 className="text-sm font-bold text-red-400 flex items-center gap-2">
              <UserX className="w-4 h-4 text-red-400" />
              Deactivate User Account
            </h2>
            <p className="text-xs text-slate-300">
              Are you sure you want to deactivate <strong className="text-white">{confirmDeactivateUser.email}</strong>?
            </p>
            <p className="text-[11px] text-slate-400 bg-red-500/10 p-2.5 rounded-xl border border-red-500/20">
              Deactivated accounts cannot authenticate or refresh tokens. Active JWT sessions will be immediately rejected on the next API call.
            </p>

            <div className="flex justify-end gap-2 pt-2">
              <button
                disabled={actionPending}
                onClick={() => setConfirmDeactivateUser(null)}
                className="px-3 py-1.5 rounded-xl bg-slate-800 text-slate-300 hover:bg-slate-700 text-xs"
              >
                Cancel
              </button>
              <button
                disabled={actionPending}
                onClick={() => handleToggleActive(confirmDeactivateUser)}
                className="px-4 py-1.5 rounded-xl bg-red-600 hover:bg-red-500 text-white font-semibold text-xs flex items-center gap-1.5"
              >
                {actionPending && <Loader2 className="w-3.5 h-3.5 animate-spin" />}
                Deactivate Account
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Password Reset Confirmation Modal */}
      {confirmResetUser && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 backdrop-blur-sm p-4">
          <div className="glass-card max-w-md w-full p-6 rounded-2xl border border-amber-500/30 bg-slate-900 shadow-2xl space-y-4">
            <h2 className="text-sm font-bold text-amber-400 flex items-center gap-2">
              <KeyRound className="w-4 h-4 text-amber-400" />
              Initiate Password Reset
            </h2>
            <p className="text-xs text-slate-300">
              Generate a secure 24-hour password reset token for <strong className="text-white">{confirmResetUser.email}</strong>?
            </p>
            <p className="text-[11px] text-slate-400 bg-amber-500/10 p-2.5 rounded-xl border border-amber-500/20">
              In accordance with security governance, raw credentials or reset tokens are never exposed in API responses or logs.
            </p>

            <div className="flex justify-end gap-2 pt-2">
              <button
                disabled={actionPending}
                onClick={() => setConfirmResetUser(null)}
                className="px-3 py-1.5 rounded-xl bg-slate-800 text-slate-300 hover:bg-slate-700 text-xs"
              >
                Cancel
              </button>
              <button
                disabled={actionPending}
                onClick={handlePasswordReset}
                className="px-4 py-1.5 rounded-xl bg-amber-600 hover:bg-amber-500 text-white font-semibold text-xs flex items-center gap-1.5"
              >
                {actionPending && <Loader2 className="w-3.5 h-3.5 animate-spin" />}
                Initiate Reset
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
