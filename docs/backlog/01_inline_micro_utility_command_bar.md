# Product Backlog: Inline Micro-Utility Command Bar

| Metadata | Details |
| :--- | :--- |
| **Spec ID** | `SPEC-01` |
| **Feature Title** | Inline Micro-Utility Command Bar |
| **Status** | `Ready for Spec` |
| **Priority** | High |
| **Target Component** | Search Drawer / App Query Engine |

---

## 1. Problem Statement

Mobile users constantly experience attention fragmentation from 3-second micro-tasks. Performing basic arithmetic (e.g., calculating a tip or split bill), unit conversions (e.g., currency, temperature, distances), or capturing a fleeting note requires:
1. Exiting the current context or home launcher.
2. Locating and launching a heavyweight calculator, browser, or converter application.
3. Waiting for app cold-start, navigating ads or complex UI layouts.
4. Copying the result and switching back.

This multi-step friction turns a 3-second mental task into a 30-second context switch, exposing the user to attention traps and notification rabbit holes.

---

## 2. User Value Proposition & Core Metrics

- **Value Proposition**: Execute rapid computations and conversions instantly at the speed of thought directly inside Nirantara's search drawer without leaving the launcher or opening external apps.
- **Core Metric**: **Sub-100ms Perceived Latency** from the last keystroke to the rendering of the evaluated result pill.
- **Secondary Metric**: 100% offline functionality for standard math and static physical unit conversions.

---

## 3. User Mental Model & Workflow

The user treats the existing search bar as a single omni-box command input. When text matches an operational pattern, Nirantara seamlessly elevates an inline answer pill without obscuring the standard app search fallback.

```
+-------------------------------------------------------------+
|  [ 45 * 12                                            (X) ] |  <- Search Input
+-------------------------------------------------------------+
|  (= 540)  [Tap to Copy Result]                              |  <- Inline Answer Pill
+-------------------------------------------------------------+
|  APPS                                                       |
|  [#] Calculator                                             |
|  [C] Calendar                                               |
+-------------------------------------------------------------+
```

### Detailed Interaction Lifecycle

1. **Invocation**: The user swipes up or taps the search bar to reveal the search drawer and keyboard.
2. **Input Entry**: The user types a mathematical expression (e.g., `45 * 12`, `15% of 250000`) or a conversion query (e.g., `50 usd to idr`, `120 km in miles`).
3. **Instant Evaluation**: 
   - A reactive parser evaluates the token stream in real time.
   - An **Inline Answer Pill** animates into view directly beneath the input field with a subtle fade/scale transition (`<100ms`).
4. **Action & Haptics**:
   - **Single Tap on Answer Pill**: Copies the computed value (e.g., `540`) to the system clipboard, displays a micro-toast confirmation, and triggers a soft tactile haptic wave.
   - **Long Press on Answer Pill**: Appends the answer to the search bar for chained calculations (e.g., `540 + `).
5. **Dismissal & Reset**: Clearing the query or pressing backspace immediately collapses the answer pill and restores the alphabetical application drawer without stutter or layout shift.

---

## 4. Supported Expression Patterns & Query Syntax

The parser must support natural, intuitive syntax variants:

### A. Arithmetic Expressions
- Standard operators: `+`, `-`, `*` or `x`, `/`, `^`, `%` (modulo or percentage)
- Natural percentage syntax:
  - `15% of 250000` -> `37,500`
  - `250000 + 10%` -> `275,000`
  - `120 - 20%` -> `96`
- Parenthetical grouping: `(24 + 16) * 3` -> `120`

### B. Physical Unit Conversions (Offline-First)
- **Length & Distance**: `km`, `miles`/`mi`, `meters`/`m`, `cm`, `feet`/`ft`, `inches`/`in`
  - Example: `120 km in miles` or `120km to mi` -> `74.56 miles`
- **Mass & Weight**: `kg`, `lbs`/`pounds`, `g`, `oz`
  - Example: `5 kg to lbs` -> `11.02 lbs`
- **Temperature**: `C`, `F`, `K`
  - Example: `100 c to f` -> `212 °F`
- **Data Storage**: `GB`, `MB`, `TB`, `KB`
  - Example: `1024 mb in gb` -> `1 GB`

### C. Currency Conversions
- Syntax: `50 usd to idr`, `100 eur in usd`, `1500 jpy to eur`
- Offline Fallback: Cached daily/periodic exchange rate snapshot. If offline and cache is available, render result with subtle indicator `(offline rate)`.

---

## 5. Edge Cases & Conflict Resolution

1. **Ambiguity Resolution (App Names vs Expressions)**:
   - If an expression could match an installed app name (e.g., an app named `1Password` or `2048`), the search drawer **prioritizes the app match in the list** while still cleanly presenting the math evaluation pill above the list.
   - Letters without mathematical/conversion semantics (e.g., `abc + 123`) do not trigger the parser, preserving 100% pure app search.
2. **Division by Zero & Malformed Syntax**:
   - Incomplete expressions (e.g., `45 *`) or invalid operations (e.g., `12 / 0`) silently suppress the answer pill. No noisy error messages or red alerts appear during partial typing.
3. **Floating Point Precision**:
   - Results format automatically using sensible significant digits (max 4 decimal places, omitting trailing zeros, e.g., `3.1416`, `5.5`).

---

## 6. Anti-Bloat & Simplicity Guardrails

- **Zero Permanent UI Footprint**: No extra buttons, persistent toolbars, or dedicated calculator widgets on the home screen. The utility exists solely as an ephemeral response to search input.
- **Zero Required Configuration**: Works out of the box with zero setup screens, toggles, or permission requests.
- **Lightweight Parser Engine**: Implemented via a tiny, pure Kotlin recursive descent parser / lexer with zero third-party heavy dependencies.
