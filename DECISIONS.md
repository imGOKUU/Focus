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

**Reconfirmed (2026-09-28):** a later design canvas (approved visual reference, separate from the original spec mockup) rendered this control as a single combined 3-row list ("0h 30m / 1h 00m / 2h 00m") rather than two wheels — a second, independent nudge toward decision (a) above. Explicitly asked; kept the dual-wheel version for the same reason as originally decided (arbitrary custom durations, e.g. `01:20`, aren't representable by a preset list). This means the built UI now knowingly diverges from the latest approved visual reference on this one control — worth surfacing to whoever owns that canvas rather than treating it as silently resolved.

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

---

## 9. "Apps to Block" list comes from real installed apps, not a curated/hardcoded set

**Decision:** `GoalConfigScreen`'s block list is populated from the device's actual launchable apps (`InstalledAppsProvider`, querying `PackageManager`), not a fixed list of the six apps shown in the mockup.

**Options considered:**
- Hardcode the six apps from the mockup (Instagram/YouTube/Reddit/X/Chrome/WhatsApp) as a seed list, with real package names, as a stopgap until a dedicated app-blocking platform module exists.
- Query real installed apps now.

**Rationale:** User explicitly rejected the seed-list stopgap — same principle as decision 3 (don't hardcode what should come from real device/user state), extended from "goals must come from the DB" to "blockable apps must come from the device." Resolving `Intent.ACTION_MAIN`/`CATEGORY_LAUNCHER` via `PackageManager.queryIntentActivities` returns every launchable app without needing the `QUERY_ALL_PACKAGES` permission or a `<queries>` manifest entry — that specific query is exempt from Android 11+ package-visibility restrictions.

**Known follow-up:** this currently returns *every* launchable app (potentially 100+, including system utilities), unfiltered. Worth revisiting with a filter (e.g. excluding `ApplicationInfo.FLAG_SYSTEM`) once tested on a real device — not done yet since it wasn't part of what was asked.

---

## 10. Goal-name field on GoalConfigScreen stays editable (confirmed)

**Decision:** Keep the `BasicTextField` (editable) rather than reverting to the design canvas's static, non-editable title.

**Rationale:** Explicitly asked and confirmed (2026-09-28) after a design-canvas review flagged the divergence. Letting a user rename a goal inline is a genuine improvement over the static mockup, not a bug to fix — same reasoning as when this was first built. No code change needed; this was already the implemented behavior.

---

## 11. UI pass aligned to an approved design canvas (colors, type, spacing)

**Decision:** Reconciled `WelcomeScreen`/`AddGoalsScreen`/`GoalConfigScreen` against an approved external design canvas's exact CSS values (colors, font sizes/weights, spacing, chip sizes) rather than the earlier hand-eyeballed approximations from the original spec mockups.

**What changed:** headline/button font weights corrected from Bold(700) to the canvas's actual SemiBold(600); three screen-specific type styles (`WelcomeHeadline`, `AddGoalsHeadline`, `ButtonLabel`) added to `Type.kt` instead of scattering `.copy()` overrides per screen; button height 56dp → 48dp; mountain/sun illustration hex values corrected to the canvas's exact values; goal-row and app-row chip sizes/spacing tightened to match; added real dividers between goal rows; switch on/off colors made explicit instead of relying on Material3 defaults; per-app fallback icon color (brand color for known apps, hash-based otherwise) instead of flat gray.

**Rationale:** The prior implementation was a reasonable approximation of the prose spec's ASCII mockups, but a pixel-accurate design canvas became available with the actual intended values — worth conforming exactly rather than leaving a visible gap between "close enough" and "correct," especially since the gap was concretely measurable (e.g. font weight literally differing, not just a matter of taste).

**Also renamed** `LandingPage` → `WelcomeScreen` and the old `WelcomeScreen` (goal list) → `AddGoalsScreen`, since the prior names had drifted from what each screen actually shows — `WelcomeScreen` is "Your time. Your rules.", `AddGoalsScreen` is "What do you want to give time to?".

---

## 12. Target platform is phone AND tablet (supersedes the "letterbox" framing in #11's `ResponsiveScreen`)

**Decision:** Tablets are a real target platform, not an untargeted edge case to gracefully degrade. Explicitly stated by the user (2026-09-28) and saved to memory (`target-platforms.md`) so it carries into future work, not just this session.

**Context this corrects:** Testing on a physical tablet during the design-canvas alignment pass (#11) surfaced a layout bug (phone-designed `weight()` spacing ballooning across 1333dp of tablet height). The fix at the time (`ResponsiveScreen`) capped content to a fixed ~480dp phone-shaped card, centered in blank space — a *letterboxing* pattern (same idea as how iOS handles non-optimized iPad apps). That was a reasonable stopgap when tablets weren't a stated target, but it's not real tablet support — it uses none of the extra width.

**What changed:** `ResponsiveScreen` now uses a breakpoint (Material's window-size-class pattern): compact widths (phones, <600dp) get the full available width exactly as before; medium/expanded widths (tablets) get a wider ~600dp reading-width column, still centered but meaningfully wider than the old fixed 480dp, and content is top-aligned rather than vertically centered (reads as a real screen starting at the top, not a floating card). The height cap (now 1000dp) still exists to keep weighted spacers from ballooning vertically — that part of the original reasoning holds regardless of target platform.

**Still not done:** this is "use the width sensibly," not a true tablet-adapted redesign (e.g. a two-pane list-detail layout for Goals list + Goal detail, larger illustrations, etc.). No tablet-specific design canvas exists yet — only phone mockups. A genuine two-pane adaptation would be a deliberate, separately-scoped feature once/if a tablet design reference exists, not something to improvise without one.

---

## 13. `FocusSoftAccent` vs `FocusIceAccent` — split, not merged

**Decision:** `FocusSoftAccent` (`#DDE5EC`) stays scoped to dividers/track fills (its original documented purpose); `FocusIceAccent` (`#EAF3F7`, added this session) is used exclusively for the Active Session screen's melting-ice fill. Removed the stale "...ice/track fill" wording from `FocusSoftAccent`'s comment.

**Rationale:** Explicitly confirmed with the user rather than silently picking one — the two colors' documented purposes had drifted into overlapping/contradictory territory (an old comment described `FocusSoftAccent` as covering "ice" before `FocusIceAccent` existed at all). Two colors, two distinct jobs, no ambiguity going forward.

---

## 14. Active Session screen ("FocusScreen"): UI only, no session/timer/blocking logic

**Decision:** Built `FocusScreen` (in `HomeScreen.kt` — file/function name left as-is, not renamed, to avoid touching `NavScreen.kt` while the user was actively hand-editing it) as a pure function of hoisted state (`goalName`, `remainingSeconds`, `totalSeconds`, `blockedAppsCount`, `accent`, `onPause`), with all-default parameters so the existing no-arg `FocusScreen()` call in `NavScreen.kt` keeps compiling untouched.

**Explicitly not built:** any ViewModel/use case/repository for sessions or timers (none existed before this — `Session`/`DailyGoal`/`SessionDao`/`TimeProvider` are schema-only so far, unlike `Goal`/`BlockedApp` which have a full chain), the real app-blocking mechanism, Minimum/Immersive mode, Session Complete/Ending Early screens, system-back-gesture interception during a session, or any paused-state visual treatment (`SessionStatus` has no `PAUSED` value yet).

**Rationale:** Matches the division of labor established for every prior screen in this project (UI as pure hoisted-state functions; business logic is the user's own build). The user's request ("start the focus and lock all the apps and the timer would start, and the most important the animation") described the screen's narrative purpose and prioritized the animation specifically — not a request to build the underlying session engine or platform blocking module, both of which are substantial, separate pieces of work.

**Timer formatting — two different formats, both taken literally from the spec's own examples, not unified:** the hero timer is always zero-padded `HH:MM:SS` (`00:42:17`, so digit width never shifts — paired with a `fontFeatureSettings = "tnum"` fix on `TimerDisplayLarge`/`TimerDisplayMinimal` in `Type.kt`, fulfilling a comment that had described tabular figures as intended but never actually set them); the total-time label is `H:MM:SS` with no hour padding (`1:00:00 total`), matching the spec's own inconsistent-looking but exact examples. Confirmed with the user rather than silently normalizing to one format.

**Ice/can illustration:** hand-drawn `Canvas`/`Path` (no image/Lottie assets), same technique as `WelcomeScreen`'s mountain illustration. The can is tinted with the goal's own `GoalAccent.onChip` color (ties the session visual back to that goal's identity in the goal list). The ice band's jagged-edge phase is seeded once via `remember { Random.nextFloat() * 2f * PI }` — deliberately *not* re-derived per frame/recomposition, since only the band's height should animate; re-seeding the jag per frame would make it visibly crawl/boil as the timer ticks, contradicting the spec's "smooth... not cartoonish" requirement. A subtle `rememberInfiniteTransition` alpha pulse is always running but only visually applied when progress ≤ 10% remaining (gating the *value*, not the animation call, avoids any restart/flash at that threshold) — matches the spec's "slightly more noticeable in the final ~10%" note.