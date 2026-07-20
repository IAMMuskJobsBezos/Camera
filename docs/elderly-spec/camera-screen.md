# Camera Screen

Wireframes: [`camera-main-a`](wireframes/camera-main-a.png),
[`camera-main-b-timer-on`](wireframes/camera-main-b-timer-on.png),
[`camera-main-c`](wireframes/camera-main-c.png) (photo mode),
[`video-mode-states`](wireframes/video-mode-states.png) (video mode, two
panels: toggles off / Flash ON + 3s Timer).

## Layout (top to bottom) — every section is a fixed size, always present

1. **Top bar** (dark strip, fixed height): two labeled toggles, always both
   visible in both Photo and Video mode.
   - Left: **Flash** — icon + "Flash OFF" / "Flash ON".
   - Right: **Timer** — icon + "Timer OFF" / "3s Timer" / "10s Timer".
   - Whichever doesn't apply (Timer in Video mode; Flash with no flash
     hardware) is **greyed out and disabled, not hidden** (decision #12) —
     the row's height never changes between modes.
2. **Live preview** fills the box between the top bar and the bottom
   section — never edge-to-edge, and never resized by mode switches
   (decision #13), since both bars around it are now fixed height.
3. **Bottom section**, two rows, both always present:
   - **Mode tabs**: "📷 Photo" (left) then "🎥 Video" (right), icon-then-word
     side by side per tab (decision #14). The active mode is **underlined
     and white**; the inactive one mid-grey, no underline.
   - **Action row**: **Flip** (left, circular-arrows icon in a circle,
     "Flip" label below it) · **Shutter** (center, biggest control on
     screen) · **Photos** (right, live thumbnail in a circle, "Photos"
     label below it) — decision #15.

## Controls

### Flash (top left)

- Two states, tap toggles: **Flash OFF ⇄ Flash ON** (decision #4). Auto and
  always-on/flashlight modes are removed.
- Photo mode: ON = fire flash with the shot. Video mode: ON = torch stays
  lit while on this screen/recording (existing video flash behavior).
- Icon: flash bolt, crossed out when OFF — but the word is the primary
  signal.
- No flash unit on the current camera (e.g. most front cameras): button
  stays in place, greyed out and disabled (decision #12) — never hidden.

### Timer (top right)

- Three states, tap cycles: **Timer OFF → 3s Timer → 10s Timer → Timer
  OFF** (decision #5). The 5s option is removed. One button; the label
  always names the current state — no expanding option row.
- Applies to photo mode only; greyed out and disabled in video mode
  (decision #12) — stays in place, doesn't disappear.

### Mode tabs (Photo / Video)

- Tap only — swipe-to-switch is removed (decision #8).
- **Photo tab is on the left, Video on the right** (decision #14) — matches
  the wireframe.
- Photo is the app's start mode (as today, launch-intent cases aside).
- When another app asks for just a photo or just a video (capture intent),
  the irrelevant tab is hidden (as today) — this is the one place a tab is
  still allowed to disappear, since a third-party capture intent is a
  different, single-purpose screen, not a mode switch the user makes.

### Flip

- **Sits to the left of the shutter** (decision #15).
- Toggles front/back camera. Greyed out and disabled if the device only has
  one camera (decision #12's same treatment, extended here) — stays in
  place rather than disappearing. Disabled while recording, as before.

### Shutter (center)

- Photo mode: white ring, hollow center. Tap → shutter animation + captured.
- Video mode: white ring with a **red dot** center = "this records". Tap →
  recording state (below).
- With a timer set, tap starts the countdown instead (below).
- Bigger than the original design pass (decision #17) and sits closer to
  the bottom edge, freeing headroom above for the bigger circles without
  crowding the tabs.

### Photos

- **Sits to the right of the shutter** (decision #15). Circular **live
  thumbnail** of the last shot — a photo, or the first frame of the last
  video (decision #16, supersedes the original "static icon" call) — with
  a static placeholder icon shown while it loads or before anything's been
  shot yet, so the circle is never blank.
- Tap opens the **last captured photo/video in the gallery's swipeable
  one-item viewer** (the existing view-last-media intent): lands on the most
  recent shot, swipe from there. Not the grid.
- Hidden during capture intents — as today.

## States

### Timer countdown (photo mode, timer set)

Big countdown numerals centered over the preview (existing `TextSwitcher`,
scaled up: ≥ 64sp), counting 3…2…1 or 10…9…, with the existing tick sound.
Tapping the shutter during countdown cancels it (as today). Then the shot
fires.

### Recording (video mode)

Per decision #9:

- Shutter becomes a clearly **red stop button** (no text label — tried and
  removed per decision #18; the red stop-shaped icon is unambiguous on its
  own).
- **Elapsed time** in big white text (≥ 30sp) above the shutter, `MM:SS`.
- **Flip and Photos are disabled/hidden** while recording; the mode tabs
  are inert (as today). The top bar (Flash/Timer) stays visible throughout,
  per decision #12.
- Tap Stop → saved, back to video-idle state.

### Capture-intent launches (third-party apps)

Same screen, same controls, minus whatever is already hidden today
(Photos button, the other mode's tab). Confirm/retake flow unchanged.

## Unchanged behaviors

Pinch-to-zoom, tap-to-focus (focus circle), volume/hardware shutter keys,
media sounds, save pipeline, orientation-rotating icons — all as today.
