# Decisions

Resolved 2026-07-19 with Emmett (interactive Q&A). Numbering is per-app;
Clock's decisions are referenced as "Clock #n" where a rule is inherited.

| # | Question | Decision |
| --- | --- | --- |
| 1 | Distribution | **Replace the UI in this fork** — no mode toggle, no flavor. Same as Clock #1. |
| 2 | Wireframe images | Kept **untracked** (`.gitignore`); spec text is what's committed. Only the four `Screenshot 2026-07-19 …` files in `~/Downloads/camera-wireframes` are source material — the two `.jpeg` files there are not. Same as Clock #4. |
| 3 | Design system | Stay on **Fossify Commons / Fossify Camera look**, scaled up and labeled. The camera screen keeps its black/dark-overlay look in both system themes. Same as Clock #3. |
| 4 | Flash modes | **Off/On only**, tap toggles. Auto and always-on-flashlight modes removed, in photo and video mode alike. Label carries the state ("Flash OFF"/"Flash ON"). |
| 5 | Timer modes | **Off / 3s / 10s, tap cycles** through them on the single button (5s removed). Not the wireframe's literal two states — Emmett kept 10s for arm's-length/propped-up shots. No expanding option row. |
| 6 | Resolution & Settings buttons | **Both removed from the screen.** Resolution locks to the best default (highest available 4:3 photo resolution; 1080p video, falling back to highest available). `SettingsActivity` stays in code but is unreachable — same pattern as Clock's unreachable Settings (Clock #16 note). Revisit in a later Settings pass. |
| 7 | Photos button | **Superseded 2026-07-20 (#16).** |
| 8 | Mode switching | **Tap the labeled tab only; swipe disabled.** Same reasoning as Clock #9. |
| 9 | Recording state | Shutter becomes a **red stop button**; big elapsed time above it; **Flip and Photos disabled/hidden** while recording. The "Stop" text label under the shutter was tried and then removed (#18) — the red stop-shaped icon reads as "stop" on its own. |
| 10 | Preview gestures | **Keep pinch-to-zoom and tap-to-focus.** Standard, invisible until used, recoverable. |
| 11 | Build process | Iterative: build → emulator screenshot → compare to wireframes → adjust, per state. Same as Clock #15; see [implementation-map.md](implementation-map.md). |
| 12 | Top bar persistence | **Flash and Timer are always visible in the same fixed-height row, in both Photo and Video mode.** When a control doesn't apply — Timer in Video mode, Flash with no flash hardware — it's greyed out (disabled, ~40% alpha) rather than hidden. Fixes a layout-jump bug where hiding a button changed the top bar's height and shifted the preview underneath it. |
| 13 | Preview boxing | **The live preview always sits boxed between the top bar and the bottom tabs/shutter row** (`FILL_CENTER`, no edge-to-edge/ViewPort mode) — it never resizes or repositions between Photo and Video mode, since both bars are now always the same height (#12). |
| 14 | Photo/Video tab order & style | **Photo tab on the left, Video on the right** (the initial build had these backwards — copied the pre-redesign app's tab order instead of the wireframe's). Tabs use a custom horizontal icon-then-text layout (not the default stacked icon-over-text), which also shortens the row and gives the preview more vertical room. |
| 15 | Flip/Photos order & shape | **Flip on the left of the shutter, Photos on the right** (the initial build also had these backwards, same root cause as #14 — matches the wireframe: Flip · Shutter · Photos). The circular background now wraps only the icon (not the text label under it), for a "badge over caption" look instead of a pill enclosing both. |
| 16 | Photos button content | **Superseded #7: shows a live circular thumbnail of the last shot** (photo, or first frame of the last video — Glide handles both automatically), not a static icon. Falls back to the static `ic_photos_vector` glyph as `placeholder`/`error` while loading or if nothing's been shot yet, so the circle is never blank. |
| 17 | Action-row touch targets | Flip/Photos circles and the shutter are all **bigger** (60dp / 96dp, up from 48dp / 88dp), and the whole row sits **closer to the bottom edge** (reduced runtime bottom margin) to make room for the larger circles without crowding the tabs above. Flip's icon also gets a touch more internal padding than Photos'. |
| 18 | Recording label | **Removed** — see #9. |

## Still open (later)

- Settings screen redesign (post-v1; unreachable until then).
- Front-camera "flash" (screen-brightness fill): out of scope — Flash button
  just greys out where no flash unit exists (#12).
- Dark-overlay contrast values to be pixel-checked on device during
  implementation (≥ 7:1 target from the shared principles).
- Light theme and real-hardware flash haven't been checked — only emulator
  verification (dark, no physical flash unit reported for the back camera)
  has been done so far.
