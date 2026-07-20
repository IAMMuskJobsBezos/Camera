# Elderly-Friendly Camera — Spec Overview

Redesign of Fossify Camera for elderly users, based on hand-drawn wireframes
(local copies in `wireframes/`, untracked; originals in
`~/Downloads/camera-wireframes` — only the four
`Screenshot 2026-07-19 …` files are source material; the two `.jpeg` files
there are not).

Sibling project to the Clock/Phone/Contacts/Messages elderly redesigns. The
fullest reference spec is Clock's, at
`~/StudioProjects/Clock/docs/elderly-spec/` — shared principles live there and
are not repeated here.

## Goal

Camera is a one-screen app and stays one: take a photo, record a video, flip
the camera, look at what you shot. Every control gets an icon **and** a word,
every state is spelled out ("Flash OFF", "3s Timer"), and everything that
isn't one of those jobs (resolution picker, settings) leaves the screen —
while staying on the Fossify Commons design system so it still looks like
Fossify Camera.

**This fork's UI is replaced outright** (no "simple mode" toggle, no separate
flavor) — same distribution decision as the sibling apps (decision #1 in
[decisions.md](decisions.md)).

## Spec files

| File | Covers |
| --- | --- |
| [design-principles.md](design-principles.md) | Camera-specific rules; pointer to the shared Clock principles |
| [camera-screen.md](camera-screen.md) | The one main screen: layout, every control, photo/video/countdown/recording states |
| [implementation-map.md](implementation-map.md) | How the spec maps onto the existing code, plus the build/verify loop |
| [decisions.md](decisions.md) | All resolved product decisions |

## Scope

**V1:** the main camera screen in all its states (photo, video, timer
countdown, recording), including behavior when launched by another app's
photo/video capture intent (same screen, same rules).

**Later (unchanged in v1):** Settings screen (left in code but unreachable —
decision #6), widgets/none, storage & saving pipeline, error handling
(`CameraErrorHandler`), hardware shutter support — all untouched.

## Status

**Implemented and verified on-device, most recently 2026-07-20.** All
main-screen states — photo idle, video idle, timer cycling (Off/3s/10s),
flash toggle (Off/On), flip, recording (red stop button, big elapsed time,
Flip/Photos hidden), and the Photos button opening the last shot in a
swipeable single-item viewer — match this spec and were screenshot-compared
against `wireframes/` on an emulator. Also verified: 200% system font size
wraps without clipping or overlap; the top bar and bottom tabs/action row
never resize or reorder between Photo and Video mode (decisions #12–#17).

Two implementation bugs were found and fixed after the first pass, both the
same mistake — copying the *original* Fossify Camera's left/right control
order instead of the wireframe's: the Photo/Video tabs and the Flip/Photos
pair were each backwards. Worth double-checking for this pattern if more
controls get added later.

Not yet checked: light theme (the camera screen intentionally stays dark in
both themes per [design-principles.md](design-principles.md), so this is
low-risk); real hardware flash (only emulator-reported flash availability was
exercised — oddly, this AVD's front camera reports having one and the back
camera doesn't). Settings screen redesign remains out of scope (decision #6).
