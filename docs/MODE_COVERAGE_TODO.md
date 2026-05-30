# Calm Launcher — Mode Coverage

Tracks the anti-addiction modes against the rebuilt implementation. Every launch flows
through `domain/policy/DefaultModeEngine` (the `ModeEngine`), which composes the modes
below into an ordered `LaunchDecision`; screens observe a `UiRestrictionState` derived
from the same engine.

## Implemented

- [x] **Intent-Based App Opening** — `FrictionStep.Reason` + `ReasonPrompt`; the reason is
  written to `LaunchEvent.reason` via `RecordLaunchUseCase`.
- [x] **Weekly patterns / Calm AI Assistant** — `BuildInsightsUseCase` summarises launch
  events + screen time into neutral insight text (Home + Screen Time + Reflection).
- [x] **Dopamine Detection Engine** — `RiskEvaluator` scores repeated opens, rapid
  switching, late-night launches and long sessions into a `RiskTier`; persisted via
  `RiskRepository`, re-run by `RiskEvaluationWorker`.
- [x] **Slow Mode** — adds delay seconds in `FrictionRules`/`DefaultModeEngine`.
- [x] **Dead-End Feeds** — repeated distracting opens emit `FrictionStep.DeadEnd` →
  `DeadEndResetScreen` (breathing + journaling).
- [x] **One-App-At-A-Time** — `oneAppAtATime` setting feeds friction; OS-level task
  closing is privilege-bound (see below).
- [x] **Friction Layers** — `FrictionLevel` {Light, Monk, Hardcore} scale delays, reason
  and confirmation requirements.
- [x] **Analog Mode** — `analogModeEnabled` adds intent + list friction.
- [x] **Real Focus Sessions** — `ModeEngine.isFocusBlocked` blocks social / entertainment /
  browser / store / game while `focusActive`; enforced in-scope by
  `FocusBlockAccessibilityService`.
- [x] **Usage Reflection** — `BuildReflectionUseCase` + `ReflectionScreen`, scheduled by
  `NightlyReflectionWorker`.
- [x] **Environment Modes** — Study / Sleep / Gym / Deep Work / Outside via
  `EnvironmentRules`.
- [x] **Invisible Social / Minimal Social Layer** — `FilterAppsUseCase` hides SOCIAL from
  default lists; `AppRepository.search` still reaches them by deliberate intent.
- [x] **Reward Real Life** — low `RiskTier` relaxes restrictions in `restrictionState`.
- [x] **Dynamic Minimalism** — `UiRestrictionState.minimalismLevel`/hidden surfaces scale
  with risk.
- [x] **Recovery Mode** — sustained high risk forces grayscale/focus suggestion.
- [x] **E-Ink Simulation** — `einkSimulationEnabled` disables motion; `EInkBackdrop` +
  `Modifier.grayscale`.
- [x] **Breath Unlock** — `FrictionStep.Breath` + `BreathOverlay`.
- [x] **Greyscale** — enforced inside the launcher UI via `Modifier.grayscale`.

## Needs platform privileges to become strict (documented in `PlatformGuardPolicy`)

- [ ] True OS-level app closing for One-App-At-A-Time (device-owner).
- [ ] Blocking quick settings / installs / notifications outside launcher scope.
- [ ] Detecting scroll velocity inside third-party apps.
- [ ] System-wide grayscale/dim without `WRITE_SECURE_SETTINGS` / device-owner.
- [ ] Preventing quick focus-mode toggling with device-owner strength.
