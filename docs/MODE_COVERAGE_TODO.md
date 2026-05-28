# Calm Launcher Mode Coverage TODO

This file tracks the requested anti-addiction modes against the current Android launcher implementation.

## Added in code

- [x] Intent-Based App Opening: launch gate asks "Why are you opening this?" and logs the reason.
- [x] Weekly patterns: launch reasons are summarized into calm insight text.
- [x] Dopamine Detection Engine: detects repeated opens, rapid app switching, repeated foreground sessions, and late-night launches, then raises grayscale/focus friction.
- [x] Slow Mode: increases launch delay and quiets interactions.
- [x] Dead-End Feeds: repeated addictive-app opens can route to a black reset screen with breathing and journaling prompts.
- [x] One-App-At-A-Time Mode: launch gate adds replacement friction. Full app task closing needs device-owner or accessibility support.
- [x] Friction Layers: Light, Monk, and Hardcore Monk change delay and reason requirements.
- [x] Analog Mode: adds page/list-style friction and extra intent requirement.
- [x] Real Focus Sessions: focus rules block browsers, stores, social, entertainment, and quick-exit paths inside launcher scope.
- [x] Calm AI Assistant: neutral usage insight text appears on home/settings.
- [x] Usage Reflection Screen: nightly reflection appears through insight/reflection surfaces.
- [x] Environment Modes: Study, Sleep, Gym, Deep Work, and Outside adjust app visibility and blocking rules.
- [x] Invisible Social Apps: social apps are hidden from home/app lists and only accessible intentionally through search/settings.
- [x] Reward Real Life: low-risk usage keeps the interface less restrictive; high-risk usag/e increases calm friction.
- [x] Dynamic Minimalism: unhealthy usage progressively hides suggestions/recent apps and increases minimal UI.
- [x] Recovery Mode: repeated risk can force focus/grayscale and dead-end feed friction.
- [x] E-Ink Simulation: monochrome/low-motion/slow refresh behavior is represented through theme and motion policy.
- [x] Breath Unlock: launch gate requires a breathing pause before opening.
- [x] Minimal Social Layer: social apps are removed from default surfaces and opened only by typed search intent.

## Needs platform privileges to become strict

- [ ] True OS-level app closing for One-App-At-A-Time Mode.
- [ ] Blocking quick settings, installs, browser networking, and notifications outside launcher scope.
- [ ] Detecting scrolling velocity inside third-party apps.
- [ ] Enforcing grayscale/dim globally without accessibility, device-owner APIs, or user-granted display permissions.
- [ ] Preventing quick focus-mode toggling with device-owner strength.
