# Repository Guidelines

## Project Structure & Module Organization
- Root Gradle configuration lives in `settings.gradle.kts`, `build.gradle.kts`, and `gradle.properties`; keep multi-module config changes there.
- Application code resides in `app/src/main/java/com/example/weighttracker/`; group features by package (`ui/`, `data/`, `domain/`) as the codebase grows.
- XML resources belong under `app/src/main/res/` (`layout/` for screens, `values/` for strings, colors, themes, etc.).
- Unit tests go in `app/src/test/java/com/example/weighttracker/`; instrumentation tests belong in `app/src/androidTest/java/com/example/weighttracker/`.
- Add shared documentation in `docs/`, automation scripts in `scripts/`, and configuration samples in `config/` when needed.

## Product Planning
- App outline: see [docs/app-outline.md](docs/app-outline.md) for the current feature and UI plan.

## Build, Test, and Development Commands
- Open the project with Android Studio (Giraffe or newer) and let it sync dependencies automatically.
- Generate the Gradle wrapper once (`gradle wrapper` or Android Studio &gt; Sync); afterwards run `./gradlew assembleDebug` to produce a debug APK.
- `./gradlew testDebugUnitTest` executes JVM unit tests; use `./gradlew connectedDebugAndroidTest` for device/emulator runs.
- `./gradlew lint` keeps static analysis clean before submitting changes.

## Coding Style & Naming Conventions
- Follow the official Kotlin style guide (`ktlint`/Android Studio default formatting); keep lines ≤ 120 characters.
- Use `PascalCase` for Activities, Fragments, and other classes; package names remain lowercase `snake_case`.
- Prefix resource IDs with the component type (`text_weight_summary`) and store user-facing strings in `res/values/strings.xml`.
- Prefer immutable data, leverage AndroidX View Binding or Compose, and keep state holders (ViewModels/use-cases) separate from UI widgets.

## Testing Guidelines
- Every feature should ship with at least one unit test under `app/src/test/...` covering happy-path logic.
- Add instrumentation coverage in `app/src/androidTest/...` when behavior depends on Android components or persistence.
- Run `./gradlew testDebugUnitTest` locally before pushing; aim to keep combined unit coverage above 85%.

## Commit & Pull Request Guidelines
- Format commit subjects as `type(scope): imperative summary` (e.g., `feat(ui): add weekly goal chart`).
- Reference issues with `Resolves #id` in the body when applicable and list manual/automated tests executed.
- Include screenshots or screen recordings in PRs whenever UI changes are visible to users.
