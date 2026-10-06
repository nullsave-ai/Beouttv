# Beouttv - Agent Guidelines & Engineering Standards (ECC & Antigravity)

This repository follows the **Everything Claude Code (ECC)** engineering standards adapted for **Google Antigravity**, **Android Kotlin development**, and mobile building environments (**Termux**, **CodeOnTheGo**, and **AIDE**).

---

## 1. Core Workflow (Plan → Implement → Verify → Secure)

1. **Plan First**: For any feature or refactor affecting multiple files, outline the architecture and impact before making changes.
2. **Modular Changes**: Keep edits focused, self-contained, and easy to revert.
3. **Verification Loop**: After modifying code, verify syntax and build integrity:
   - Ensure changes compile cleanly within **AIDE** and **CodeOnTheGo** on Android.
   - Run optional quick syntax or Gradle checks via Termux (`./gradlew check` or `./gradlew assembleDebug`) when needed.
4. **Security Review**: Check for exposed API credentials, private stream tokens, or insecure HTTP endpoints.

---

## 2. Kotlin & Android Architecture Standards

- **Clean Architecture & Separation of Concerns**:
  - Keep UI/Leanback components decoupled from business logic and stream parsers.
  - State management must use `StateFlow` or `SharedFlow` with proper lifecycle awareness (`repeatOnLifecycle` or `flowWithLifecycle`).
- **Concurrency & Coroutines**:
  - Never execute blocking network requests or channel catalog parsing on `Dispatchers.Main`.
  - Always bind coroutines to appropriate scopes (`viewModelScope`, `lifecycleScope`) to avoid leaks.
- **Media & ExoPlayer Guidelines**:
  - Always release media players in `onStop()` / `onDestroy()` to prevent memory leaks and decoder stalls.
  - Implement robust error fallbacks for IPTV streams (Ostora, MovieBox, HLS/DASH) to prevent app ANR/crashes.
- **Immutability & Safety**:
  - Prefer `val` over `var`. Default to immutable collections (`List`, `Map`, `Set`).
  - Do not use force unwrap `!!`. Prefer `?.`, `?:`, `requireNotNull()`.

---

## 3. Mobile Environment Compatibility (CodeOnTheGo & AIDE)

- **Bytecode & Language Compatibility**:
  - Maintain compatibility with Java 8 / Java 11 bytecode levels so mobile builders (AIDE / D8 in Termux) can compile without complex desugaring errors.
- **Dependency Hygiene**:
  - Avoid adding unnecessary heavy third-party dependencies that break mobile Gradle caching or exceed Dex method limits.
- **Build Scripts**:
  - Respect existing configuration in `build.gradle.kts`, `settings.gradle.kts`, and `gradle.properties`.
  - Ensure resource IDs (`res/`) remain valid and compliant with Android AAPT/AAPT2.

---

## 4. Skills & Rules Quick Reference

The following workspace skills and rules are active in `.agents/`:
- **Skills**: `android-clean-architecture`, `kotlin-coroutines-flows`, `kotlin-patterns`, `kotlin-testing`, `security-review`, `verification-loop`.
- **Rules**: `.agents/rules/kotlin-*.md` and `common-*.md`.
