package com.learnpulse.backend.service;

import com.learnpulse.backend.dto.*;
import com.learnpulse.backend.entity.*;
import com.learnpulse.backend.exception.ApiException;
import com.learnpulse.backend.exception.ResourceNotFoundException;
import com.learnpulse.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EnrollmentService {

    private final TeacherSubjectRepository teacherSubjectRepository;
    private final StudentEnrollmentRepository studentEnrollmentRepository;
    private final SubjectRepository subjectRepository;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;

    // ==========================================
    // 1. ADMIN TEACHER-SUBJECT MANAGEMENT
    // ==========================================

    @Transactional
    public TeacherSubjectDTO assignTeacherToSubject(User admin, UUID subjectId, UUID teacherId) {
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found with ID: " + subjectId));

        User teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher user not found with ID: " + teacherId));

        if (teacher.getRole() != Role.TEACHER && teacher.getRole() != Role.ADMIN) {
            throw new ApiException("User with email '" + teacher.getEmail() + "' is not a teacher (role: " + teacher.getRole() + ")", HttpStatus.BAD_REQUEST);
        }

        if (teacherSubjectRepository.existsByTeacherIdAndSubjectId(teacherId, subjectId)) {
            throw new ApiException("Teacher is already assigned to subject '" + subject.getName() + "'", HttpStatus.CONFLICT);
        }

        TeacherSubject assignment = TeacherSubject.builder()
                .teacher(teacher)
                .subject(subject)
                .build();

        TeacherSubject saved = teacherSubjectRepository.save(assignment);
        log.info("Assigned teacher '{}' to subject '{}' (ID: {})", teacher.getEmail(), subject.getName(), subject.getId());

        int chapterCount = subject.getChapters() != null ? subject.getChapters().size() : 0;
        return TeacherSubjectDTO.builder()
                .subjectId(subject.getId())
                .subjectName(subject.getName())
                .subjectCode(subject.getCode())
                .subjectDescription(subject.getDescription())
                .chapterCount(chapterCount)
                .assignedAt(saved.getAssignedAt())
                .build();
    }

    @Transactional
    public void removeTeacherFromSubject(User admin, UUID subjectId, UUID teacherId) {
        if (!subjectRepository.existsById(subjectId)) {
            throw new ResourceNotFoundException("Subject not found with ID: " + subjectId);
        }

        TeacherSubject assignment = teacherSubjectRepository.findByTeacherIdAndSubjectId(teacherId, subjectId)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher assignment not found for subject ID: " + subjectId + " and teacher ID: " + teacherId));

        teacherSubjectRepository.delete(assignment);
        log.info("Removed teacher assignment (teacher: {}, subject: {})", teacherId, subjectId);
    }

    @Transactional(readOnly = true)
    public List<TeacherSubjectDTO> getTeacherSubjects(User teacher) {
        List<TeacherSubject> assignments;
        if (teacher.getRole() == Role.ADMIN) {
            // Admin can see all subjects
            return subjectRepository.findAll().stream()
                    .map(sub -> TeacherSubjectDTO.builder()
                            .subjectId(sub.getId())
                            .subjectName(sub.getName())
                            .subjectCode(sub.getCode())
                            .subjectDescription(sub.getDescription())
                            .chapterCount(sub.getChapters() != null ? sub.getChapters().size() : 0)
                            .assignedAt(sub.getCreatedAt())
                            .build())
                    .collect(Collectors.toList());
        } else {
            assignments = teacherSubjectRepository.findByTeacherId(teacher.getId());
            return assignments.stream()
                    .map(ts -> {
                        Subject sub = ts.getSubject();
                        int chapterCount = sub.getChapters() != null ? sub.getChapters().size() : 0;
                        return TeacherSubjectDTO.builder()
                                .subjectId(sub.getId())
                                .subjectName(sub.getName())
                                .subjectCode(sub.getCode())
                                .subjectDescription(sub.getDescription())
                                .chapterCount(chapterCount)
                                .assignedAt(ts.getAssignedAt())
                                .build();
                    })
                    .collect(Collectors.toList());
        }
    }

    @Transactional(readOnly = true)
    public List<TeacherProfileDTO> getSubjectTeachers(UUID subjectId) {
        if (!subjectRepository.existsById(subjectId)) {
            throw new ResourceNotFoundException("Subject not found with ID: " + subjectId);
        }

        List<TeacherSubject> assignments = teacherSubjectRepository.findBySubjectId(subjectId);
        return assignments.stream()
                .map(ts -> {
                    User teacher = ts.getTeacher();
                    Optional<UserProfile> profile = userProfileRepository.findByUserId(teacher.getId());
                    return TeacherProfileDTO.builder()
                            .id(profile.map(UserProfile::getId).orElse(null))
                            .userId(teacher.getId())
                            .email(teacher.getEmail())
                            .firstName(profile.map(UserProfile::getFirstName).orElse("Teacher"))
                            .lastName(profile.map(UserProfile::getLastName).orElse(""))
                            .department(profile.map(UserProfile::getDepartment).orElse(""))
                            .build();
                })
                .collect(Collectors.toList());
    }

    // ==========================================
    // 2. TEACHER-STUDENT ENROLLMENT MANAGEMENT
    // ==========================================

    @Transactional
    public EnrollmentDTO enrollStudentInSubject(User teacher, UUID subjectId, UUID studentId) {
        // Enforce ownership: Teacher must be assigned to this subject (unless ADMIN)
        if (teacher.getRole() != Role.ADMIN && !teacherSubjectRepository.existsByTeacherIdAndSubjectId(teacher.getId(), subjectId)) {
            throw new ApiException("Teacher is not assigned to manage enrollments for this subject", HttpStatus.FORBIDDEN);
        }

        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found with ID: " + subjectId));

        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student user not found with ID: " + studentId));

        if (student.getRole() != Role.STUDENT) {
            throw new ApiException("Cannot enroll user with role '" + student.getRole() + "'. Only STUDENTS can be enrolled.", HttpStatus.BAD_REQUEST);
        }

        Optional<StudentEnrollment> existingOpt = studentEnrollmentRepository.findByStudentIdAndSubjectId(studentId, subjectId);
        if (existingOpt.isPresent()) {
            StudentEnrollment existing = existingOpt.get();
            if (existing.getStatus() == EnrollmentStatus.ACTIVE) {
                throw new ApiException("Student '" + student.getEmail() + "' is already actively enrolled in subject '" + subject.getName() + "'", HttpStatus.CONFLICT);
            } else {
                // Reactivate existing enrollment (soft-delete reactivation)
                existing.setStatus(EnrollmentStatus.ACTIVE);
                StudentEnrollment reactivated = studentEnrollmentRepository.save(existing);
                log.info("Reactivated enrollment for student '{}' in subject '{}'", student.getEmail(), subject.getName());
                return mapToEnrollmentDTO(reactivated);
            }
        }

        StudentEnrollment enrollment = StudentEnrollment.builder()
                .student(student)
                .subject(subject)
                .status(EnrollmentStatus.ACTIVE)
                .build();

        StudentEnrollment saved = studentEnrollmentRepository.save(enrollment);
        log.info("Enrolled student '{}' in subject '{}' by teacher '{}'", student.getEmail(), subject.getName(), teacher.getEmail());
        return mapToEnrollmentDTO(saved);
    }

    @Transactional
    public void unenrollStudentFromSubject(User teacher, UUID subjectId, UUID studentId) {
        // Enforce ownership: Teacher must be assigned to this subject (unless ADMIN)
        if (teacher.getRole() != Role.ADMIN && !teacherSubjectRepository.existsByTeacherIdAndSubjectId(teacher.getId(), subjectId)) {
            throw new ApiException("Teacher is not assigned to manage enrollments for this subject", HttpStatus.FORBIDDEN);
        }

        if (!subjectRepository.existsById(subjectId)) {
            throw new ResourceNotFoundException("Subject not found with ID: " + subjectId);
        }

        StudentEnrollment enrollment = studentEnrollmentRepository.findByStudentIdAndSubjectId(studentId, subjectId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment record not found for student ID: " + studentId + " in subject ID: " + subjectId));

        // Soft-delete: Mark as INACTIVE to preserve historical quiz results, progress, and attempts
        enrollment.setStatus(EnrollmentStatus.INACTIVE);
        studentEnrollmentRepository.save(enrollment);
        log.info("Unenrolled student '{}' from subject '{}' (marked INACTIVE, historical results preserved)", studentId, subjectId);
    }

    @Transactional(readOnly = true)
    public List<StudentSummaryDTO> getStudentsForSubject(User teacher, UUID subjectId) {
        // Enforce ownership: Teacher must be assigned to this subject (unless ADMIN)
        if (teacher.getRole() != Role.ADMIN && !teacherSubjectRepository.existsByTeacherIdAndSubjectId(teacher.getId(), subjectId)) {
            throw new ApiException("Teacher is not assigned to view students for this subject", HttpStatus.FORBIDDEN);
        }

        if (!subjectRepository.existsById(subjectId)) {
            throw new ResourceNotFoundException("Subject not found with ID: " + subjectId);
        }

        List<StudentEnrollment> enrollments = studentEnrollmentRepository.findBySubjectId(subjectId);
        return enrollments.stream()
                .map(e -> {
                    User student = e.getStudent();
                    Optional<UserProfile> profile = userProfileRepository.findByUserId(student.getId());
                    return StudentSummaryDTO.builder()
                            .userId(student.getId())
                            .email(student.getEmail())
                            .firstName(profile.map(UserProfile::getFirstName).orElse("Student"))
                            .lastName(profile.map(UserProfile::getLastName).orElse(""))
                            .department(profile.map(UserProfile::getDepartment).orElse(""))
                            .enrollmentNumber(profile.map(UserProfile::getEnrollmentNumber).orElse(""))
                            .enrolledAt(e.getEnrolledAt())
                            .status(e.getStatus())
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<StudentSummaryDTO> getAllStudentsForTeacher(User teacher) {
        // Return safe student summaries for student enrollment picker
        List<User> students = userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.STUDENT && u.isEnabled())
                .collect(Collectors.toList());

        return students.stream()
                .map(s -> {
                    Optional<UserProfile> profile = userProfileRepository.findByUserId(s.getId());
                    return StudentSummaryDTO.builder()
                            .userId(s.getId())
                            .email(s.getEmail())
                            .firstName(profile.map(UserProfile::getFirstName).orElse("Student"))
                            .lastName(profile.map(UserProfile::getLastName).orElse(""))
                            .department(profile.map(UserProfile::getDepartment).orElse(""))
                            .enrollmentNumber(profile.map(UserProfile::getEnrollmentNumber).orElse(""))
                            .build();
                })
                .collect(Collectors.toList());
    }

    // ==========================================
    // 3. STUDENT ENROLLED SUBJECTS ACCESS
    // ==========================================

    @Transactional(readOnly = true)
    public List<EnrolledSubjectDTO> getEnrolledSubjectsForStudent(User student) {
        List<StudentEnrollment> enrollments = studentEnrollmentRepository.findByStudentIdAndStatus(student.getId(), EnrollmentStatus.ACTIVE);

        return enrollments.stream()
                .map(e -> {
                    Subject sub = e.getSubject();
                    int chapterCount = sub.getChapters() != null ? sub.getChapters().size() : 0;

                    // Look up assigned teacher
                    List<TeacherSubject> teachers = teacherSubjectRepository.findBySubjectId(sub.getId());
                    String teacherFirstName = null;
                    String teacherLastName = null;
                    String teacherEmail = null;

                    if (!teachers.isEmpty()) {
                        User t = teachers.get(0).getTeacher();
                        teacherEmail = t.getEmail();
                        Optional<UserProfile> tProfile = userProfileRepository.findByUserId(t.getId());
                        teacherFirstName = tProfile.map(UserProfile::getFirstName).orElse("Faculty");
                        teacherLastName = tProfile.map(UserProfile::getLastName).orElse("");
                    }

                    return EnrolledSubjectDTO.builder()
                            .subjectId(sub.getId())
                            .subjectName(sub.getName())
                            .subjectCode(sub.getCode())
                            .subjectDescription(sub.getDescription())
                            .chapterCount(chapterCount)
                            .status(e.getStatus())
                            .enrolledAt(e.getEnrolledAt())
                            .teacherFirstName(teacherFirstName)
                            .teacherLastName(teacherLastName)
                            .teacherEmail(teacherEmail)
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<EnrollmentDTO> getMyEnrollments(User student) {
        List<StudentEnrollment> enrollments = studentEnrollmentRepository.findByStudentId(student.getId());
        return enrollments.stream()
                .map(this::mapToEnrollmentDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public boolean isStudentActivelyEnrolled(UUID studentId, UUID subjectId) {
        if (studentId == null || subjectId == null) {
            return false;
        }
        return studentEnrollmentRepository.existsByStudentIdAndSubjectIdAndStatus(studentId, subjectId, EnrollmentStatus.ACTIVE);
    }

    @Transactional(readOnly = true)
    public boolean isTeacherAssignedToSubject(UUID teacherId, UUID subjectId) {
        if (teacherId == null || subjectId == null) {
            return false;
        }
        return teacherSubjectRepository.existsByTeacherIdAndSubjectId(teacherId, subjectId);
    }

    private EnrollmentDTO mapToEnrollmentDTO(StudentEnrollment enrollment) {
        User student = enrollment.getStudent();
        Subject subject = enrollment.getSubject();
        Optional<UserProfile> profile = userProfileRepository.findByUserId(student.getId());

        return EnrollmentDTO.builder()
                .id(enrollment.getId())
                .studentId(student.getId())
                .studentEmail(student.getEmail())
                .studentFirstName(profile.map(UserProfile::getFirstName).orElse("Student"))
                .studentLastName(profile.map(UserProfile::getLastName).orElse(""))
                .enrollmentNumber(profile.map(UserProfile::getEnrollmentNumber).orElse(""))
                .subjectId(subject.getId())
                .subjectName(subject.getName())
                .status(enrollment.getStatus())
                .enrolledAt(enrollment.getEnrolledAt())
                .build();
    }
}
