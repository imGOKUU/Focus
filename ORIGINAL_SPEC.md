# Focus App — Product & Engineering Specification

> This is the original spec document as given, verbatim. `SPEC.md` is a condensed/reorganized working version of this plus the build plan we agreed on — use that one day-to-day and treat this file as the source of truth to check back against.

## 1. Product Idea

A minimalist Android app built around one simple idea:

> **Give your time to something. The app protects that time.**

The user defines daily goals such as:

- Mathematics — 1 hour
- DSA — 1 hour
- Projects — 1 hour
- Reading — 30 minutes

When the user starts a goal, a countdown begins.

During that time, selected distracting applications are blocked.

The psychological mechanic is deliberately simple:

> **The remaining time keeps decreasing.**

There is no productivity score, XP, streak pressure, social feed, AI coach, or complicated dashboard.

The application should feel closer to an elegant timer/lifestyle utility than a productivity application.

---

# 2. Product Principles

### 2.1 Minimalism

Every screen should answer one question.

Avoid:

- excessive cards
- excessive statistics
- gamification
- unnecessary animations
- motivational messages
- complicated navigation
- feature-heavy dashboards

### 2.2 Time is the primary UI element

The most important information is:

```text
01:00:00
```

and then:

```text
00:42:17
```

The user should visually feel that the amount of remaining time is shrinking.

### 2.3 The application protects the user's decision

The application doesn't tell the user:

> "You should study."

The user already decided that.

The application simply says:

> "You gave Mathematics one hour. Here's your hour."

### 2.4 Two visual modes

Normal mode:

- very light background
- two primary colors
- subtle secondary colors
- clean typography
- minimal cards
- YPT-inspired simplicity

Minimal mode:

- almost completely black/dark
- extremely dim
- only the goal and remaining time
- no unnecessary controls
- intended for leaving the phone somewhere while working

---

# 3. Main User Journey

```text
First Launch
     ↓
Welcome
     ↓
App Permission
     ↓
Create Goals
     ↓
Configure Daily Time
     ↓
Configure Apps to Block
     ↓
Today's Goals
     ↓
Start Goal
     ↓
Pre-session confirmation
     ↓
Active Session
     ↓
Blocked App Protection
     ↓
Timer decreases
     ↓
Session Complete
     ↓
Goal completed
     ↓
Today's remaining goals
```

---

# 4. First Launch

## Screen: Welcome

The first screen should immediately communicate the product.

```text
────────────────────────

        Your time.
        Your rules.

   Give time to what matters.
   We'll keep the distractions away.

              [ Start ]

────────────────────────
```

No account.

No sign-up.

No email.

No backend.

The application should be usable immediately.

---

# 5. Required Permission

The app needs permission/access necessary to determine which application is currently being used and enforce the selected blocking behavior.

Screen:

```text
────────────────────────

       One small thing

 To keep you away from selected
 apps, the app needs permission
 to monitor app usage.

       [ Allow access ]

        Why is this needed?

────────────────────────
```

The user should be able to understand why the permission exists.

If permission is denied:

```text
Permission required

App blocking cannot work without
this permission.

[ Try Again ]
[ Continue without blocking ]
```

The second option can be considered depending on the final product decision.

---

# 6. Goal Setup

The user defines their own goals.

Example:

```text
WHAT DO YOU WANT
TO GIVE TIME TO?

Mathematics          1 hour
DSA                  1 hour
Projects             1 hour
Reading              30 min

        + Add goal

             Continue
```

A goal is not a task.

For example:

```text
Mathematics
```

doesn't mean:

```text
Finish chapter 4
```

It means:

> I want to give Mathematics one hour today.

---

# 7. Create Goal

Fields:

### Goal name

Example:

```text
Mathematics
```

### Daily duration

Options:

```text
15 min
30 min
45 min
1 hour
1.5 hours
2 hours
Custom
```

Custom duration should allow arbitrary values.

Example:

```text
01 : 20
```

---

# 8. Apps to Block

Each goal can have its own blocking configuration.

Example:

```text
MATHEMATICS

Daily time
01:00:00

Apps to block

Instagram       ON
YouTube         ON
Reddit          ON
X               ON
Chrome          OFF
WhatsApp        OFF

              [ Save ]
```

This is important because blocking should be contextual.

For example:

### Mathematics

```text
Instagram     blocked
YouTube       blocked
Reddit        blocked
Chrome        allowed
```

### Projects

```text
Instagram     blocked
Reddit        blocked
YouTube       allowed
Chrome        allowed
```

The user decides.

---

# 9. Today's Screen

This is the main screen.

```text
                 Today

            3h 30m remaining


     Mathematics             1:00:00

     DSA                     1:00:00

     Projects                1:00:00

     Reading                   30:00
```

The overall remaining time should decrease as goals are completed.

A goal that has already been completed can become visually muted.

Example:

```text
Mathematics             ✓
DSA                     1:00:00
Projects                1:00:00
Reading                   30:00
```

Avoid a large percentage dashboard.

---

# 10. Start Goal

Tap:

```text
Mathematics
```

The application shows:

```text
MATHEMATICS

1 hour

These apps will be blocked:

Instagram
YouTube
Reddit
X

             [ Start ]
```

This is the last opportunity to review the session.

---

# 11. Commitment Confirmation

Before starting:

```text
MATHEMATICS

      01:00:00

Instagram
YouTube
Reddit
X

They'll stay blocked
until your session ends.

        [ Start ]
```

Once started, the timer begins.

---

# 12. Active Session

This is the most important screen.

```text
────────────────────────

          Mathematics

          00:42:17

        ───────────

      1:00:00 total

       4 apps blocked

              [ Pause ]

────────────────────────
```

The countdown is the hero.

The timer must always decrease.

Do not emphasize:

```text
42% completed
```

Instead emphasize:

```text
42:17 remaining
```

---

# 13. Beer Can / Ice Mechanic

The visual metaphor:

> **Time is melting the ice around the can.**

At session start:

```text
       ┌─────────┐
       │  CAN    │
       │         │
       └─────────┘
      ❄ ❄ ❄ ❄ ❄
```

The ice gradually melts as the remaining time decreases.

Example:

### 100%

Can heavily surrounded by ice.

### 75%

Some ice has melted.

### 50%

Can becomes more visible.

### 25%

Almost all ice is gone.

### 0%

The can is completely revealed.

The animation should be subtle rather than cartoonish.

The metaphor should communicate:

> **Time is passing.**

Not:

> "You're being productive."

---

# 14. Near Completion

At approximately the final 10%:

```text
          Mathematics

           00:05:12

              🧊
             CAN
```

The animation becomes slightly more noticeable.

But the timer remains dominant.

---

# 15. Session Complete

At zero:

```text
             Mathematics

                  ✓

                 Done.

              01:00:00

        One less thing today.

               [ Close ]
```

The can is now completely out of the ice.

A very short completion animation can play.

No:

```text
🔥 AMAZING!!!
🏆 YOU'RE A PRODUCTIVITY GOD
```

The application should remain calm.

---

# 16. Blocked Application

When the user attempts to open a blocked application:

```text
────────────────────────

             Instagram

            Not right now.

        You're in a Mathematics
             session.

             42 minutes left.

             [ Go back ]

────────────────────────
```

This screen should be extremely simple.

The application should not shame the user.

It simply enforces the decision.

---

# 17. Ending Early

The user can choose to stop a session.

First confirmation:

```text
END SESSION?

You still have 42 minutes.

Everything will become
available again.

       [ Keep going ]

       [ End session ]
```

If they confirm:

```text
Session ended.

Mathematics
18 minutes completed

            [ Done ]
```

The goal should NOT be marked completed.

Remaining time stays:

```text
Mathematics
42 minutes remaining
```

This preserves honesty.

---

# 18. Minimum / Immersive Mode

This should be one of the signature features.

User taps:

```text
[ Minimum ]
```

The entire application becomes almost completely black.

```text
────────────────────────




              42:17


────────────────────────
```

Possibly:

```text
Mathematics

42:17
```

and nothing else.

Brightness should be extremely low visually.

No cards.

No buttons.

No navigation.

No statistics.

No distracting elements.

A tap can reveal controls temporarily.

For example:

```text
Tap
 ↓

Pause
Exit Minimum Mode
End Session
```

After a few seconds, controls disappear again.

---

# 19. Minimum Mode Behaviour

Requirements:

- Full-screen experience
- Hide unnecessary system UI where Android permits
- Very dark background
- Dim typography
- Timer remains readable
- Screen should not constantly animate
- Prevent accidental interaction as much as practical
- Tap to reveal controls
- Automatic control hiding
- Session continues even if screen goes off

The user should be able to put the phone beside them and essentially forget about it.

---

# 20. Today's Completion

When a goal completes:

```text
Today

Mathematics              ✓
DSA                      1:00:00
Projects                 1:00:00

2h remaining
```

The global remaining time decreases.

Example:

```text
Before:

3h remaining


After Mathematics:

2h remaining
```

This creates the second psychological loop:

> **The day is getting lighter.**

---

# 21. End-of-Day Screen

Keep it simple.

```text
               Today

             2h 30m / 3h

       Mathematics          ✓
       DSA                  ✓
       Projects             30m
       Reading              —

────────────────────────────

              30m left
```

No complicated analytics initially.

---

# 22. Goals Screen

Navigation:

```text
Today      Goals      Settings
```

Goals:

```text
My Goals

Mathematics       1 hour
DSA               1 hour
Projects          1 hour
Reading           30 min

               + Add Goal
```

Tap a goal:

```text
Mathematics

Daily time
1 hour

Blocked apps
Instagram
YouTube
Reddit
X

[ Edit ]
```

---

# 23. Goal Editing

The user can modify:

- Goal name
- Daily duration
- Blocked applications
- Goal icon/color

Deleting a goal should require confirmation.

Deleting a goal should not delete historical session data.

---

# 24. History

The history should remain minimal.

Example:

```text
September

M T W T F S S
● ● ● ● ● ○ ●

Total
31h 42m

Mathematics
12h 30m

DSA
8h 20m

Projects
7h 40m
```

The user can tap a date:

```text
September 18

Mathematics       1:00
DSA               1:00
Projects          0:42
```

No leaderboard.

No social features.

No achievements.

---

# 25. Settings

Only essential settings.

```text
Settings

App Blocking
Notifications
Minimum Mode
Appearance
Sound & Haptics
About
```

Potential options:

### App Blocking

- Usage access status
- Re-check permission
- Blocked-app behavior

### Notifications

- Session started
- Session completed
- Session interrupted

### Appearance

- Light
- Dark
- System

### Sound & Haptics

- Completion sound
- Haptic feedback

---

# 26. Navigation

Keep navigation extremely small.

```text
              ┌──────────┐
              │   TODAY  │
              └────┬─────┘
                   │
       ┌───────────┼───────────┐
       ▼           ▼           ▼
     Goals      History     Settings
       │
       ▼
   Goal Detail
       │
       ▼
 Start Session
       │
       ▼
 Active Session
       │
       ├──────────────┐
       ▼              ▼
 Completed         End Early
```

During an active session, the user should not be encouraged to navigate around the app.

---

# 27. Data Model

At the product level, we need only a few entities.

## Goal

```kotlin
Goal(
    id: Long,
    name: String,
    dailyDurationSeconds: Long,
    icon: String,
    color: String,
    isActive: Boolean
)
```

## BlockedApp

```kotlin
BlockedApp(
    id: Long,
    goalId: Long,
    packageName: String,
    appName: String
)
```

## DailyGoal

This represents today's instance of a recurring goal.

```kotlin
DailyGoal(
    id: Long,
    goalId: Long,
    date: LocalDate,
    allocatedSeconds: Long,
    completedSeconds: Long,
    status: GoalStatus
)
```

Possible status:

```kotlin
enum class GoalStatus {
    PENDING,
    ACTIVE,
    COMPLETED,
    INTERRUPTED
}
```

## Session

```kotlin
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

---

# 28. Timer Requirements

The timer must NOT simply depend on:

```kotlin
countdown--
```

because the application could be paused or killed.

Instead, persist:

```text
sessionStartTime
sessionEndTime
```

Remaining time is calculated from the clock.

Conceptually:

```text
remaining =
    sessionEndTime - currentTime
```

Therefore:

- App backgrounded → timer remains correct
- Process killed → timer remains correct
- Device screen turned off → timer remains correct
- App reopened → timer resumes from the correct point

This is an important engineering detail.

---

# 29. App Blocking Requirements

The application must:

1. Know which apps are configured for the active goal.
2. Detect when the foreground application changes.
3. Determine whether the new application is blocked.
4. Show the blocking experience.
5. Allow the user to return.
6. Stop blocking immediately when the session ends.
7. Restore normal behavior after interruption/reboot as appropriate.

The implementation mechanism needs to account for Android version differences and platform restrictions.

This should be treated as its own module.

---

# 30. Session Lifecycle

```text
PENDING
   │
   ▼
STARTED
   │
   ├───────────────┐
   │               │
   ▼               ▼
COMPLETED       INTERRUPTED
   │               │
   ▼               ▼
FINISHED        RESUME / END
```

Potential interruptions:

- Process killed
- Device reboot
- App force-stopped
- Permission revoked
- System time changes
- User disables required access

These need explicit handling.

---

# 31. Architecture

This is where the joke becomes real.

I'd use:

```text
UI
│
├── Compose Screens
│
├── ViewModels
│
└── UI State
        │
        ▼
Domain
│
├── StartSession
├── EndSession
├── CalculateRemainingTime
├── CompleteGoal
├── GetTodayGoals
├── ObserveForegroundApp
└── ShouldBlockApp
        │
        ▼
Data
│
├── Room
├── Repositories
└── Preferences/DataStore
        │
        ▼
Platform
│
├── App Usage Detection
├── App Blocking
├── Notifications
├── System Time
└── Lifecycle
```

---

# 32. Suggested Android Stack

```text
Kotlin
Jetpack Compose
MVVM
Clean Architecture
Coroutines
StateFlow
Room
DataStore
Hilt
Navigation Compose
WorkManager
```

Only use libraries where they solve an actual problem.

The application should not become a showcase of dependencies.

---

# 33. Core Modules

Potential package structure:

```text
com.focus.app

├── core
│   ├── time
│   ├── permissions
│   ├── notifications
│   └── platform
│
├── data
│   ├── database
│   ├── datastore
│   └── repository
│
├── domain
│   ├── goal
│   ├── session
│   └── blocking
│
├── feature
│   ├── onboarding
│   ├── today
│   ├── goals
│   ├── session
│   ├── history
│   └── settings
│
└── blocking
    ├── detector
    ├── controller
    └── ui
```

---

# 34. Visual Design System

The visual direction is intentionally close to the clean YPT aesthetic.

### Primary palette

Use approximately:

```text
Background
#F7F5EF

Primary text
#18212B

Secondary text
#66707A

Primary dark
#263640

Soft accent
#DDE5EC
```

Then allow a **single small accent color** for the goal/can state.

Avoid rainbow dashboards.

### Design characteristics

- Rounded rectangles
- Thin borders
- Large whitespace
- Simple icons
- Soft shadows
- Minimal gradients
- Mostly two-tone UI
- Large readable timer
- No visual clutter

---

# 35. Beer Can Visual System

The beer can should become the visual identity of a session.

But it shouldn't turn the application into a drinking-themed app.

Think of it as:

> **A can trapped in ice, gradually becoming free as time passes.**

States:

```text
100% remaining
████████████████
Heavy ice


75%
████████████░░░░
Ice partially melted


50%
████████░░░░░░░░
Can visible


25%
████░░░░░░░░░░░░
Almost free


0%
░░░░░░░░░░░░░░░░
Can completely free
```

The actual animation should be smooth rather than discrete.

---

# 36. Minimum Mode Visual

Normal:

```text
MATHEMATICS

      00:42:17

      [can]
```

Minimum:

```text
                   

                42:17

                   
```

Almost everything disappears.

Brightness is visually reduced.

The user should feel like the application itself has **gone quiet**.

---

# 37. MVP

The first release should contain only:

### Required

- Onboarding
- Goal creation
- Daily duration
- Today's goals
- Goal-specific blocked apps
- Session countdown
- App blocking
- Session completion
- End session
- Persistent timer
- Minimum mode
- Melting ice/can animation
- Basic history

### Explicitly NOT MVP

- Login
- Cloud sync
- Social features
- Leaderboards
- Friends
- AI
- Streaks
- Achievements
- Subscription
- Ads
- Backend
- Cross-platform support

---

# 38. V2 Possibilities

Only after the core experience feels excellent:

- Automatic daily goal generation
- Weekly statistics
- Goal scheduling
- Flexible rest days
- Notification reminders
- More immersive themes
- Better session animations
- Backup/export
- Optional cloud sync
- Widgets

But feature additions should be resisted.

The product's value comes partly from what it refuses to do.

---

# 39. Engineering Quality Requirements

The GitHub repository should demonstrate:

### Testing

Unit tests for:

- Remaining-time calculation
- Goal completion
- Session lifecycle
- Interrupted sessions
- Daily goal generation
- Blocking decisions

Integration tests for:

- Room
- Session persistence
- Goal persistence

UI tests for:

- Creating goal
- Starting session
- Completing session
- Ending session

### CI

GitHub Actions:

```text
push
 ↓
compile
 ↓
lint
 ↓
unit tests
 ↓
instrumentation/UI tests
```

### Documentation

README should contain:

```text
What is this?
Why does it exist?
Screenshots
Architecture
How app blocking works
Timer architecture
Permissions
Testing
Known Android limitations
```

---

# 40. The Product in One Sentence

If someone asks what you built:

> **"It's a minimalist Android app where you decide how much time you want to give something every day, and it blocks the apps you choose while that time counts down."**

That's it.

The user experience is tiny.

The implementation underneath is substantial enough to demonstrate:

- Android platform knowledge
- Compose
- state management
- persistence
- lifecycle handling
- background behavior
- system permissions
- app monitoring
- reliable timers
- animations
- testing
- product/design judgment

And that's the part I think makes this project valuable for your GitHub: **the product doesn't look complicated, but the repository shows that you know how to build a real Android application properly.**