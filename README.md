# Calm Launcher

**A minimalist Android home screen replacement inspired by the Light Phone III — built to reduce digital noise and encourage intentional phone usage.**

[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.24-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Min SDK](https://img.shields.io/badge/Min%20SDK-26%20(Android%208.0)-3DDC84?logo=android&logoColor=white)](https://developer.android.com/)
[![Target SDK](https://img.shields.io/badge/Target%20SDK-34-3DDC84?logo=android&logoColor=white)](https://developer.android.com/)
[![License](https://img.shields.io/badge/License-Unspecified-lightgrey)](#license)

---

## Table of Contents

- [Overview](#overview)
- [Philosophy](#philosophy)
- [Features](#features)
- [Tech Stack](#tech-stack)
- [Architecture](#architecture)
- [Getting Started](#getting-started)
  - [Prerequisites](#prerequisites)
  - [Build & Run](#build--run)
  - [Required Permissions](#required-permissions)
- [Design System](#design-system)
- [Project Structure](#project-structure)
- [Troubleshooting](#troubleshooting)
- [Contributing](#contributing)
- [License](#license)

---

## Overview

**Calm Launcher** is a minimalist Android home screen replacement that inverts the traditional smartphone premise of capturing attention. Rather than competing for your focus with colorful icons, badges, and infinite scroll, it acts as a **"calm appliance"** — surfacing only the tools and actions you've deliberately chosen, while introducing gentle friction to distracting behaviors.

The interface follows a typography-first, strictly monochrome design language, giving the device an "E-Ink"-like presence on standard Android hardware.

## Philosophy

- 🖋️ **Typography First** — Relies on clear sans-serif type (a Helvetica Neue stack) instead of colorful app icons.
- ⚫ **Monochrome Palette** — Strictly limited to black, white, and grayscale to minimize visual stimulation.
- 🐢 **Intentional Friction** — Uses delays, confirmation prompts, and reflective questions before launching gated apps.
- 🎯 **Focus-First** — Ships with a dedicated Focus Mode and system-level accessibility services to enforce boundaries across the entire OS, not just within the launcher.

## Features

### Home Screen
A pure-black canvas that acts as the "resting state" of the device — an oversized clock and date block, a row of pinned favorite shortcuts, and a minimal status bar.

- **Swipe down** → opens Search
- **Swipe right** → opens Settings
- **Long-press the clock** → alternate shortcut to Settings

### App List & Management
- **App List** — a text-only, icon-free vertical list that avoids "logo-hunting" behavior, with an alphabet side-index for fast navigation.
- **Manage Apps** — hide apps from the main list, categorize them, or pin favorites to the Home screen.

### Search
A single, deliberate search field (bottom-border style, no card chrome). Unlike the App List, search results include hidden and social apps so they stay reachable via explicit intent — without being available for passive browsing.

### Focus Mode
A high-friction environment for deep work that suppresses standard navigation and requires a **3-second hold-to-confirm** gesture to exit, often paired with an E-Ink-style backdrop texture.

### Reflection & Nightly Journaling
A nightly prompt (triggered around 22:00) encourages digital mindfulness through short reflection questions. Responses are persisted and used to generate "Calm AI" insights shown on the Home screen.

### Reminders
Time-based nudges styled to match the launcher's monochrome aesthetic, scheduled through Android's `AlarmManager` and delivered via a dedicated notification manager.

### App Limits & Usage Enforcement
A proactive alternative to standard Digital Wellbeing tools:

- **Individual App Limits** — a time quota for a single app.
- **Group Limits** — a shared timer across related apps (e.g., a "Social" group where Instagram, TikTok, and X all draw from one budget).
- **Default Groups** — Social, Entertainment, Browser, and Information.
- **Approach Notifications** — alerts before a limit is actually reached.
- **Insight Metrics** — tracks blocked launches, estimated time saved, and the most-limited app.

### Screen Time & Analytics
A dashboard summarizing daily/weekly usage, backed by an analytics data pipeline that rolls up Android's `UsageStatsManager` data into the local database.

### Settings & Configuration
Granular control over environment modes, friction levels, and advanced/debug options, all persisted through a dedicated settings domain model.

## Tech Stack

| Layer | Technology |
|---|---|
| **Language** | Kotlin 1.9.24 |
| **UI Framework** | Jetpack Compose (Material 3) |
| **Dependency Injection** | Hilt |
| **Persistence** | Room (SQL) & DataStore (Preferences) |
| **Background Work** | WorkManager |
| **Architecture Pattern** | MVVM with Unidirectional Data Flow (UDF) |
| **Annotation Processing** | KSP (Room) & Kapt (Hilt) |
| **Build System** | Gradle 8.5.2 |

## Architecture

Calm Launcher follows an **MVVM + Unidirectional Data Flow** pattern with a clear separation between UI, domain, and data layers:

- **UI & Design System** — a custom `CalmTheme` built on `HelveticaNeue` typography and a monochrome palette, with shared components like `AppActionSheet` and `SettingRow` for consistency across screens.
- **Launcher & Navigation** — `HomeRoleManager` handles the `HOME` intent and manages the app's status as the system default launcher; screen navigation is handled via `androidx.navigation.compose`.
- **Data & Persistence** — the `CalmDatabase` (Room) stores app metadata, usage analytics, and limits, while a `SettingsDataStore` persists user preferences such as grayscale mode and friction levels.
- **Accessibility & Enforcement** — an `AccessibilityService` and `NotificationListenerService` enforce Focus Mode and app limits system-wide, beyond the launcher's own UI.

## Getting Started

### Prerequisites

| Requirement | Version |
|---|---|
| JDK | 17 |
| Kotlin | 1.9.24 |
| Android Gradle Plugin | 8.5.2 |
| Compile / Target SDK | 34 |
| Min SDK | 26 (Android 8.0) |
| Android Studio | Hedgehog or newer (recommended) |

### Build & Run

```bash
# 1. Clone the repository
git clone https://github.com/Prem-does/calm-laucher-.git
cd calm-laucher-

# 2. Build a debug APK from the terminal...
./gradlew assembleDebug

# ...or open the project in Android Studio and hit Run
```

The project uses **KSP** for Room and **Kapt** for Hilt annotation processing — these are wired up automatically on Gradle sync.

### Required Permissions

Because Calm Launcher enforces its "Focus" and "Minimalist" principles at the OS level, it requests several high-level system permissions. Most are requested at runtime; a few require manual setup for local testing.

| Permission / Role | Purpose |
|---|---|
| **Home Role** | Required to intercept the Home button and act as the default launcher |
| **Usage Access** | Required to calculate screen time and enforce app limits |
| **Accessibility Service** | Required to block apps by detecting window state changes |
| **Notification Access** | Required to suppress distracting notifications |
| **Device Admin** | Required for the screen-lock action from the launcher |

After installation, the launcher deep-links directly into the relevant Android system settings screens (Default Apps, Accessibility, Usage Access, Battery Optimization) to help you enable each of these.

## Design System

The UI is prototyped first in HTML/Tailwind CSS under `design/stitch/` (the "Stitch" prototypes), which act as the source of truth for layout, then implemented as Compose design tokens.

| Token Category | Examples | Purpose |
|---|---|---|
| **Colors** | `CalmWhite`, `CalmBlack`, `CalmGray`, `CalmDivider` | High-contrast monochrome UI |
| **Typography** | `headlineMd`, `labelMd`, `heroTime` | Scale from 8rem clock digits down to 12px labels |
| **Spacing** | `marginMobile`, `gutter`, `stackSm/Md/Lg` | Consistent padding and vertical rhythm |

**Theme modes:**

- **Dark** — `#000000` background / `#FFFFFF` foreground
- **Light** — `#F7F7F2` background / `#111111` foreground

Shared components such as `CalmStatusBar`, `CalmBackBar`, and `ThinDivider` keep the "flat," shadow-free aesthetic consistent across every screen.

## Project Structure

```
calm-laucher-/
├── app/
│   ├── src/main/java/com/calmlauncher/
│   │   ├── accessibility/       # FocusBlockAccessibilityService, notification listener
│   │   ├── core/designsystem/   # CalmTheme, shared Compose components
│   │   ├── data/                # Repositories, system actions, DB access
│   │   ├── domain/               # Models, repository interfaces, use cases
│   │   ├── feature/
│   │   │   ├── home/             # HomeScreen
│   │   │   ├── applist/          # AppListScreen
│   │   │   ├── search/           # SearchScreen
│   │   │   ├── focus/            # FocusScreen
│   │   │   ├── reflection/       # ReflectionScreen
│   │   │   ├── limits/           # AppLimitsScreen & ViewModel
│   │   │   └── settings/         # ManageAppsScreen, AdvancedSettings
│   │   └── launcher/             # HomeRoleManager
│   └── schemas/                  # Exported Room database schemas
├── design/stitch/                # HTML/Tailwind UI prototypes (source of truth)
├── build.gradle.kts
├── settings.gradle.kts
└── gradle.properties
```

## Troubleshooting

- **Namespace mismatch** — ensure `applicationId` and `namespace` in `app/build.gradle.kts` both remain `com.calmlauncher`.
- **Room schema errors** — Room exports schemas to `app/schemas/`; if the build fails on a schema mismatch, inspect the generated JSON there.
- **Hilt/Kapt injection failures** — verify `correctErrorTypes` is enabled in the `kapt` block of `app/build.gradle.kts`.

## Contributing

Contributions, issues, and feature requests are welcome. If you'd like to contribute:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/your-feature`)
3. Commit your changes
4. Open a pull request describing what you changed and why

Please try to keep new UI work aligned with the existing monochrome, typography-first design system.

## License

No license file was found in this repository at the time of writing. Please check with the repository owner ([Prem-does](https://github.com/Prem-does)) before reusing or redistributing this code, or add a `LICENSE` file to clarify usage terms.

---

