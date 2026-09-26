# Focus App — Spec & Build Plan

> "It's a minimalist Android app where you decide how much time you want to give something every day, and it blocks the apps you choose while that time counts down."

## 1. Product Idea

Give your time to something. The app protects that time.

The user defines daily goals (e.g. Mathematics — 1 hour, DSA — 1 hour, Reading — 30 min). Starting a goal begins a countdown; during that time, selected distracting apps are blocked. The only mechanic is: **remaining time keeps decreasing.** No score, no XP, no streaks, no social feed, no AI coach, no dashboard. Feels like an elegant timer/lifestyle utility, not a productivity app.

## 2. Product Principles

- **Minimalism** — every screen answers one question. No excessive cards/stats/gamification/animation/motivational copy/complex nav.
- **Time is the primary UI element** — `01:00:00` shrinking to `00:42:17` should feel visceral.
- **The app protects a decision already made** — it never says "you should study"; it says "you gave Mathematics one hour, here's your hour."
- **Two visual modes**:
  - *Normal* — light background, two primary colors, subtle secondary colors, clean typography, minimal cards.
  - *Minimal* — near-black, extremely dim, only goal name + remaining time, no controls, meant for leaving the phone aside while working.

## 3. Main User Journey

```
First Launch → Welcome → App Permission → Create Goals → Configure Daily Time
→ Configure Apps to Block → Today's Goals → Start Goal → Pre-session confirmation
→ Active Session → Blocked App Protection → Timer decreases → Session Complete
→ Goal completed → Today's remaining goals
```

## 4. Screens (behavioral spec)

### Welcome (first launch)
No account, no sign-up, no email, no backend — usable immediately.
```
Your time. Your rules.
Give time to what matters. We'll keep the distractions away.
[ Start ]
```

### Permission
Explain *why* usage-access permission is needed before asking.
```
One small thing
To keep you away from selected apps, the app needs permission to monitor app usage.
[ Allow access ]   Why is this needed?
```
If denied: `[ Try Again ]` / `[ Continue without blocking ]` (decide at build time whether the second option ships).

### Goal Setup
A goal is not a task. "Mathematics" means "I want to give Mathematics one hour today," not "finish chapter 4."
```
WHAT DO YOU WANT TO GIVE TIME TO?
Mathematics   1 hour
DSA           1 hour
Projects      1 hour
Reading       30 min
+ Add goal          Continue
```

### Create Goal
- Name (free text)
- Daily duration: 15/30/45 min, 1h, 1.5h, 2h, or Custom (`01:20` arbitrary entry)

### Apps to Block (per goal)
Blocking is contextual — each goal has its own app list (e.g. Mathematics blocks YouTube, Projects allows it).
```
MATHEMATICS
Daily time  01:00:00
Apps to block
Instagram  ON   YouTube  ON   Reddit  ON   X  ON   Chrome  OFF   WhatsApp  OFF
[ Save ]
```

### Today (main screen)
```
Today
3h 30m remaining
Mathematics   1:00:00
DSA           1:00:00
Projects      1:00:00
Reading         30:00
```
Completed goals become visually muted (e.g. `Mathematics  ✓`). No percentage dashboard.

### Start Goal / Pre-session review
```
MATHEMATICS
1 hour
These apps will be blocked: Instagram, YouTube, Reddit, X
[ Start ]
```

### Commitment Confirmation
```
MATHEMATICS
01:00:00
Instagram / YouTube / Reddit / X
They'll stay blocked until your session ends.
[ Start ]
```

### Active Session (the most important screen)
Countdown is the hero. Emphasize `42:17 remaining`, never `42% completed`.
```
Mathematics
00:42:17
1:00:00 total     4 apps blocked
[ Pause ]
```
Includes the beer-can/ice visual (section 5 below).

### Blocked App interception
```
Instagram
Not right now. You're in a Mathematics session.
42 minutes left.
[ Go back ]
```
No shaming — it simply enforces the decision.

### Ending Early
Two-step confirm. Ending does **not** mark the goal complete — remaining time is preserved honestly (e.g. `Mathematics — 42 minutes remaining`, not partial credit).
```
END SESSION?  You still have 42 minutes. Everything will become available again.
[ Keep going ]  [ End session ]
→ Session ended. Mathematics — 18 minutes completed.  [ Done ]
```

### Session Complete
```
Mathematics
✓  Done.
01:00:00
One less thing today.
[ Close ]
```
Calm — no "AMAZING!!!" or "productivity god" messaging.

### Minimum / Immersive Mode (signature feature)
Full black, only goal name + timer. Tap reveals Pause/Exit/End controls temporarily, then they auto-hide. Session continues if screen goes off; must survive backgrounding.
```
(near-empty screen)
42:17
```

### End-of-Day / History
```
Today            2h 30m / 3h
Mathematics ✓   DSA ✓   Projects 30m   Reading —
30m left
```
History view: monthly calendar dots + per-goal totals + tap-a-date detail. No leaderboard, no social, no achievements.

### Goals / Settings
Goals list → tap → detail (duration, blocked apps) → Edit. Deleting a goal requires confirmation and does not delete historical session data.
Settings: App Blocking, Notifications, Minimum Mode, Appearance (Light/Dark/System), Sound & Haptics, About.

### Navigation
Bottom-level: **Today / Goals / History / Settings**. During an active session, don't encourage navigating away.

## 5. Beer Can / Ice Visual Mechanic

Metaphor: **time is melting the ice around a can.** Not "you're being productive" — just "time is passing." Smooth continuous melt from 100% ice-covered → 0% (can fully free), driven by remaining-time fraction. Slightly more noticeable animation in the final ~10%, but the timer digits stay dominant at all times. Subtle, not cartoonish.

## 6. Data Model

```kotlin
Goal(
    id: Long,
    name: String,
    dailyDurationSeconds: Long,
    icon: String,
    color: String,
    isActive: Boolean
)

BlockedApp(
    id: Long,
    goalId: Long,
    packageName: String,
    appName: String
)

// Today's instance of a recurring goal
DailyGoal(
    id: Long,
    goalId: Long,
    date: LocalDate,
    allocatedSeconds: Long,
    completedSeconds: Long,
    status: GoalStatus   // PENDING, ACTIVE, COMPLETED, INTERRUPTED
)

Session(
    id: Long,
    dailyGoalId: Long,
    startedAt: Instant,
    endedAt: Instant?,
    allocatedSeconds: Long,
    completedSeconds: Long,
    status: SessionStatus
)
```

## 7. Timer Requirements (critical engineering detail)

**Never** drive the countdown with `countdown--` in memory. Persist `sessionStartTime` / `sessionEndTime` and always derive:

```
remaining = sessionEndTime - currentTime
```

This must hold across: app backgrounded, process killed, screen off, app reopened. Handle explicit interruption cases: process killed, device reboot, force-stop, permission revoked, system time changed, required access disabled mid-session.

```
Session lifecycle:
PENDING → STARTED → COMPLETED → FINISHED
                  → INTERRUPTED → RESUME / END
```

## 8. App Blocking Requirements

1. Know which apps are configured for the *active* goal (blocking is per-goal, not global).
2. Detect foreground-app changes.
3. Decide if the new foreground app is blocked.
4. Show the block screen.
5. Let the user return.
6. Stop blocking immediately at session end.
7. Recover correctly after interruption/reboot.

Treat this as its own module — mechanism must account for Android version differences (`UsageStatsManager` polling vs. `AccessibilityService`) and platform restrictions (background execution limits, battery optimization, special usage-access permission flow).

## 9. Architecture

```
UI (Compose Screens → ViewModels → UI State)
   ↓
Domain (StartSession, EndSession, CalculateRemainingTime, CompleteGoal,
        GetTodayGoals, ObserveForegroundApp, ShouldBlockApp)
   ↓
Data (Room, Repositories, DataStore/Preferences)
   ↓
Platform (App Usage Detection, App Blocking, Notifications, System Time, Lifecycle)
```

Package structure:
```
com.focus.app
├── core        (time, permissions, notifications, platform)
├── data        (database, datastore, repository)
├── domain      (goal, session, blocking)
├── feature     (onboarding, today, goals, session, history, settings)
└── blocking    (detector, controller, ui)
```

Stack: Kotlin, Jetpack Compose, MVVM, Clean Architecture, Coroutines, StateFlow, Room, DataStore, Hilt, Navigation Compose, WorkManager. Use libraries only where they solve a real problem — don't turn this into a dependency showcase.

## 10. Visual Design System

```
Background      #F7F5EF
Primary text    #18212B
Secondary text  #66707A
Primary dark    #263640
Soft accent     #DDE5EC
```
Plus a single small accent color per goal/can state — never a rainbow dashboard. Rounded rectangles, thin borders, large whitespace, simple icons, soft shadows, minimal gradients, mostly two-tone UI, large readable timer.

## 11. MVP Scope

**In:** onboarding, goal creation, daily duration, today's goals, per-goal blocked apps, session countdown, app blocking, session completion, end-session, persistent (clock-based) timer, minimum mode, melting ice/can animation, basic history.

**Explicitly out:** login, cloud sync, social features, leaderboards, friends, AI, streaks, achievements, subscriptions, ads, backend, cross-platform.

**V2 (resist scope creep until MVP feels excellent):** automatic daily goal generation, weekly stats, goal scheduling, rest days, reminder notifications, richer themes/animations, backup/export, optional cloud sync, widgets.

## 12. Engineering Quality Bar

- **Unit tests**: remaining-time calculation, goal completion, session lifecycle, interrupted sessions, daily goal generation, blocking decisions.
- **Integration tests**: Room, session persistence, goal persistence.
- **UI tests**: create goal, start session, complete session, end session early.
- **CI** (GitHub Actions): push → compile → lint → unit tests → instrumentation/UI tests.
- **README**: what/why, screenshots, architecture, how blocking works, timer architecture, permissions, testing, known Android limitations.

## 13. Build Plan (one week, learning-oriented)

Vertical-slice approach: get one goal fully real (Room → repo → clock-based timer → one real Compose screen) before widening, rather than building UI against fake state first. The timer/session code is the most valuable thing to learn correctly here, and Compose state wiring changes once persistence is real — better to not redo it.

- **Day 1** — Project scaffold: git init, package structure, Hilt setup, Room schema (Goal, DailyGoal, Session), DataStore.
- **Day 2** — Timer engine: `sessionStartTime`/`sessionEndTime` persistence, `CalculateRemainingTime`, survives process death/reboot. Unit-test heavily.
- **Day 3** — Compose: Today screen + Active Session screen wired to the *real* timer/repository.
- **Day 4** — Onboarding + Goal creation/editing + per-goal blocked-apps config.
- **Day 5** — Platform module: usage-access permission flow + foreground-app detection + block screen.
- **Day 6** — Minimum mode + ice/can melt animation (Compose Canvas or Lottie).
- **Day 7** — History screen, polish, instrumentation tests, README.
