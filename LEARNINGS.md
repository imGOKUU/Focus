# Learnings

A running log of things worth remembering as we build this. Newest entries at the bottom of each section.

## Architecture / Platform decisions

- **KMP vs. native Android**: chose native Android. The app's hardest, most distinctive feature — detecting the foreground app and blocking it — has no shared implementation across platforms. Android uses `UsageStatsManager`/`AccessibilityService`; iOS uses the entirely different Screen Time `DeviceActivity`/`ManagedSettings` APIs, gated behind a special Apple entitlement. Since that logic can't be shared anyway, KMP would only add build/tooling complexity without reducing the amount of platform-specific code we still have to write. Revisit only if/when an iOS version is actually planned.

## Jetpack Compose

- `Canvas` in Compose gives you a `DrawScope` receiver inside its lambda — functions like `drawCircle(...)` and `drawPath(...)` are members of `DrawScope`, not top-level functions, so they don't need their own import once you're inside a `Canvas { ... }` block.
- Flat illustrations (like the landing page's sun/mountains) can be built cheaply with layered `Path()` polygons drawn back-to-front (furthest/lightest first, closest/darkest last) rather than image assets — keeps the app free of drawables for simple geometric shapes and makes the illustration themeable via plain `Color` values.
- `Modifier.weight(1f)` inside a `Column` is the idiomatic way to push content apart (e.g. pinning a button to the bottom while text sits at the top) without hardcoding spacer heights for different screen sizes.
- `TextStyle.copy(...)` lets you tweak one property (e.g. `fontWeight`, `fontSize`) of a shared `MaterialTheme.typography` style for a single screen without editing the shared type scale — useful when a design mockup wants slightly heavier/larger type in one specific spot than what's defined globally.
- **Hoisted state, not hardcoded data**: screen composables (`WelcomeScreen`, `GoalConfigScreen`) take their lists (`goals`, `blockableApps`) and current values as parameters with safe defaults (`emptyList()`, no-op lambdas), rather than owning the data themselves. This keeps them pure functions of their input — trivially previewable with fake data now, and swappable for real ViewModel/Room-backed state later with zero changes to the composable itself.
- **Wheel/reel picker pattern** (`GoalConfigScreen`'s hour/minute picker): built from a plain `LazyColumn` plus `rememberSnapFlingBehavior(listState)` so it snaps to whichever item is nearest the center after a fling/scroll, instead of stopping wherever momentum happens to run out.
  - To find *which* item is currently centered, read `listState.layoutInfo.visibleItemsInfo` and pick the one whose midpoint is closest to the viewport's midpoint (`derivedStateOf { ... minByOrNull { abs(...) } }`). This is more robust than computing it from `firstVisibleItemIndex`/`firstVisibleItemScrollOffset`, which gets fiddly once `contentPadding` is involved.
  - To make item N appear in the *middle* of a 3-row-tall wheel (not the top), add `contentPadding = PaddingValues(vertical = itemHeight)` so the list can scroll one extra row past each end, and set the initial scroll position to `N - 1` (not `N`) so item N lands in the center row on first composition.
  - A single list of preset durations (e.g. "30m / 1h / 2h") can't represent an arbitrary custom value like `01:20`. Two independent wheels (hours × minutes) can — each wheel just reports its own centered value, and the pair combines into the total duration.