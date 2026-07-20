# Design Principles (Camera)

The shared elderly-UX rules are specced once, in
`~/StudioProjects/Clock/docs/elderly-spec/design-principles.md`, and apply
here unchanged: ≥ 18sp text everywhere, ≥ 56dp touch targets, icon **and**
word on every button, no hidden gestures for anything essential, plain
sentence-case language, color never the only signal, stay on Fossify Commons.

What follows is only what's camera-specific.

## Dark surface, not themed surface

The camera screen keeps its black background and dark overlay bars — a
viewfinder is dark in every camera app, and Commons' light theme would put
white bars around a live preview. All text and icons on this screen are
**white** (active) or **mid-grey** (inactive/secondary) on the dark overlay;
contrast must still meet the ≥ 7:1 body-text bar. Light/dark system theme
does not change this screen.

## Words carry state

The two top toggles don't just show a state icon — they *say* the state:
"Flash OFF" / "Flash ON", "Timer OFF" / "3s Timer" / "10s Timer". When a
toggle is tapped, the label changes; the icon change is secondary. Never an
icon-only state indicator.

## One screen, no chrome

No app bar, no overflow menu, no settings icon, no resolution button
(decision #6). The full control set is: Flash, Timer, Photo/Video tabs,
Flip, Shutter, Photos. Six controls, all always visible, none nested, and
none of them ever disappear when they stop applying — Timer in video mode,
Flash with no flash hardware, Flip with only one camera all just grey out
in place (decision #12). A control popping in and out of existence would
also shift the fixed-size bars around it, which is exactly what decision
#12/#13 rule out: the live preview always occupies the same box on screen.

## Big targets over a live preview

- Shutter: ≥ 80dp circle, centered, bottom.
- Flip / Photos: ≥ 56dp icon buttons with labels beneath.
- Top toggles and mode tabs: full label + icon is the tap target, ≥ 56dp
  tall.
- Controls sit on the top/bottom overlay bars, never floating over the
  live preview area itself.

## Gestures

Pinch-to-zoom and tap-to-focus on the preview stay (decision #10) — both
are additive; ignoring them costs nothing. Swiping to switch Photo/Video is
**disabled** (decision #8); the labeled tabs are the only mode switch, same
as Clock's decision #9.
