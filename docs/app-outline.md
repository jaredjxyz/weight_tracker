# Weight Tracker App Outline

Build instructions live in `docs/build-guide.md`.

## Product Vision
- Deliver a modern, friendly Android experience that makes daily weight tracking effortless.
- Surface trends clearly via visuals so users can understand progress without manual analysis.
- Keep all weight data in the Android Connected Health ecosystem (Health Connect) so records are portable and remain under user control.

## Core Features
- **Visual Trend Dashboard**: Chart raw daily weights and a seven-day rolling average to highlight fluctuations vs. trend.
- **Weight Log & Entry**: Browse existing entries, add new weigh-ins, and edit or delete mistakes.
- **Health Connect Sync**: Read and write `WeightRecord` data, respecting permissions, time zones, and conflict resolution.
- **Material 3 Expressive UI**: Dynamic color, typography, and motion aligned with system settings (including dark mode).

## Screen Framework

### 1. Dashboard (Charts)
- **Hero charts**:
  - Primary line chart showing raw daily weight values across the selected date range (default 30 days).
  - Secondary overlay line depicting the seven-day rolling average.
- **Range controls**: Quick chips (7d, 30d, 90d, 1y, All) and date picker for custom spans.
- **Trend summary**: Delta vs. last week/month, streak indicators, and BMI snapshot if height is available.
- **Empty/error states**: Encouraging illustration when no data; inline permission request if Health Connect access missing.

### 2. Log & Entry
- **Entry list**: Reverse-chronological list grouped by week with relative timestamps and optional notes.
- **Quick actions**: Edit or delete from contextual actions; swipe gestures for speed.
- **New entry sheet**:
  - Weight input with unit selector (kg/lb) synced to Health Connect preferences.
  - Date/time picker defaulting to “now.”
  - Optional note field.
- **Validation**: Guard rails for improbable values, duplicates, or unsynced permissions.

## Navigation
- Bottom navigation with two destinations (`Dashboard`, `Log`) or a Material 3 `NavigationBar` driven by a single-activity Compose setup.
- Top app bar per screen with contextual actions (filters, export).

## Data & Platform Integration
- Use `androidx.health.connect.client` APIs:
  - Request `HealthPermission.WRITE_WEIGHT` and `HealthPermission.READ_WEIGHT`.
  - Persist new weights via `insertRecords(listOf(WeightRecord(...)))`.
  - Query history with `readRecords(WeightRecord::class, ReadRecordsRequest(...))`.
- Cache recent results locally (Room or in-memory) for quick rendering while syncing updates asynchronously.
- Handle permission rationale, revocation, and conflict resolution (e.g., duplicate timestamps).

## Architecture & Modules
- **UI Layer**: Jetpack Compose with Material 3 design system and `Charts` UI built via `Androidx.compose.ui.graphics` or MPAndroidChart wrapper module.
- **State Management**: MVVM with `ViewModel`, `StateFlow`, and `Immutable` UI state objects.
- **Domain Layer**: Use cases for fetching chart data, computing rolling averages, unit conversions, and writing records.
- **Data Layer**: Repository coordinating Health Connect access, caching, and analytics.
- Future modularization: `core/design`, `core/data`, `feature/dashboard`, `feature/log`.

## Theming & Styling
- Adopt Material 3 Expressive guidance: high-contrast palettes, custom typography scales, and rich motion.
- Respect system dynamic color via `dynamicLightColorScheme`/`dynamicDarkColorScheme` fallback to branded palette.
- Leverage Material 3 components (navigation bars, cards, elevated buttons) and MDC 3 motion specs for transitions.

## Accessibility & Quality
- Ensure charts expose content descriptions and alternative summaries.
- Support larger font sizes and TalkBack via semantic nodes.
- Localization-ready strings; store all copy in `res/values/strings.xml`.
- Provide unit tests for rolling-average calculations and integration tests around Health Connect read/write flows.

## Future Enhancements
- Goal tracking (target weight, milestone alerts).
- Wear OS companion for quick entries.
- Data export/import (CSV, Google Drive).
- Widgets and notifications for reminders.
