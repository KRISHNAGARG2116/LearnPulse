import React, { useEffect, useState } from 'react';
import {
  getTeacherSubjects,
  getTeacherEnrolledStudents,
  getTeacherAvailableStudents,
  enrollStudentInSubject,
  unenrollStudentFromSubject,
} from '../../api/lmsApi';
import { TeacherSubjectDTO, StudentSummaryDTO } from '../../types/lms';
import {
  GraduationCap,
  BookOpen,
  UserPlus,
  UserX,
  Search,
  Loader2,
  AlertCircle,
  CheckCircle2,
  Building,
  Mail,
  User,
  X,
} from 'lucide-react';

export const TeacherStudentsPage: React.FC = () => {
  const [subjects, setSubjects] = useState<TeacherSubjectDTO[]>([]);
  const [selectedSubjectId, setSelectedSubjectId] = useState<string>('');
  const [enrolledStudents, setEnrolledStudents] = useState<StudentSummaryDTO[]>([]);
  const [availableStudents, setAvailableStudents] = useState<StudentSummaryDTO[]>([]);

  const [loadingSubjects, setLoadingSubjects] = useState<boolean>(true);
  const [loadingStudents, setLoadingStudents] = useState<boolean>(false);
  const [actionLoading, setActionLoading] = useState<boolean>(false);

  const [error, setError] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  // Modal State
  const [isEnrollModalOpen, setIsEnrollModalOpen] = useState<boolean>(false);
  const [selectedStudentToEnroll, setSelectedStudentToEnroll] = useState<string>('');
  const [searchFilter, setSearchFilter] = useState<string>('');
  const [availableSearchFilter, setAvailableSearchFilter] = useState<string>('');

  // 1. Fetch Teacher's Assigned Subjects
  useEffect(() => {
    const fetchAssignedSubjects = async () => {
      setLoadingSubjects(true);
      setError(null);
      try {
        const data = await getTeacherSubjects();
        setSubjects(data);
        if (data.length > 0) {
          setSelectedSubjectId(data[0].subjectId);
        }
      } catch (err: any) {
        setError(err.message || 'Failed to load assigned subjects.');
      } finally {
        setLoadingSubjects(false);
      }
    };
    fetchAssignedSubjects();
  }, []);

  // 2. Fetch Enrolled & Available Students when subject changes
  const fetchStudentsForSubject = async (subjectId: string) => {
    if (!subjectId) return;
    setLoadingStudents(true);
    setError(null);
    try {
      const [enrolled, available] = await Promise.all([
        getTeacherEnrolledStudents(subjectId),
        getTeacherAvailableStudents(subjectId),
      ]);
      setEnrolledStudents(enrolled);
      setAvailableStudents(available);
    } catch (err: any) {
      setError(err.message || 'Failed to load students for the selected subject.');
    } finally {
      setLoadingStudents(false);
    }
  };

  useEffect(() => {
    if (selectedSubjectId) {
      fetchStudentsForSubject(selectedSubjectId);
    }
  }, [selectedSubjectId]);

  // Handle Enrollment
  const handleEnroll = async () => {
    if (!selectedSubjectId || !selectedStudentToEnroll) return;
    setActionLoading(true);
    setError(null);
    setSuccessMessage(null);
    try {
      await enrollStudentInSubject(selectedSubjectId, { studentId: selectedStudentToEnroll });
      setSuccessMessage('Student enrolled successfully!');
      setIsEnrollModalOpen(false);
      setSelectedStudentToEnroll('');
      await fetchStudentsForSubject(selectedSubjectId);
    } catch (err: any) {
      setError(err.message || 'Failed to enroll student.');
    } finally {
      setActionLoading(false);
    }
  };

  // Handle Unenrollment
  const handleUnenroll = async (studentId: string, studentName: string) => {
    if (!selectedSubjectId) return;
    const confirmed = window.confirm(`Are you sure you want to unenroll ${studentName}? Historical quiz results will be preserved.`);
    if (!confirmed) return;

    setActionLoading(true);
    setError(null);
    setSuccessMessage(null);
    try {
      await unenrollStudentFromSubject(selectedSubjectId, studentId);
      setSuccessMessage(`${studentName} was unenrolled successfully.`);
      await fetchStudentsForSubject(selectedSubjectId);
    } catch (err: any) {
      setError(err.message || 'Failed to unenroll student.');
    } finally {
      setActionLoading(false);
    }
  };

  const selectedSubject = subjects.find((s) => s.subjectId === selectedSubjectId);

  // Filtered lists
  const filteredEnrolled = enrolledStudents.filter((s) => {
    const term = searchFilter.toLowerCase();
    const fullName = `${s.firstName || ''} ${s.lastName || ''}`.toLowerCase();
    const email = (s.email || '').toLowerCase();
    const dept = (s.department || '').toLowerCase();
    const enrollNo = (s.enrollmentNumber || '').toLowerCase();
    return fullName.includes(term) || email.includes(term) || dept.includes(term) || enrollNo.includes(term);
  });

  const filteredAvailable = availableStudents.filter((s) => {
    const term = availableSearchFilter.toLowerCase();
    const fullName = `${s.firstName || ''} ${s.lastName || ''}`.toLowerCase();
    const email = (s.email || '').toLowerCase();
    const dept = (s.department || '').toLowerCase();
    const enrollNo = (s.enrollmentNumber || '').toLowerCase();
    return fullName.includes(term) || email.includes(term) || dept.includes(term) || enrollNo.includes(term);
  });

  if (loadingSubjects) {
    return (
      <div className="flex flex-col items-center justify-center min-h-[400px] gap-4">
        <Loader2 className="w-10 h-10 text-indigo-400 animate-spin" />
        <p className="text-slate-400 text-sm font-medium">Loading assigned subjects...</p>
      </div>
    );
  }

  return (
    <div className="space-y-8">
      {/* Header Banner */}
      <div className="glass-panel rounded-2xl p-6 md:p-8 border border-indigo-500/20 relative overflow-hidden">
        <div className="absolute -right-10 -bottom-10 w-64 h-64 bg-indigo-500/10 rounded-full blur-3xl pointer-events-none" />
        <div className="relative z-10 flex flex-col md:flex-row md:items-center justify-between gap-6">
          <div>
            <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-indigo-500/10 text-indigo-400 border border-indigo-500/20 text-xs font-semibold mb-3">
              <GraduationCap className="w-3.5 h-3.5" />
              <span>Student Enrollment Management</span>
            </div>
            <h1 className="text-2xl md:text-3xl font-extrabold text-white tracking-tight">
              Class & Student Enrollment
            </h1>
            <p className="text-slate-400 text-sm mt-1 max-w-xl">
              Manage student enrollments for your assigned academic subjects. Enrolled students gain immediate access to chapters, materials, and quizzes.
            </p>
          </div>
          {subjects.length > 0 && (
            <button
              onClick={() => {
                setAvailableSearchFilter('');
                setSelectedStudentToEnroll('');
                setIsEnrollModalOpen(true);
              }}
              disabled={actionLoading || !selectedSubjectId}
              className="glow-button px-5 py-3 rounded-xl font-bold text-white text-sm inline-flex items-center gap-2 shadow-lg shadow-indigo-500/25 shrink-0"
            >
              <UserPlus className="w-4 h-4" />
              <span>Enroll Student</span>
            </button>
          )}
        </div>
      </div>

      {/* Notifications */}
      {error && (
        <div className="p-4 rounded-xl bg-rose-500/10 border border-rose-500/20 flex items-center justify-between text-rose-400 text-sm">
          <div className="flex items-center gap-3">
            <AlertCircle className="w-5 h-5 shrink-0" />
            <span>{error}</span>
          </div>
          <button onClick={() => setError(null)} className="text-rose-400 hover:text-white">
            <X className="w-4 h-4" />
          </button>
        </div>
      )}

      {successMessage && (
        <div className="p-4 rounded-xl bg-emerald-500/10 border border-emerald-500/20 flex items-center justify-between text-emerald-400 text-sm">
          <div className="flex items-center gap-3">
            <CheckCircle2 className="w-5 h-5 shrink-0" />
            <span>{successMessage}</span>
          </div>
          <button onClick={() => setSuccessMessage(null)} className="text-emerald-400 hover:text-white">
            <X className="w-4 h-4" />
          </button>
        </div>
      )}

      {/* If No Subjects Assigned */}
      {subjects.length === 0 ? (
        <div className="glass-panel rounded-2xl p-12 text-center border border-dashed border-slate-800 space-y-4">
          <BookOpen className="w-12 h-12 text-slate-600 mx-auto" />
          <h3 className="text-lg font-bold text-white">No Subjects Assigned Yet</h3>
          <p className="text-slate-400 text-sm max-w-md mx-auto">
            You currently have no academic subjects assigned to your teacher account. An administrator must assign subjects to you before you can manage student enrollments.
          </p>
        </div>
      ) : (
        <div className="space-y-6">
          {/* Subject Selector Tabs / Cards */}
          <div className="glass-panel rounded-2xl p-5 border border-slate-800 space-y-4">
            <div className="flex items-center justify-between">
              <label className="text-xs font-semibold text-slate-400 uppercase tracking-wider">
                Select Assigned Subject
              </label>
              <span className="text-xs text-indigo-400 font-medium">
                {subjects.length} assigned {subjects.length === 1 ? 'subject' : 'subjects'}
              </span>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-3">
              {subjects.map((sub) => {
                const isSelected = sub.subjectId === selectedSubjectId;
                return (
                  <button
                    key={sub.subjectId}
                    type="button"
                    onClick={() => setSelectedSubjectId(sub.subjectId)}
                    className={`p-4 rounded-xl text-left transition-all border ${
                      isSelected
                        ? 'bg-indigo-600/20 border-indigo-500/60 text-white shadow-lg shadow-indigo-500/10'
                        : 'bg-slate-900/60 border-slate-800 text-slate-300 hover:border-slate-700 hover:text-white'
                    }`}
                  >
                    <div className="flex items-center justify-between mb-1.5">
                      <span className="px-2 py-0.5 rounded bg-indigo-500/10 text-indigo-400 border border-indigo-500/20 text-[11px] font-mono font-bold">
                        {sub.subjectCode}
                      </span>
                      <span className="text-[11px] text-slate-500">{sub.chapterCount} chapters</span>
                    </div>
                    <p className="font-bold text-sm truncate">{sub.subjectName}</p>
                    {sub.subjectDescription && (
                      <p className="text-xs text-slate-400 line-clamp-1 mt-1">{sub.subjectDescription}</p>
                    )}
                  </button>
                );
              })}
            </div>
          </div>

          {/* Enrolled Students Management Section */}
          <div className="glass-panel rounded-2xl p-6 border border-slate-800 space-y-5">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
              <div>
                <h2 className="text-lg font-bold text-white flex items-center gap-2">
                  <span>Enrolled Students</span>
                  <span className="px-2.5 py-0.5 rounded-full bg-indigo-500/20 text-indigo-300 text-xs font-semibold">
                    {enrolledStudents.length}
                  </span>
                </h2>
                <p className="text-xs text-slate-400 mt-0.5">
                  Subject: <span className="font-semibold text-slate-200">{selectedSubject?.subjectName}</span> ({selectedSubject?.subjectCode})
                </p>
              </div>

              {/* Search Bar */}
              <div className="relative w-full sm:w-64">
                <Search className="w-4 h-4 text-slate-500 absolute left-3 top-1/2 -translate-y-1/2" />
                <input
                  type="text"
                  placeholder="Filter students..."
                  value={searchFilter}
                  onChange={(e) => setSearchFilter(e.target.value)}
                  className="w-full bg-slate-900 border border-slate-800 rounded-xl pl-9 pr-3 py-2 text-xs text-slate-100 placeholder-slate-500 focus:outline-none focus:border-indigo-500"
                />
              </div>
            </div>

            {loadingStudents ? (
              <div className="flex flex-col items-center justify-center py-12 gap-3">
                <Loader2 className="w-8 h-8 text-indigo-400 animate-spin" />
                <p className="text-slate-400 text-xs">Loading enrolled students...</p>
              </div>
            ) : filteredEnrolled.length === 0 ? (
              <div className="p-8 text-center border border-dashed border-slate-800 rounded-xl space-y-3">
                <GraduationCap className="w-10 h-10 text-slate-600 mx-auto" />
                <p className="text-slate-300 text-sm font-semibold">
                  {searchFilter ? 'No matching students found' : 'No students enrolled in this subject'}
                </p>
                <p className="text-slate-500 text-xs max-w-sm mx-auto">
                  {searchFilter
                    ? 'Try adjusting your search criteria.'
                    : 'Click "Enroll Student" above to add students from your institution into this course.'}
                </p>
              </div>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-left text-xs text-slate-300">
                  <thead className="text-[11px] uppercase font-bold text-slate-400 border-b border-slate-800 bg-slate-900/50">
                    <tr>
                      <th className="py-3 px-4">Student</th>
                      <th className="py-3 px-4">Department</th>
                      <th className="py-3 px-4">Enrollment No.</th>
                      <th className="py-3 px-4">Enrolled Date</th>
                      <th className="py-3 px-4">Status</th>
                      <th className="py-3 px-4 text-right">Actions</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-800/60">
                    {filteredEnrolled.map((student) => {
                      const fullName = student.firstName
                        ? `${student.firstName} ${student.lastName || ''}`
                        : 'Student';
                      return (
                        <tr key={student.userId} className="hover:bg-slate-900/40 transition-colors">
                          <td className="py-3.5 px-4">
                            <div className="flex items-center gap-3">
                              <div className="w-8 h-8 rounded-lg bg-indigo-500/20 border border-indigo-500/30 flex items-center justify-center text-indigo-300 font-bold shrink-0">
                                {fullName.charAt(0)}
                              </div>
                              <div>
                                <p className="font-semibold text-white">{fullName}</p>
                                <p className="text-[11px] text-slate-400 flex items-center gap-1">
                                  <Mail className="w-3 h-3 text-slate-500" />
                                  <span>{student.email}</span>
                                </p>
                              </div>
                            </div>
                          </td>
                          <td className="py-3.5 px-4 text-slate-300">
                            {student.department ? (
                              <span className="inline-flex items-center gap-1">
                                <Building className="w-3 h-3 text-slate-500" />
                                <span>{student.department}</span>
                              </span>
                            ) : (
                              <span className="text-slate-600 italic">Not set</span>
                            )}
                          </td>
                          <td className="py-3.5 px-4 text-slate-300 font-mono">
                            {student.enrollmentNumber || <span className="text-slate-600 italic">N/A</span>}
                          </td>
                          <td className="py-3.5 px-4 text-slate-400">
                            {student.enrolledAt ? new Date(student.enrolledAt).toLocaleDateString() : '—'}
                          </td>
                          <td className="py-3.5 px-4">
                            <span className="px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
                              ACTIVE
                            </span>
                          </td>
                          <td className="py-3.5 px-4 text-right">
                            <button
                              onClick={() => handleUnenroll(student.userId, fullName)}
                              disabled={actionLoading}
                              className="px-3 py-1.5 rounded-lg bg-rose-500/10 text-rose-400 hover:bg-rose-500/20 border border-rose-500/20 font-semibold transition-all inline-flex items-center gap-1.5"
                              title="Unenroll student (historical results preserved)"
                            >
                              <UserX className="w-3.5 h-3.5" />
                              <span>Unenroll</span>
                            </button>
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        </div>
      )}

      {/* Enroll Student Modal */}
      {isEnrollModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/80 backdrop-blur-sm">
          <div className="glass-panel w-full max-w-lg rounded-2xl border border-slate-700 bg-slate-900 p-6 space-y-5 shadow-2xl animate-in fade-in zoom-in-95 duration-200">
            <div className="flex items-center justify-between border-b border-slate-800 pb-3">
              <div className="flex items-center gap-2">
                <UserPlus className="w-5 h-5 text-indigo-400" />
                <h3 className="text-lg font-bold text-white">Enroll Student into Subject</h3>
              </div>
              <button
                onClick={() => setIsEnrollModalOpen(false)}
                className="text-slate-400 hover:text-white transition-colors"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="space-y-1">
              <p className="text-xs text-slate-400">
                Target Subject: <span className="font-semibold text-white">{selectedSubject?.subjectName}</span> ({selectedSubject?.subjectCode})
              </p>
              <p className="text-xs text-slate-500">
                Only safe profile details are displayed. Passwords and credentials remain fully protected.
              </p>
            </div>

            {/* Available Students Search */}
            <div className="relative">
              <Search className="w-4 h-4 text-slate-500 absolute left-3 top-1/2 -translate-y-1/2" />
              <input
                type="text"
                placeholder="Search eligible students by name, email, or dept..."
                value={availableSearchFilter}
                onChange={(e) => setAvailableSearchFilter(e.target.value)}
                className="w-full bg-slate-950 border border-slate-800 rounded-xl pl-9 pr-3 py-2.5 text-xs text-slate-100 placeholder-slate-500 focus:outline-none focus:border-indigo-500"
              />
            </div>

            {/* Student Picker List */}
            <div className="max-h-60 overflow-y-auto space-y-2 pr-1">
              {filteredAvailable.length === 0 ? (
                <div className="p-6 text-center border border-dashed border-slate-800 rounded-xl space-y-2">
                  <User className="w-8 h-8 text-slate-600 mx-auto" />
                  <p className="text-slate-400 text-xs">
                    {availableSearchFilter ? 'No eligible students match your filter.' : 'All registered students are already enrolled.'}
                  </p>
                </div>
              ) : (
                filteredAvailable.map((st) => {
                  const fullName = st.firstName ? `${st.firstName} ${st.lastName || ''}` : 'Student';
                  const isChosen = selectedStudentToEnroll === st.userId;
                  return (
                    <div
                      key={st.userId}
                      onClick={() => setSelectedStudentToEnroll(st.userId)}
                      className={`p-3 rounded-xl border cursor-pointer transition-all flex items-center justify-between ${
                        isChosen
                          ? 'bg-indigo-600/20 border-indigo-500 text-white shadow-md'
                          : 'bg-slate-950/60 border-slate-800 text-slate-300 hover:border-slate-700'
                      }`}
                    >
                      <div className="flex items-center gap-3">
                        <div className="w-7 h-7 rounded-lg bg-indigo-500/20 border border-indigo-500/30 flex items-center justify-center text-indigo-300 text-xs font-bold">
                          {fullName.charAt(0)}
                        </div>
                        <div>
                          <p className="font-semibold text-xs text-white">{fullName}</p>
                          <p className="text-[11px] text-slate-400">{st.email}</p>
                          {st.department && (
                            <p className="text-[10px] text-slate-500">Dept: {st.department}</p>
                          )}
                        </div>
                      </div>
                      <div className="text-right">
                        {st.enrollmentNumber && (
                          <span className="text-[10px] font-mono bg-slate-800 px-2 py-0.5 rounded text-slate-300">
                            {st.enrollmentNumber}
                          </span>
                        )}
                      </div>
                    </div>
                  );
                })
              )}
            </div>

            {/* Actions */}
            <div className="flex items-center justify-end gap-3 pt-3 border-t border-slate-800">
              <button
                type="button"
                onClick={() => setIsEnrollModalOpen(false)}
                className="px-4 py-2 rounded-xl border border-slate-800 text-slate-400 hover:text-white text-xs font-semibold"
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={handleEnroll}
                disabled={actionLoading || !selectedStudentToEnroll}
                className="glow-button px-5 py-2 rounded-xl text-white font-bold text-xs inline-flex items-center gap-2 shadow-lg shadow-indigo-500/20 disabled:opacity-50"
              >
                {actionLoading && <Loader2 className="w-3.5 h-3.5 animate-spin" />}
                <span>Confirm Enrollment</span>
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
