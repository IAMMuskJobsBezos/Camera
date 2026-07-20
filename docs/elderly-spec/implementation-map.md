# Implementation Map

How the spec lands in the existing codebase
(`app/src/main/kotlin/org/fossify/camera/`). Pointer map, not a task plan.
Product decisions live in [decisions.md](decisions.md).

## Build & verify loop (decision #11)

Build → install on emulator → screenshot each specced state (photo idle,
video idle, timer countdown, recording, flash/timer toggled) → compare
side-by-side with `wireframes/` → adjust → repeat. Screenshots go in
`_progress/` (untracked). Also re-check at 200% system font size before
calling the screen done. Don't declare it done from code alone.

## Top bar (Flash, Timer)

| Spec | Code | Notes |
| --- | --- | --- |
| Two labeled toggles, always both present | `layout_top.xml` (two weight-1 `MaterialButton`s), `activity_main.xml` `top_options` | The old 4-button icon row + expanding `MaterialButtonToggleGroup`s (`layout_flash.xml`, `layout_timer.xml`) are gone entirely — deleted, not just unused |
| Flash: 2-state, greys out (never hides) when no hardware | `Constants.kt` (`FLASH_OFF`/`FLASH_ON` only), `Config.flashlightState` (migrates legacy Auto/Always-on to Off), `CameraXPreview.toggleFlashlight()`/`setFlashlightState()`, `MainActivity.setFlashAvailable()` → `setButtonGreyedOut()` | Decision #4, #12 |
| Timer: 3-state cycle, greys out (never hides) in video mode | `TimerMode.kt` (`OFF`/`TIMER_3`/`TIMER_10`, `next()`, `fromPrefValue()` migrates legacy 5s→3s), `MainActivity.onInitPhotoMode()`/`onInitVideoMode()` → `setButtonGreyedOut()` | Decision #5, #12 |

## Removed controls

| Spec | Code | Notes |
| --- | --- | --- |
| No resolution button (decision #6) | Deleted `layout_button.xml`; `MyPreview.showChangeResolution()`, `CameraXPreview.showChangeResolution()`/`toggleResolutions()`, `CameraXPreviewListener.showImageSizes()` all removed | Resolution silently stays at index 0, which `ImageQualityManager`/`VideoQualityManager` already treat as the best default — no new locking logic needed |
| No settings button (decision #6) | `layout_top.xml`, `MainActivity.launchSettings()` removed | `SettingsActivity` stays in code, unreachable |

## Mode tabs

| Spec | Code | Notes |
| --- | --- | --- |
| Photo tab left, Video right; icon-then-text per tab, tap only | `activity_main.xml` `camera_mode_tab`, new `tab_item_photo.xml`/`tab_item_video.xml` (custom `TabItem android:layout=`), `color/tab_text_color.xml`, `MainActivity` (`PHOTO_MODE_INDEX = 0`, `VIDEO_MODE_INDEX = 1`) | Decision #14. The fling/swipe gesture handler was deleted outright (decision #8) — no dead code left behind |

## Action row

| Spec | Code | Notes |
| --- | --- | --- |
| Flip left, Photos right, bigger circles, row shifted down | `activity_main.xml` (`flip_button`/`last_photo_video_preview` constraints swapped), `dimens.xml` (`action_circle_size` 60dp, `shutter_size` 96dp, `flip_icon_padding`), `MainActivity`'s window-insets listener (smaller runtime bottom margin) | Decision #15, #17 |
| Photos: live circular thumbnail, opens last shot in swipeable viewer | `activity_main.xml` `photos_icon` id, `MainActivity.loadLastTakenMedia()` (Glide `circleCrop()` + `placeholder`/`error` fallback to `ic_photos_vector`), called from `setupPreviewImage()` and `onMediaSaved()` | Decision #16 |
| Shutter states (photo ring / video red dot / red stop, no label) | `activity_main.xml` `shutter`, `ic_shutter_animated`/`ic_video_rec_animated` drawables (unchanged animated-vector states) | The `shutter_label` "Stop" TextView was added, then deleted (decision #18) |
| Recording state | `video_rec_curr_timer` TextView, `MainActivity.onVideoRecordingStarted()`/`onVideoRecordingStopped()` | Bigger elapsed time; Flip/Photos disabled+faded; top bar (Flash/Timer) stays visible throughout (decision #12) |

## Preview boxing (decision #13)

| Spec | Code | Notes |
| --- | --- | --- |
| Preview always boxed between the fixed bars, never edge-to-edge | `CameraXPreview.bindCameraUseCases()` | Always calls `listener.adjustPreviewView(true)`, always `ScaleType.FILL_CENTER`, always the plain `bindToLifecycle(previewUseCase, captureUseCase)` path. The old `isFullSize`/`ViewPort`/`UseCaseGroup` branch (edge-to-edge WYSIWYG crop mode) was deleted along with its now-unused `windowMetricsCalculator` field |

## Timer countdown

| Spec | Existing code | Change |
| --- | --- | --- |
| Big centered countdown | `timer_text` `TextSwitcher` (`timer_text.xml`), countdown logic + sounds in `MainActivity`/`MediaSoundHelper` | Scale up to ≥ 64sp; behavior unchanged (tap shutter cancels) |

## Untouched in v1

`CameraXPreview`/`CameraXInitializer` capture pipeline (aside from the
boxing change above), `ImageSaver`/`PhotoProcessor`/`MediaOutputHelper`
saving, `CameraErrorHandler`, `HardwareShutterReceiver`, pinch-zoom/tap-focus
(`PinchToZoomOnScaleGestureListener`, `FocusCircleView`), capture-intent
handling, `SettingsActivity` internals, lint baselines conventions.

## Android permissions

None added — the redesign is UI-only (repo `CLAUDE.md` rule).
