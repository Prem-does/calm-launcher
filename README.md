# Calm Launcher

A minimalist Android launcher inspired by the UI philosophy of the Light Phone III. Built to reduce distraction, make the phone feel calmer, monochrome, and more intentional while preserving essential utility.


## Project Overview

Calm Launcher is a purpose-built Android home screen replacement for users who want less friction and less digital noise. It is not designed to be a productivity launcher; it is designed to be a focus-first environment that makes using your phone feel deliberate rather than reactive.

Core goals:

- Reduce visual clutter through typography-first design
- Keep the interface strictly monochrome and highly legible
- Present only the apps and actions that matter
- Encourage intentional use through soft friction, confirmations, and delayed access
- Support modern Android launcher integrations while keeping build complexity low


## Why Calm Launcher Exists

Smartphones are built to capture attention. Calm Launcher inverts that premise: it puts a calm, restrained shell over Android and only surfaces the things the user has chosen to keep.

Key value propositions:

- A home screen that behaves like a calm appliance, not an attention machine
- No endless grid of icons, no colorful badges, no autoplay motion
- A focused mode that blocks triggers and keeps the user in the present
- Compatibility with Android 14 launch capabilities and modern API levels


## Vision Statement

> Calm Launcher is for people who want to live with their phone instead of being lived by it.

It asks:

- What if the launcher had fewer apps and more breathing room?
- What if daily phone use began with a neutral, sans-serif interface instead of a noisy feed?
- What if the device felt gentle and intentional from the moment the user woke the screen?


## Design Principles

1. Typography first; icons only when essential
2. Monochrome palette only: black, white, grayscale
3. Minimal choice architecture: fewer options, clearer intent
4. Soft friction for distractions: confirmations, delays, and block modes
5. Predictable, accessible navigation with simple gestures
6. Calm motion only: gentle fades and subtle state changes
7. Clear separation between utility and distraction


## Product Pillars

- Minimal visual surface
- Explicit app allowlist / denylist
- Meaningful focus state
- Lightweight and battery-friendly architecture
- Android-native tooling and standard launcher behavior


## What This Launcher Is

- A true home replacement app
- A typography-driven interface
- A launcher that places app access under user control
- A project built with Jetpack Compose, Hilt, Room, and WorkManager


## What This Launcher Is Not

- Not a widget container
- Not a notification feed
- Not an app store or discovery surface
- Not a colorful or animated experience by design
- Not a replacement for core OS-level parental controls


## Current Project Status

The repository contains an Android launcher application configured for:

- Kotlin 1.9.24
- Gradle plugin 8.5.2
- Compile API level 34
- Target API level 34
- Minimum supported API level 26
- Jetpack Compose UI
- Hilt dependency injection
- Room persistence
- WorkManager for background tasks


## Architecture Summary

The app is split into well-defined layers:

- `ui`: Compose screens and reusable UI components
- `viewmodel`: Android ViewModels and state handling
- `domain`: business rules, use cases, and app filtering logic
- `data`: persistence, repositories, and app/system data sources
- `accessibility`: optional services and greyscale integration
- `navigation`: route definitions and navigation host setup
- `utils`: shared helpers and system interaction helpers


## Technical Stack

- Kotlin
- Android Gradle Plugin 8.5.2
- Jetpack Compose
- Hilt
- Room
- WorkManager
- DataStore preferences
- AndroidX Navigation Compose
- Material 3 design system


## Key Features

### Launcher Behavior

- Acts as a default home app when granted
- Supports the Android launcher role and home intent flow
- Loads using a monochrome, text-first interface
- Reduces accidental taps by relying on spaced rows and full-width buttons

### Home Screen

The home screen is designed to show only essential device status:

- Time and date
- Battery level
- Cellular signal state
- Wi-Fi connectivity state
- Optional compact weather information

Rules enforced in design:

- No custom wallpapers except simple solid fills
- No classic icon grid
- No widgets by default
- Soft fade transitions only
- No colorful backgrounds or badges


### App Access

The launcher intentionally surfaces a small set of tools by default. This is achieved through app allowlisting and filtering logic.

Built-in utility access patterns may include:

- Phone
- Messages
- Alarm
- Calculator
- Calendar
- Camera
- Maps
- Notes
- Music
- Settings

Additional installed apps are hidden unless the user explicitly chooses to expose them.


### Anti-Distraction Controls

- Optional delay before launching selected apps
- Confirmation prompts for apps marked as distracting
- Screen-time counters and usage tracking
- Pin-protected settings and exclusions
- Hard blocking through focus mode
- Greyscale / low-saturation presentation


### Focus Mode

Focus mode is a primary feature that reduces the phone to a minimal state.

Focus mode capabilities:

- Block selected apps entirely
- Leave only emergency access or essential tools active
- Show calming text, quotes, or a blank interface
- Provide a timer for intentional periods of calm


### Navigation Model

The launcher uses a gesture- and list-based navigation model:

- Vertical scroll navigation in core screens
- Swipe down to reveal search and quick actions
- Horizontal swipes to access secondary tools or modes
- Long press on the home area for focus mode activation


### Accessibility

Accessibility is a core consideration:

- Large text and generous spacing
- High contrast monochrome design
- Support for screen readers through content descriptions
- Accessibility services can enforce greyscale and block patterns


## Build Configuration

The application module (`app/build.gradle.kts`) is configured with:

- `namespace = "com.calmlauncher"`
- `compileSdk = 34`
- `minSdk = 26`
- `targetSdk = 34`
- Kotlin JVM target 17
- Compose compiler extension 1.5.14
- Support for `buildConfig`

Build features enabled:

- Compose UI
- Build config generation

Packaging exclusions:

- `/META-INF/{AL2.0,LGPL2.1}`


## Dependencies at a Glance

- `androidx.core:core-ktx:1.13.1`
- `androidx.activity:activity-compose:1.9.0`
- Compose BOM `androidx.compose:compose-bom:2024.06.00`
- `androidx.compose.ui:ui`, `ui-graphics`, `ui-tooling-preview`
- `androidx.compose.material3:material3`
- `androidx.compose.material:material-icons-extended`
- `androidx.lifecycle:lifecycle-runtime-ktx:2.8.2`
- `androidx.lifecycle:lifecycle-runtime-compose:2.8.2`
- `androidx.lifecycle:lifecycle-viewmodel-compose:2.8.2`
- `androidx.navigation:navigation-compose:2.8.0`
- `androidx.datastore:datastore-preferences:1.1.1`
- `androidx.room:room-runtime:2.6.1`
- `androidx.room:room-ktx:2.6.1`
- `androidx.hilt:hilt-navigation-compose:1.2.0`
- `androidx.hilt:hilt-work:1.2.0`
- `androidx.work:work-runtime-ktx:2.9.0`
- `org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1`
- Hilt runtime and compiler dependencies
- Test dependencies for JUnit and Compose UI testing


## Build and Run Instructions

### Prerequisites

- Java 17 JDK
- Android SDK with API level 34
- Gradle wrapper included in repository
- Android device or emulator targeting API 26+

### Local Build

From the repository root, run:

```powershell
.\gradlew.bat assembleDebug
```

This produces a debug APK in:

```text
app\build\outputs\apk\debug\
```

### Install on device

Use ADB:

```powershell
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

If the device already has a different launcher installed, select Calm Launcher when prompted or manually set it as default through system settings.


## Release Configuration

The release build is currently configured as:

- `minifyEnabled = false`
- ProGuard files: `proguard-android-optimize.txt` and `proguard-rules.pro`

To prepare a release build, update the signing configuration and consider enabling code shrinking once stable.


## Code Organization

### UI Layer

The UI layer should contain:

- Compose themes and typography definitions
- Reusable low-level components like buttons, rows, and dividers
- Screen-level composables for each launcher section
- Navigation host and route definitions
- Animation and transition helpers

Good candidates:

- `HomeScreen.kt`
- `AppListScreen.kt`
- `SearchScreen.kt`
- `FocusModeScreen.kt`
- `SettingsScreen.kt`


### ViewModel Layer

The viewmodels manage state and side effects:

- `HomeViewModel.kt`
- `AppListViewModel.kt`
- `FocusModeViewModel.kt`
- `SettingsViewModel.kt`

They should expose immutable UI state, handle event dispatching, and coordinate use case execution.


### Domain Layer

The domain layer should implement core business rules and use cases such as:

- App filtering and allowlist/denylist rules
- Delay and confirmation logic for app launches
- Calculating home screen content state
- Deciding focus mode transitions
- Handling daily usage and screen-time tracking

Potential use cases:

- `FilterAppsUseCase`
- `ShouldDelayOpenUseCase`
- `ShouldShowConfirmationUseCase`
- `BuildHomeStateUseCase`


### Data Layer

The data layer should include:

- Room database definitions
- DAOs for app records, settings, and screen time
- Repositories that expose data to domain and UI layers
- System data sources for package info, battery, and connectivity

Potential files:

- `CalmDatabase.kt`
- `AppDao.kt`, `SettingsDao.kt`, `ScreenTimeDao.kt`
- `AppRepository.kt`, `SettingsRepository.kt`, `ScreenTimeRepository.kt`
- `LauncherSystemObserver.kt`
- `UsageStatsTracker.kt`
- `BatteryObserver.kt`
- `NetworkObserver.kt`
- `WeatherProvider.kt`


### Accessibility Layer

Accessibility pieces may include services and helpers for:

- Greyscale enforcement
- Focus blocking overlays
- Screen reader labels and accessibility semantics
- High contrast display and text scaling support

Suggested files:

- `GreyscaleAccessibilityService.kt`
- `FocusBlockService.kt`


### Navigation Layer

A dedicated navigation layer should hold route definitions and the navigation host.

Suggested files:

- `Routes.kt`
- `LauncherNavHost.kt`


### Utilities

Shared utilities support time formatting, intent construction, theme constants, and system interactions.

Suggested files:

- `TimeFormatter.kt`
- `IntentBuilders.kt`
- `MonochromePalette.kt`


## Launcher Integration

The launcher module must be configured to handle Android home and app drawer intents. Key integration points:

- `android.intent.action.MAIN`
- `android.intent.category.HOME`
- `android.intent.category.DEFAULT`

The launcher should also request and manage the default home role using the `RoleManager` API when available.


## Permission and Role Requirements

The launcher may need handling for the following system permissions or roles:

- Home role / default launcher role
- Notification access (optional, not required by core launcher)
- Accessibility service permission if greyscale or blocking services are enabled

The project intentionally avoids requiring broad runtime permissions for core operation.


## App Filtering and Allowlist Behavior

The app should expose a deterministic filtering pipeline:

1. Query installed launchable packages from `PackageManager`
2. Filter out system apps that are not useful for the calm experience
3. Apply user allowlist / denylist rules
4. Mark apps for distraction delay or confirmation if configured
5. Surface only the filtered set in the launcher UI

This ensures the user sees a consistent set of apps and that hidden apps remain hidden until explicitly allowed.


## Focus Mode Implementation

Focus mode should be a global application state that can be toggled from the launcher.

Expected behavior:

- When enabled, block launch attempts for selected distracting apps
- When enabled, optionally hide non-essential sections of the UI
- When enabled, display a calm state screen with minimal information
- When disabled, restore normal launcher access

Consider using a persistent setting and `WorkManager` or in-memory state to keep the mode active across restarts.


## Screen Time and Usage Tracking

To support anti-distraction rules, the launcher should record usage events:

- Timestamp of app launches
- Duration of session states
- Daily counts and totals
- Focus mode time

Storage may use Room entities and DAOs. The data should be immutable and queryable by day.


## App Launch Delay / Confirmation Flow

Apps identified as potentially distracting may follow a soft friction flow:

- When tapped, display a delay overlay for 2–5 seconds
- After the delay, show a confirmation card with the app name and reason
- Allow the user to cancel or proceed explicitly

This flow is designed to interrupt automatic impulse launches without removing the app entirely.


## UI Design Language

### Typography

The UI relies on large, readable text and a neutral type scale:

- Display text for headings
- Title text for actionable items
- Body text for status and labels
- Caption text for small hints

### Spacing

- Generous vertical spacing between rows
- Clear separation between status blocks
- Soft padding around buttons and controls

### Color Usage

The palette is restricted to:

- Pure black
- Pure white
- Multiple grayscale stops for hierarchy
- Single accent tone only if absolutely needed

### Motion

The motion system should be subtle:

- Soft crossfades for screen changes
- Slow fade-ins for dialogs and prompts
- No bouncing or heavy physics


## Accessibility and Large Text

A calm UI must also be easy to read for a wide range of users.

The project should support:

- Dynamic type scaling through Compose
- Content descriptions for interactive elements
- Clear focus order and touch targets
- High contrast mode semantics
- Compatibility with TalkBack and similar services


## Testing Strategy

### Unit Tests

- Use JUnit 4 for domain and repository tests
- Validate filtering logic and allowlist rules
- Test focus mode state transitions
- Test time formatting utility functions

### Compose UI Tests

- Use `androidx.compose.ui:ui-test-junit4`
- Verify critical screens render with the expected state
- Test navigation between home and app list
- Validate the delay/confirmation UI flows

### Instrumented Tests

- Use `androidx.test.ext:junit:1.2.1`
- Test default home selection and full app launch flow
- Use emulator or physical device to validate system integration


## Developer Workflow

### Clone the repository

```powershell
git clone <repository-url>
cd "MY OWN OS"
```

### Start development

```powershell
.\gradlew.bat clean assembleDebug
```

### Run linting and static analysis

```powershell
.\gradlew.bat lint
```

### Run unit tests

```powershell
.\gradlew.bat test
```

### Run instrumented tests

```powershell
.\gradlew.bat connectedAndroidTest
```


## Configuration Notes

### Gradle Properties

Project-level configuration lives in `gradle.properties` and `settings.gradle.kts`.

This project uses the Gradle wrapper for consistency.

### Java Compatibility

- Source compatibility: Java 17
- Target compatibility: Java 17
- Kotlin JVM target: `17`

### Packaging

To avoid unnecessary metadata collisions, the packaging section excludes multiple META-INF licenses from the final APK.


## Recommended Development Tools

- Android Studio Electric Eel or newer
- Kotlin plugin matching Kotlin 1.9.24
- Android SDK platforms 34 and 33
- An Android device or emulator with API 26+
- ADB for install and debugging


## Debugging Tips

### Common build command

```powershell
.\gradlew.bat assembleDebug
```

### Install the debug APK

```powershell
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

### View log output

```powershell
adb logcat | Select-String "com.calmlauncher"
```

### Clear app data

```powershell
adb shell pm clear com.calmlauncher
```


## Recommended Architecture Practices

### Single responsibility

Keep each file and class focused on a single responsibility:

- UI composables render a UI state
- ViewModels expose state and handle events
- Use cases encapsulate business rules
- Repositories abstract data sources

### Testable design

Design the domain layer so it can be tested without Android frameworks. Keep logic pure where possible.

### Dependency injection

Use Hilt to wire core services. Keep module bindings explicit and avoid global singletons outside Hilt scope.

### Compose best practices

- Prefer state hoisting
- Keep composables small and descriptive
- Avoid side effects inside composables except through effect handlers
- Use `remember` and `LaunchedEffect` carefully


## Project Structure and File Responsibilities

### `MainActivity.kt`

This is the host activity for the launcher. It should:

- Set the Compose content root
- Register the launcher navigation host
- Observe default home / role state
- Request the home role when appropriate

### `LauncherActivity.kt`

This activity handles launcher-specific intents and may coordinate the default home prompt.

### `DefaultHomePrompt.kt`

Contains logic and UI for prompting the user to make Calm Launcher the default home app.

### `HomeRoleManager.kt`

Manages the Android role API and default launcher prompt flow.

### UI Theme Files

- `Color.kt`: grayscale palette values and color constants
- `Type.kt`: typography scale definitions
- `Shape.kt`: shape styling and corner radius values
- `Theme.kt`: Material 3 theme implementation and dark/light mode control

### Low-Level Components

- `TextRow.kt`: reusable row item for text-based app entries
- `ThinDivider.kt`: divider component with subtle styling
- `CalmButton.kt`: button styling consistent with the calm design
- `ConfirmationSheet.kt`: overlay sheet for confirmation flows
- `DelayOverlay.kt`: full-screen delay overlay for app launches

### Screens

- `HomeScreen.kt`: primary landing screen with status info and quick access
- `AppListScreen.kt`: filtered app list and allowlist management
- `SearchScreen.kt`: search interface for installed apps and settings
- `FocusModeScreen.kt`: calm mode UI and timer
- `SettingsScreen.kt`: all user-adjustable options and guardrails

### ViewModels

ViewModels should be scoped to navigation destinations and expose state with `StateFlow` or `LiveData`.

- `HomeViewModel.kt`: home screen state, clock updates, battery and network status
- `AppListViewModel.kt`: filtering, search, and allowlist logic
- `FocusModeViewModel.kt`: focus state toggles and block logic
- `SettingsViewModel.kt`: preferences and customization values

### Domain Models

- `AppEntry.kt`: representation of an app item with metadata and access state
- `FocusModePolicy.kt`: focus mode configuration and block lists
- `ScreenTimeRecord.kt`: usage tracking record for a single day or session

### Use Cases

- `FilterAppsUseCase.kt`: central filtering pipeline for launcher content
- `ShouldDelayOpenUseCase.kt`: determines whether an app should show a delay
- `ShouldShowConfirmationUseCase.kt`: determines whether to show a confirmation prompt
- `BuildHomeStateUseCase.kt`: composes the home screen state from system and app sources

### Data Entities

- `AppEntity.kt`: persisted app metadata and user preferences
- `SettingsEntity.kt`: persisted user preferences and mode settings
- `ScreenTimeEntity.kt`: persisted usage records and session data

### Repositories

- `AppRepository.kt`: app metadata and allowlist persistence
- `SettingsRepository.kt`: settings storage and read/write access
- `ScreenTimeRepository.kt`: usage event storage and analytics

### System Observers

- `LauncherSystemObserver.kt`: monitors package changes, app installs, and default home state
- `UsageStatsTracker.kt`: records usage events and screen time
- `BatteryObserver.kt`: observes battery and charging state
- `NetworkObserver.kt`: observes connectivity state
- `WeatherProvider.kt`: optional compact weather data provider

### Accessibility Services

- `GreyscaleAccessibilityService.kt`: optional service for enforcing a constrained display palette
- `FocusBlockService.kt`: optional service for blocking apps and enforcing focus mode

### Navigation

- `Routes.kt`: route constants and screen identifiers
- `LauncherNavHost.kt`: Compose navigation host configuration

### Utility Files

- `TimeFormatter.kt`: date/time formatting helpers
- `IntentBuilders.kt`: helper methods for building app launch intents
- `MonochromePalette.kt`: grayscale palette helpers and contrast utilities


## Example Usage Scenarios

### Getting Started

1. Install Calm Launcher.
2. Set it as the default home app.
3. Configure the allowlist with only essential apps.
4. Enable focus mode when you want a calm screen.

### Reducing Distraction

- Mark social apps as distracting
- Enable delay/confirmation for those apps
- Use focus mode during work sessions
- Periodically review the allowlist to keep it small

### Maintaining a Calm Home

- Use the home screen for status only
- Resist the urge to customize with icons or widgets
- Keep the visual palette monochrome and stable


## Common Scenarios and Handling

### When the user installs a new app

- The launcher should detect the new package via `PackageManager`
- The app is initially hidden unless the user explicitly exposes it
- Users can choose to add the app to the allowlist manually

### When the user opens a distracting app

- If marked as distracting, the launcher may show a delay overlay
- After delay, the user sees an explicit confirmation dialog
- If the user cancels, the app does not start

### When the device is low on battery

- The home screen should surface battery status clearly
- Focus mode can be used to reduce screen-on time
- The app should avoid battery-intensive background tasks

### When the launcher is not the default home

- Prompt the user to adopt Calm Launcher as the default launcher
- Provide a friendly explanation of the calm experience
- Fall back gracefully if the user declines until next launch


## Implementation Notes

### App Loading Pipeline

1. Query launchable activities via `PackageManager.queryIntentActivities`
2. Filter by category and visibility rules
3. Sort by allowlist status and usage preference
4. Cache filtered app metadata in Room for fast startup
5. Present the filtered list in Compose screens


### Persistence Strategy

- Use Room for structured persistent state
- Use DataStore for settings that are simple key-value pairs
- Use on-disk caching for system metadata to reduce load overhead
- Keep persistent state minimal and user-centric


### Background Work

Small background tasks can use WorkManager for:

- Synchronizing usage stats
- Maintaining daily summaries
- Cleaning up old records
- Updating optional weather data

Prefer `WorkManager` with `ExistingWorkPolicy.KEEP` to avoid redundant work.


## Release and Distribution

### Release build steps

1. Configure signing keys in `build.gradle.kts` or `gradle.properties`
2. Set `isMinifyEnabled = true` when ready
3. Build release APK with:

```powershell
.\gradlew.bat assembleRelease
```

4. Test the release build on physical devices
5. Upload to distribution channel or share internally


## Contribution Guidelines

### How to contribute

- Fork the repository
- Create a feature branch
- Implement changes with clear intent
- Add tests for business logic and critical flows
- Submit a pull request with a detailed description

### Code style

- Use idiomatic Kotlin
- Keep Compose UIs readable and small
- Prefer immutable state flows in ViewModels
- Keep functions short and expressive
- Avoid large monolithic classes

### Commit messages

- Use present tense and short descriptions
- Mention the purpose and scope of the change
- Example: `Add focus mode confirmation overlay`


## Troubleshooting

### Build fails with missing SDK

Ensure Android SDK platform 34 is installed and `ANDROID_HOME` / `ANDROID_SDK_ROOT` are configured correctly.

### App does not appear as launcher

- Confirm the app is installed
- Open device settings and set Calm Launcher as the default home app
- Reboot if necessary
- Ensure the launcher intent filter is configured in `AndroidManifest.xml`

### UI layout issues on small screens

- Verify Compose `Modifier` usage and responsive layout constraints
- Use `weight`, `fillMaxSize`, and `imePadding` appropriately
- Test on devices as small as API 26 screens

### Delay or confirmation overlay never showing

- Confirm the app filtering and distraction policy logic
- Check the ViewModel state and whether `ShouldDelayOpenUseCase` returns true
- Validate the overlay composable is properly tied to the launch event


## Known Design Tradeoffs

### Minimalism vs. discoverability

The design intentionally sacrifices discoverability for calmness. This means onboarding must explain the experience clearly so users understand why apps are hidden by default.

### App visibility vs. system integration

Hiding apps in the launcher does not prevent them from receiving notifications or being launched through other system flows. Calm Launcher focuses on the home surface, not OS-level app restrictions.

### Accessibility vs. aesthetic restraint

The interface is monochrome and restrained, but accessibility should always take precedence. Where necessary, add support for high-contrast modes and larger touch targets without compromising the calm aesthetic.


## Roadmap

### Short-term work

- Complete app allowlist / denylist settings UI
- Implement focus mode timer and status
- Add persistent screen-time analytics
- Introduce app launch delay and confirmation flows
- Improve default home role prompt experience

### Mid-term work

- Add optional greyscale enforcement via accessibility service
- Add quick settings shortcuts for focus mode
- Implement a minimal search interface with system app lookup
- Add onboarding screens for first-time users
- Add support for user-curated shortcuts or folders

### Long-term work

- Add optional kiosk mode for deep focus sessions
- Add remote or companion device support for locking
- Integrate with wearable or physical button triggers
- Add advanced usage analytics and daily summary reports


## FAQ

### Q: Does Calm Launcher block notifications?

A: Not by default. It focuses on the home experience and app launch surface. Notifications are still handled by the system unless additional accessibility or helper services are built to manage them.

### Q: Will this work on older Android phones?

A: The minimum supported API level is 26, so the launcher should run on devices running Android 8.0 and above. Modern Android launcher role APIs are supported on Android 10+ but fallback behavior is provided for older versions.

### Q: Can I show only one app on the home screen?

A: Yes. The underlying app filtering model supports a small allowlist. The launcher intentionally makes the list small to maintain calmness.

### Q: Is there a way to hide the status bar?

A: The status bar is optional. The launcher can support a minimal or hidden status bar mode for users who prefer deeper immersion.


## Security and Privacy

- No analytics or analytics frameworks are included by default
- Usage tracking is designed for local, device-only data
- Focus and distraction policies are stored locally in Room and DataStore
- The launcher does not transmit user data without explicit user consent


## Appendix: Sample Project Tree

```text
.
├── README.md
├── app
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   ├── src
│   │   └── main
│   │       ├── AndroidManifest.xml
│   │       ├── java
│   │       │   └── com
│   │       │       └── calmlauncher
│   │       │           ├── MainActivity.kt
│   │       │           ├── launcher
│   │       │           ├── ui
│   │       │           ├── viewmodel
│   │       │           ├── domain
│   │       │           ├── data
│   │       │           ├── accessibility
│   │       │           ├── navigation
│   │       │           └── utils
│   │       └── res
│   │           ├── drawable
│   │           ├── layout
│   │           └── values
├── build.gradle.kts
├── gradle.properties
├── gradlew
├── gradlew.bat
├── settings.gradle.kts
└── other project files
```


## Appendix: Debug APK Location

After a successful debug build, the generated debug APK can be found at:

```text
app\build\outputs\apk\debug\app-debug.apk
```


## Appendix: Useful Gradle Commands

- `.
  \gradlew.bat assembleDebug` — Build debug APK
- `.
  \gradlew.bat assembleRelease` — Build release APK
- `.
  \gradlew.bat clean` — Remove build outputs
- `.
  \gradlew.bat test` — Run unit tests
- `.
  \gradlew.bat connectedAndroidTest` — Run instrumentation tests
- `.
  \gradlew.bat lint` — Run lint checks


## Appendix: Recommended Commit Workflow

1. Pull the latest main branch
2. Create a descriptive feature branch
3. Implement the feature with tests
4. Run local build and test passes
5. Add a focused commit message
6. Open a pull request with screenshots and rationale


## Attribution

Calm Launcher is inspired by the design restraint of Light Phone, Nothing, and minimal e-ink experiences. The goal is to offer a calm, restrained home experience on Android without sacrificing essential utility.


## Closing Notes

This repository is intended to be a living implementation of a calm launcher concept. Use this README as the central source of truth for how the app works, how it should be built, and how future contributors can extend it safely.

If you want to expand the concept, focus on preserving the core experience: monochrome clarity, fewer choices, and a soft but intentional interaction model.

A text-only vertical list of allowed apps.

### Search Screen

A restrained search surface invoked by swipe down.

### Focus Mode Screen

A blank or quote-based shield screen that blocks distracting apps.

### Settings Screen

PIN-protected settings for whitelist control, delays, greyscale enforcement, and kiosk options.

### Tool Screens

Dedicated text-based screens for Phone, Messages, Alarm, Calculator, Calendar, Camera, Maps, Notes, Music, and Settings.

## State Management

Recommended state pattern:

- StateFlow for UI state
- Immutable screen models
- One-way data flow
- Repository-backed persistence
- Small, testable use cases for app filtering and lock policies

Example state categories:

- Home state
- App visibility state
- Focus mode state
- Lock / PIN state
- Screen-time state
- Accessibility and restriction state

## UI Behavior Rules

- App launch surfaces must be text-only
- No icon badges or notification bubbles
- No infinite scrolling surfaces
- No recommendation feeds
- No colorful media previews
- No distracting motion on screen changes
- Preserve immediate access to emergency functions

## Sample Launch Intent Logic

The launcher should intercept home presses, expose only allowed apps by default, and route all launches through policy checks.

Pseudo-flow:

1. User taps an app label
2. Launcher checks app category and lock policy
3. If distracting, show confirmation or delay overlay
4. If allowed, launch the app
5. Record screen-time and session metadata locally

## Minimal Theme Direction

The theme should emulate e-ink and Swiss design:

- Background: black or white
- Text: grayscale only
- Emphasis: size, spacing, and hierarchy, not color
- Dividers: thin and low-contrast
- Corners: subtle rounding only
- Shadows: none or nearly none

## Implementation Milestones

1. Scaffold the project and launcher manifest
2. Build the monochrome theme system
3. Implement the home screen
4. Add allowed-app filtering and text-only app list
5. Add search and gesture navigation
6. Add focus mode and restrictions
7. Add PIN-protected settings and screen-time tracking
8. Harden default-home, kiosk, and accessibility behavior
9. Polish typography, spacing, and performance

## Non-Goals

- Social feeds
- App store replacement
- Cloud sync as a requirement
- Rich personalization that encourages tinkering
- Visual complexity
- Anything that reintroduces attention traps

## Deliverables To Build Next

- Full launcher architecture
- Flutter or Compose implementation files
- Home screen sample code
- Navigation system
- Theme system
- App filtering logic
- Focus mode implementation
- Local persistence layer
- Accessibility support

## License

## Troubleshooting

- **Symptom:** Occasionally the app shows a fully black screen instead of the Home screen.

- **Quick reproduction steps:**
  - Run the app on a device or emulator with USB debugging enabled.
  - Trigger the launcher as the home app with:

    adb shell am start -W -a android.intent.action.MAIN -c android.intent.category.HOME

- **Collect logs while reproducing:**
  - Clear previous logs: `adb logcat -c`
  - Start a fresh capture: `adb logcat > launcher-log.txt` and reproduce the issue, then stop capture.
  - Inspect `launcher-log.txt` for `FATAL EXCEPTION`, `IllegalStateException`, Compose/Hilt initialization errors, or frequent process restarts.

- **Common causes & checks:**
  - Lifecycle: confirm `MainActivity`/launcher activity `onCreate()` calls `setContent` and Compose content is emitted.
  - Theme / colors: verify background and text colors are not identical; try switching background to white to rule out color-only issues.
  - Process crashes: check for rapid process restarts in `adb logcat` or `adb shell ps` output.
  - Focus/kiosk mode: ensure a blank focus screen or kiosk setting isn't being enabled unexpectedly.
  - Heavy initialization on startup: long blocking work in `onCreate` can prevent UI from drawing—move to background threads.

- **Debugging tips:**
  - Run in debug and set breakpoints in `MainActivity.kt`, `HomeScreen.kt`, and `SettingsScreen.kt` to confirm execution reaches the UI composition.
  - If logs show Skia/renderer errors, try disabling hardware acceleration for the activity in the manifest as a test.
  - Capture a screenshot (or photo of the device) and attach `launcher-log.txt` when reporting the issue.

- **Next steps to help us investigate:**
  - Reproduce the issue and attach `launcher-log.txt` plus a screenshot.

## License

To be decided.
