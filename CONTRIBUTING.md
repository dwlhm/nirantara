# Contributing to Nirantara

Thank you for your interest in contributing to **Nirantara**! We welcome all forms of contributions: bug reports, documentation enhancements, feature proposals, and pull requests.

---

## Code of Conduct

All contributors and maintainers are expected to abide by our [Code of Conduct](CODE_OF_CONDUCT.md). Please read it before participating in our discussions and development.

---

## Branch Strategy

- **`main`**: Production-ready branch. Releases and stable tagged versions originate here.
- **`develop` / Feature Branches**: Create feature branches branching off `main` with descriptive prefixes:
  - `feat/<feature-name>` for new features
  - `fix/<bug-name>` for bug fixes
  - `refactor/<scope>` for code refactoring
  - `docs/<topic>` for documentation improvements
  - `ci/<workflow>` for CI/CD updates

---

## Commit Message Guidelines (Conventional Commits)

We follow the [Conventional Commits](https://www.conventionalcommits.org/) specification:

```text
<type>(<optional scope>): <description>

[optional body]

[optional footer(s)]
```

### Allowed Types:
- `feat`: A new feature
- `fix`: A bug fix
- `docs`: Documentation only changes
- `style`: Changes that do not affect the meaning of the code (white-space, formatting, etc.)
- `refactor`: A code change that neither fixes a bug nor adds a feature
- `perf`: A code change that improves performance
- `test`: Adding missing tests or correcting existing tests
- `build`: Changes that affect the build system or external dependencies (Gradle, version catalogs)
- `ci`: Changes to CI configuration files and scripts (GitHub Actions)
- `chore`: Other changes that don't modify src or test files

### Example:
```text
feat(scrollbar): add haptic vibration intensity slider
fix(search): resolve crash when searching non-ascii characters
docs(readme): add release signing setup details
```

---

## Code Style & Formatting

- **Kotlin**: Follow official [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html) and Android Kotlin styling.
- **Jetpack Compose**: Adhere to [Compose API Guidelines](https://github.com/androidx/androidx/blob/androidx-main/compose/docs/compose-api-guidelines.md). Composable functions should be PascalCase, and state hoisting must be preserved.
- **No Unused Imports**: Ensure spotless code hygiene by removing unused imports and dead code.
- **Architecture**: Follow MVVM + Unidirectional Data Flow (UDF) patterns. State should remain immutable and handled via StateFlow.

---

## Development & Verification Workflow

Before submitting a Pull Request, run the local verification suite:

```bash
# 1. Run Kotlin & Android Lint
./gradlew lintDebug

# 2. Run all unit tests
./gradlew testDebugUnitTest

# 3. Assemble debug APK to ensure compilation succeeds
./gradlew assembleDebug
```

---

## Submitting a Pull Request

1. **Fork** the repository and create your branch from `main`.
2. **Implement** your changes following our code and commit style.
3. **Verify** all unit tests and lint checks pass locally.
4. **Push** your branch to your fork.
5. **Open a Pull Request** against `main` using our standardized Pull Request template.
6. Provide a clear description of the problem solved, architectural rationale, and include screenshots/videos if UI changes are involved.
7. Engage constructively in code review discussions!
