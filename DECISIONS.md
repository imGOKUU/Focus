# Decisions Log

A running record of the choices made while building this app and *why* — separate from `LEARNINGS.md` (technical things learned) and `SPEC.md`/`ORIGINAL_SPEC.md` (what we're building). Kept as a lightweight ADR (architecture decision record) so future-us doesn't have to re-litigate settled questions or forget why something is the way it is. Newest at the bottom.

---

## 1. Native Android, not Kotlin Multiplatform (KMP)

**Decision:** Build this as a native Android app, not KMP/Compose Multiplatform.

**Options considered:**
- KMP from day one, sharing UI (Compose Multiplatform) and domain logic across Android/iOS.
- Native Android first, decide on iOS later.

**Rationale:** The app's hardest and most distinctive feature — detecting the foreground app and blocking it — has no shared implementation across platforms. Android does it via `UsageStatsManager`/`AccessibilityService`; iOS does it via the entirely different Screen Time `DeviceActivity`/`ManagedSettings` APIs, which sit behind a special Apple entitlement you have to apply for. Since the one thing KMP is meant to save you from (rewriting logic per platform) can't be avoided for this app's core feature anyway, adopting KMP now would only add build/tooling complexity on top of everything else being learned (Compose, Room, Hilt, the timer engine), with no corresponding payoff.

**Revisit if:** An iOS version becomes an actual near-term goal — and even then, expect the blocking module to be a separate, deliberate build, not a shared one.

---

## 2. Build order: one vertical slice before widening

**Decision:** Build one goal fully real (Room → repository → clock-based timer → one real Compose screen) before building out the rest of the screens, rather than UI-first-with-fake-data or backend-first-with-no-UI.

**Options considered:**
- UI-first: build all Compose screens against fake/hardcoded state, wire real persistence and the timer in afterward.
- Backend-first: finish the whole data/domain layer, then start on screens.
- Vertical slice: one real path end-to-end first, then widen to the rest of the screens/features.

**Rationale:** The app's timer must be clock-based (`remaining = sessionEndTime - now`), not a naive `countdown--`, so it survives process death, reboot, and backgrounding (see spec section 28). That's also the single most valuable thing to learn correctly in this project. If screens are built first against fake `countdown--`-style state, that state-management code is largely thrown away once the real timer and persistence show up — better to learn the real pattern once, early, than twice.

---

## 3. Screens are pure functions of hoisted state — never own their data

**Decision:** Every screen composable takes its data (lists, current values) and its actions (button clicks, toggles) as parameters with safe defaults (`emptyList()`, no-op lambdas), rather than hardcoding sample content or reading from a data source itself.

**Where this shows up:** `WelcomeScreen(goals: List<GoalListItem> = emptyList(), onAddGoal = {}, onContinue = {})`, `GoalConfigScreen(blockableApps: List<BlockableAppItem> = emptyList(), ...)`.

**Rationale:** User's explicit correction after seeing a first hardcoded goal list: goals will be added by the user and read from the database, so the screen must be shaped to receive that data rather than bake in placeholder content. This is also just correct Compose practice — a composable that's a pure function of its input is trivially previewable, testable, and swappable between fake and real data with zero changes to the composable itself. Defaults exist purely so the file compiles standalone (e.g. `NavScreen.kt`'s current no-arg call) before real wiring is added — they are not meant to ship as real content.

**Applies going forward:** Every new screen should follow this shape. Anything that's genuinely fixed UI configuration (e.g. the picker's hour/minute step options, which are a UI control's range, not user data) is fine to default in the signature; anything that represents the user's actual goals/apps/history must come in as a parameter.

---

## 4. Blocking stays per-goal, not app-level/global

**Decision:** Keep `BlockedApp` scoped to a `goalId` (already modeled that way in Room) and keep the "Apps to Block" section on the per-goal configuration screen, rather than moving to one global blocklist.

**Options considered:**
- Per-goal blocking: each goal has its own independent block list (e.g. Mathematics blocks YouTube, Projects allows it).
- App-level/global blocking: one blocklist that applies no matter which goal session is active.

**Rationale:** Per-goal blocking is already the data model (`BlockedApp.goalId` foreign key, with an existing code comment: *"Mathematics can block YouTube while Projects allows it"*) and is a genuine product differentiator over generic app blockers — the same app can be blocked in one context and allowed in another. Global blocking is simpler to build but throws away that contextual nuance and would contradict the schema already in place.

**If reconsidered later:** Would mean dropping "Apps to Block" from the per-goal screen entirely and adding a single global list under Settings > App Blocking instead (spec section 25) — a deliberate scope cut, not a default.

---

## 5. Daily-time picker: two independent scroll-snap wheels, not a circular dial or a single preset list

**Decision:** Implement the daily-duration picker as two side-by-side scrollable "wheels" (hours, minutes in 5-minute steps) built on `LazyColumn` + `rememberSnapFlingBehavior`, rather than a circular clock-face dial or a single reel of preset durations (15m/30m/45m/1h/...).

**Options considered:**
- Circular clock dial (drag around a circle to set time).
- Single vertical reel of fixed preset durations.
- Two independent linear wheels (hours × minutes).

**Rationale:**
- A circular dial looks more "designed" but is objectively worse for precision input and needs custom gesture/angle-snapping logic for no real benefit — exactly the kind of flourish the spec's minimalism principle (section 2.1) warns against.
- A single list of presets can't represent an arbitrary custom duration like `01:20` (spec section 7 explicitly requires arbitrary custom values), because presets are discrete named options, not composable values.
- Two independent wheels solve this naturally: each wheel just reports its own centered value (hours, minutes), and any combination is reachable by construction — the "custom duration" feature falls out for free instead of needing a separate UI path.

**Implementation note:** Centering is derived by finding the visible item whose midpoint is closest to the viewport's midpoint (`derivedStateOf` over `listState.layoutInfo`), not by reading `firstVisibleItemIndex`/`scrollOffset` directly — see `LEARNINGS.md` for why.

---

## 6. Added `material-icons-extended` for goal-category icons

**Decision:** Add the `androidx.compose.material:material-icons-extended` library (via the version catalog) rather than hand-drawing category icons (bar chart, code brackets, monitor, book) with `Canvas`/`Path`.

**Options considered:**
- Hand-drawn vector icons via `Canvas`, matching the approach used for the landing-page illustration.
- Pull in the extended Material icon set.

**Rationale:** The landing-page illustration (sun + mountains) is a one-off flat illustration where hand-drawing made sense and kept things themeable with plain `Color` values. Goal-category icons are a different case — they're small, standard glyphs (a bar chart, `<>`, a monitor, a book) that the extended icon library already provides correctly at negligible size cost, and hand-rolling four-plus more glyphs via `Canvas` would be effort spent redrawing what already exists, not effort spent on the product. This is a UI asset dependency, not an app-functionality dependency (no networking, no business logic), so it doesn't conflict with the "don't add functionality" boundary on these screens.

---

## 7. Landing-page illustration: hand-drawn `Canvas` shapes, not image assets

**Decision:** Render the landing screen's sun-and-mountains illustration as layered `Path` polygons drawn in a `Canvas`, rather than shipping a PNG/SVG drawable.

**Rationale:** The illustration is simple flat geometry (a circle, a few triangular polygons in two-tone color). Drawing it with `Canvas` keeps the app free of image assets for something this simple, makes it re-themeable via plain `Color` values (e.g. easy to retint for dark mode later) without touching image files, and it's a technique worth having in hand for the ice/can melt animation later in the build (spec sections 13–15), which will need the same kind of shape-drawing approach.

---

## 8. Landing-page copy: image mockup as source of truth over the original spec text

**Decision:** Button reads "Get Started" (from the provided mockup image) rather than "Start" (the original spec document's placeholder text).

**Rationale:** User explicitly pointed to the image as the visual reference for this screen ("This would be the LandingPage" + screenshot). Where the image and the prose spec disagree on exact wording, the image — being the actual approved design — wins; the prose spec's bracketed labels (`[ Start ]`) were always illustrative placeholders, not final copy.