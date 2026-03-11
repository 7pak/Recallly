# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Recallly is a B2B, offline-first, AI-powered native Android app for field professionals (Sales Reps, Field Engineers, Insurance Adjusters). It eliminates manual CRM data entry using on-device AI and system integrations.

- **Package**: `com.at.recallly`
- **Min SDK**: 28 (Android 9) / **Target SDK**: 36 / **Compile SDK**: 36
- **JVM Target**: Java 11
- **Compose BOM**: 2026.02.01
- **AGP**: 8.13.2 / **Gradle**: 8.13 / **Kotlin**: 2.3.10

## Build Commands

```bash
./gradlew build              # Full build
./gradlew assembleDebug      # Build debug APK
./gradlew assembleRelease    # Build release APK
./gradlew test               # Run unit tests
./gradlew connectedAndroidTest  # Run instrumented tests (requires device/emulator)
./gradlew :app:testDebugUnitTest --tests "com.at.recallly.ExampleUnitTest"  # Single test class
./gradlew clean              # Clean build artifacts
```

On Windows, use `gradlew.bat` instead of `./gradlew`.

## Architecture

**Clean Architecture + MVVM** with Unidirectional Data Flow (UDF). Single `:app` module.

### Layer Structure (under `com.at.recallly`)

- **`presentation/`** — UI layer (Compose screens, ViewModels, Navigation)
  - ViewModels expose exactly ONE `StateFlow<UiState>` and accept `UiEvent` sealed interfaces
  - No business logic in ViewModels
  - Feature screens organized by feature (e.g., `auth/` has LoginScreen, SignUpScreen, AuthViewModel, AuthUiState, AuthUiEvent)
- **`domain/`** — Business logic (pure Kotlin models, repository interfaces, UseCases)
  - No Android dependencies allowed here
  - UseCases use `operator fun invoke()` pattern
- **`data/`** — Data layer (Room DAOs, DataStore, repository implementations, API clients)
- **`core/`** — Shared infrastructure
  - `di/` — Koin dependency injection modules
  - `result/` — `Result<T>` sealed class (Success/Error) for error handling
  - `theme/` — Material3 Color, Theme, Typography
  - `util/` — DispatcherProvider, Constants, Extensions, RecalllyException

### Key Files

- **Entry point**: `MainActivity.kt` → `RecalllyNavGraph` → screens
- **Application**: `RecalllyApplication.kt` (Koin + Timber initialization)
- **DI**: `core/di/AppModule.kt` (Koin module definitions)
- **Navigation**: `presentation/navigation/RecalllyNavGraph.kt` (type-safe routes via `@Serializable` sealed classes in `Route.kt`)
- **Theme**: `core/theme/` (Color, Theme, Type)
- **Database**: `data/local/db/RecalllyDatabase.kt` (Room, schema exports to `app/schemas/`)
- **Preferences**: `data/local/datastore/PreferencesManager.kt`

## Dependency Management

Dependencies managed via version catalog at `gradle/libs.versions.toml`. Add new dependencies there, not in `build.gradle.kts`.

### Key Dependencies

| Category | Library |
|----------|---------|
| DI | Koin |
| DB | Room (KSP for annotation processing) |
| Prefs | DataStore Preferences |
| Nav | Navigation Compose |
| Auth | Firebase Auth + Credential Manager |
| Calendar | Google Calendar API |
| Billing | Google Play Billing |
| Background | WorkManager |
| Logging | Timber |
| Serialization | Kotlinx Serialization JSON |

## Architectural Rules

- 100% Kotlin. No Java.
- Kotlin Coroutines & Flow/StateFlow for concurrency.
- Domain layer must have zero Android dependencies.
- Repositories implement domain interfaces in the data layer.
- Strictly offline-first — no cloud database.
- KSP + Room compiler are commented out — KSP 2.3+ requires AGP 9.0+ which Android Studio doesn't yet support. Uncomment when upgrading to AGP 9.
