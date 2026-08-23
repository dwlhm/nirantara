# Nirantara Product Backlog & Horizon Index

Welcome to the product backlog and specification index for **Nirantara Launcher**. This repository houses detailed product requirements documents (PRDs), feature concepts, and architectural feasibility assessments for upcoming releases.

---

## 🌟 Product Vision & North Star

**Nirantara** is an intentional, ergonomic, and blazingly fast Android launcher designed to restore agency to mobile computing.

- **Intentionality**: Computing should serve human focus, not hijack attention. Interactions are designed to be purposeful, conscious, and calm.
- **Ergonomic Simplicity**: Engineered from the ground up for thumb-first, one-handed navigation using physics-driven wave dynamics and bottom-biased reachability.
- **Zero Bloat**: No feeds, no news tickers, no unnecessary background services, no persistent clutter, and zero telemetry.

---

## ⚖️ The Three Product Laws

Every feature proposed, accepted, or built into Nirantara must strictly adhere to **The Three Product Laws**. Any proposal violating these laws is automatically dropped.

```
+-----------------------------------------------------------------------------+
|                               THE THREE LAWS                                |
+-----------------------------------------------------------------------------+
| 1. THE ZERO-PIXEL IDLE RULE                                                 |
|    In its idle state, Nirantara must never display unrequested UI elements, |
|    telemetry widgets, promotional cards, or persistent visual noise.       |
|                                                                             |
| 2. THE ONE-THUMB BOUNDARY                                                   |
|    100% of core navigational and utility actions must be comfortably         |
|    accessible within the lower ergonomic thumb sweep zone without grip shift.|
|                                                                             |
| 3. SUB-100MS PERCEIVED LATENCY                                              |
|    Every gesture, search query evaluation, and transition must render within|
|    100 milliseconds, maintaining fluid 60/120 FPS hardware acceleration.    |
+-----------------------------------------------------------------------------+
```

1. **The Zero-Pixel Idle Rule**: When resting on the home screen or idle, the interface displays only what the user explicitly pinned or configured. Features must live on-demand, disappearing instantly when completed.
2. **The One-Thumb Boundary**: Every interaction—scrolling the alphabetical wave, initiating searches, invoking micro-utilities, or launching applications—must remain fully operable with a single thumb.
3. **Sub-100ms Perceived Latency**: Speed is a core feature. Computations (such as inline math evaluations or fuzzy filtering) must be instant and local, with zero perceptible lag.

---

## 📋 Feature Backlog Index

| Spec File | Feature Name | Priority | Status | Summary |
| :--- | :--- | :--- | :--- | :--- |
| [`01_inline_micro_utility_command_bar.md`](01_inline_micro_utility_command_bar.md) | **Inline Micro-Utility Command Bar** | High | `Ready for Spec` | Instant sub-100ms arithmetic and unit conversions directly inside the search drawer. |
| [`02_mindful_focus_pomodoro_integration.md`](02_mindful_focus_pomodoro_integration.md) | **Mindful Focus & Pomodoro Integration** | High | `Ready for Spec` | Calm ambient Pomodoro focus sessions paired with mindful breathing intentional pauses. |
| [`03_contextual_habit_glances.md`](03_contextual_habit_glances.md) | **Contextual Habit Glances** | Medium | `Future Horizon` | Quiet, battery-neutral adaptive top shelf for temporal routine applications. |

---

## 🚦 Status Classification Definitions

- 🟢 **`Ready for Spec`**: The feature problem statement, user value proposition, edge cases, interaction flows, and anti-bloat guardrails are fully approved. The feature is ready for technical architecture planning, Jetpack Compose UI implementation, and unit testing.
- 🟡 **`Future Horizon`**: Exploratory concepts undergoing ergonomic assessment, battery impact evaluation, or user workflow refinement. Implementation is deferred until technical prerequisites and ergonomic viability are satisfied.
- 🔴 **`Dropped`**: Concepts or feature requests that violate The Three Product Laws, introduce screen clutter, require continuous background polling, or detract from core launcher minimalism.
