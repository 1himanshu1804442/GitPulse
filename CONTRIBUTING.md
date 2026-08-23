# Contributing to GitPulse

Thank you for your interest in contributing to **GitPulse**! We are building a high-performance GitHub intelligence and developer productivity telemetry dashboard.

Whether you are a seasoned engineer or a beginner taking your first steps, your contributions are warmly welcome.

---

## 🎯 Guiding Principles

Our codebase adheres strictly to three core tenets:
1. **Organization:** Strict modular separation of concerns.
2. **Readability:** Clear, descriptive code with no clever one-liners or missing implementations.
3. **Debuggability:** Verbose structured logging and explicit error handling.

---

## 🛠️ Tech Stack Overview

- **Backend:** Java 17 + Spring Boot 3.2 + Spring Data JPA + PostgreSQL 16
- **Frontend:** React 18 + TypeScript + Vite + Tailwind CSS + Lucide Icons
- **DevOps:** Docker Compose + GitHub Actions CI + Windows PowerShell 1-Click Runner (`dev.ps1`)

---

## 🚀 Quick Development Setup

### 1. Prerequisites
- **JDK 17+** (e.g., Eclipse Temurin 17)
- **Node.js 20+** & **npm**
- **Docker Desktop** (Optional - if unavailable, the dev runner seamlessly boots an in-memory H2 database)

### 2. Running Locally (1 Command)
Open PowerShell in the repository root and run:
```powershell
.\dev.ps1
```
This script will:
1. Auto-detect or provision PostgreSQL via Docker (or fall back to H2).
2. Start the Spring Boot backend on [http://localhost:8080](http://localhost:8080).
3. Start the React Vite frontend on [http://localhost:3000](http://localhost:3000).
4. Stream synchronized, color-coded unified logs.

---

## 📐 Architecture & Coding Standards

### ☕ Backend Standards (Java Spring Boot)
- **Strict Layered Architecture:**
  - `controller/`: REST Controllers only (`@RestController`). No direct business logic or DB calls.
  - `service/`: Business logic layer interfaces and implementations.
  - `repository/`: Spring Data JPA repositories extending `JpaRepository`.
  - `model/` or `entity/`: JPA entities mapped to PostgreSQL tables.
  - `dto/`: Request/Response Data Transfer Objects with validation annotations (`@Valid`, `@NotNull`, etc.).
  - `exception/`: Custom domain exceptions handled by `@ControllerAdvice` / `@ExceptionHandler`.
- **Dependency Injection:** Always use constructor injection (preferred via Lombok `@RequiredArgsConstructor` or explicit constructor). Never use field `@Autowired`.
- **Verbose Logging:** Use `@Slf4j`. Add `log.info()` for major state transitions and `log.error("...", ex)` with stack traces for exceptions.
- **No Placeholders:** Write complete, functioning methods. Never leave `// TODO` or empty catch blocks.

### ⚛️ Frontend Standards (React)
- **Strict Folder Structure:**
  - `src/components/`: Reusable presentational & UI components.
  - `src/pages/`: Page-level route views.
  - `src/services/`: API interaction layer (Axios / Fetch). Never write direct API calls inside UI components!
  - `src/hooks/`: Custom React hooks for shared stateful logic.
  - `src/types/`: TypeScript type and interface definitions.
- **Functional Components:** Use functional components with hooks (`useState`, `useEffect`, `useMemo`, `useCallback`).
- **Styling Reset (Seamless Bounce):** Ensure `html, body, #root` have `margin: 0; padding: 0; min-height: 100vh;` and match the dark theme background.
- **Error Handling:** Catch and display informative UI states for failed API calls, and log errors to `console.error` with contextual information.

---

## 🌿 Git Branch & PR Workflow

1. **Fork or branch:**
   ```bash
   git checkout -b feature/your-feature-name
   # or
   git checkout -b fix/bug-description
   ```
2. **Make your changes:**
   - Write clear commit messages following Conventional Commits:
     - `feat: add commit velocity telemetry endpoint`
     - `fix: resolve null pointer in user pull request parser`
     - `docs: update API documentation in README`
3. **Verify builds & tests:**
   ```bash
   # Backend verification
   cd backend-spring
   ./mvnw clean test

   # Frontend verification
   cd ../frontend
   npm run lint
   npm run build
   ```
4. **Open a Pull Request:**
   - Provide a clear summary of what changes were made and why.
   - Attach screenshots if you modified UI components.

---

## 💬 Getting Help

If you run into issues, have questions, or want to discuss a new feature proposal, feel free to open a [GitHub Issue](https://github.com/gitpulse/gitpulse/issues) or reach out to the maintainers.
