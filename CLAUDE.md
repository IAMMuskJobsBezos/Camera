# Camera — project rules for Claude

Fossify Camera fork being redesigned for elderly users. Spec lives in
`docs/elderly-spec/` — read its README before touching UI code. Fullest
sibling reference: `~/StudioProjects/Clock/docs/elderly-spec/`.

## Rules (apply to this and the sibling Fossify forks: Clock, Phone, Contacts, Messages)

- **Stay on the Fossify Commons design system.** Reuse Commons theming,
  colors, and components; the app must keep looking like a Fossify app (and
  like Fossify Camera specifically — dark viewfinder surface included) —
  just bigger and simpler per the spec. Don't invent a new visual language.
- **Follow standard Fossify conventions**: Kotlin, existing package layout
  (`activities/`, `helpers/`, `implementations/`, …), match surrounding code
  style, keep lint baselines updated the way the repo already does.
- **Permissions**: UI-only work must not add Android permissions. If a change
  genuinely needs a new permission, stop and ask first.
- **Build/iterate loop**: when implementing spec screens, build and run on an
  emulator, take screenshots, compare against `docs/elderly-spec/wireframes/`
  (untracked — ask Emmett if missing), and iterate until the screen matches.
  Don't declare a screen done from code alone.
- **Asking questions**: batch decisions into interactive multiple-choice
  questions (AskUserQuestion) instead of long lists the user must type
  answers to.
- Wireframe images stay untracked (`.gitignore`d); specs are text-only in git.
