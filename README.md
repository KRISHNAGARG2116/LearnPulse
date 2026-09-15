# LearnPulse LMS — AI-Powered Learning Management System

LearnPulse LMS is an enterprise-grade AI-powered Learning Management System featuring a context-aware assessment engine, automated document ingestion, adaptive course progression, and a modern single-page React frontend.

---

## 🚀 Tech Stack

### Frontend (Week 7 Infrastructure)
- **Framework**: React 19 (TypeScript)
- **Build Tool**: Vite 6
- **Styling**: Tailwind CSS v4, Custom CSS Design System, Glassmorphism UI
- **Routing**: React Router v7 (`BrowserRouter`, `Routes`, `Route`, `Navigate`)
- **State & Form Management**: React Context (`AuthContext`), React Hook Form
- **HTTP Client**: Axios with JWT Request Interceptor & Automatic Token Refresh
- **Icons**: Lucide React

### Backend Infrastructure (Week 2–7)
- **Framework**: Spring Boot 3.2.5 (Java 21)
- **Database**: PostgreSQL 16 (Spring Data JPA / Hibernate)
- **Security**: Spring Security 6 with JJWT (Stateless JWT Authentication, RBAC)
- **Document Processing**: Apache Tika 2.9.2 & Apache PDFBox 2.0.30
- **API Documentation**: SpringDoc OpenAPI / Swagger UI (`/swagger-ui.html`)

---

## 📚 Domain & Course Progression Architecture

The domain hierarchy models academic course structures and enforces mastery-based progression:

```
Subject (Course Container)
  ├── Chapter 1: Basics
  │     └── Chapter Quiz 1 (Passing: 80%) [UNLOCKED BY DEFAULT]
  ├── Chapter 2: Object-Oriented Programming
  │     └── Chapter Quiz 2 (Passing: 80%) [LOCKED until Quiz 1 Passed]
  └── Final Course Assessment (Passing: 75%) [LOCKED until All Chapter Quizzes Passed]
```

### Progression Rules (Enforced Server-Side)
1. **Initial Access**: Chapter 1 is unlocked by default for enrolled students.
2. **Sequential Unlocking**: Passing Chapter $N$ quiz with score $\ge 80\%$ unlocks Chapter $N+1$.
3. **Final Course Assessment**: Passing all chapter quizzes unlocks the Final Course Quiz.
4. **Course Completion**: Scoring $\ge 75\%$ on the Final Course Quiz marks the course status as `COMPLETED`.
5. **Access Control**: Direct access to locked quizzes via API returns `403 Forbidden`.

---

## 🔑 Authentication & Authorization (RBAC)

Supported Roles: `STUDENT`, `TEACHER`, `ADMIN`.

- **JWT Tokens**: Short-lived Access Token (15 min) + Refresh Token (7 days).
- **Interceptors**: Frontend Axios client automatically injects `Bearer <token>` headers and handles token refresh on HTTP 401 responses.
- **Registration Security**: Public registration allows standard user registration for `STUDENT` and `TEACHER` roles. Administrative `ADMIN` role registration is strictly restricted.

---

## 🛠️ Project Setup & Installation

### Prerequisites
- **Node.js**: v18+ (Node 25 recommended)
- **Java**: JDK 21
- **Database**: PostgreSQL 16 (listening on `localhost:5432` for `learning_assistant_db`)

### Frontend Installation & Build
```bash
# Install NPM dependencies
npm install

# Start development server
npm run dev

# Run TypeScript type check and production build
npm run build
```

### Backend Installation & Verification
```bash
# Build backend and run JUnit integration test suite
mvn clean test
```

---

## 🌐 Key Frontend Routes

| Route | Role | Description |
|---|---|---|
| `/login` | Public | User authentication login page |
| `/register` | Public | User registration page (Student / Teacher options) |
| `/student/dashboard` | `STUDENT` | Course progression overview & quiz launcher |
| `/student/practice` | `STUDENT` | Single scrollable quiz practice launcher |
| `/student/results` | `STUDENT` | Assessment attempt analytics |
| `/student/profile` | `STUDENT` | Student account profile details |
| `/teacher/dashboard` | `TEACHER`, `ADMIN` | Faculty command console |
| `/teacher/questions` | `TEACHER`, `ADMIN` | Manual question authoring & AI priority inputs |
| `/admin/dashboard` | `ADMIN` | System administrator dashboard |
| `/admin/users` | `ADMIN` | System user management |

---

## 📄 License & Phase Classification

Project Phase: **WEEK 7 IMPLEMENTATION**  
Component: **React Client Setup & Course Progression Extension**
