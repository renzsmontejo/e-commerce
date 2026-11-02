# Batch B.1 — login typography refinement

## Result

The login component now uses Inter with system fallbacks. Its typography is more
consistent: natural heading spacing, lighter labels, readable body text and captions,
and sentence-case introductory text. The welcome heading keeps the same wording
and purple accent. Component layout, breakpoints, controls and behavior are unchanged.

One 48,256-byte Latin variable WOFF2 font is hosted locally. The normal 400–700
range covers the requested 400, 500, 600 and 650 weights without multiple font files.
`font-display: swap` keeps text visible during loading. The source and included
open-font license are documented in `public/fonts/inter/README.md`.

The face is declared in `src/styles/login-font.css` and imported by the stylesheet
bundle. Font declarations alone do not change typography: only the login component
selects Inter. Browser checks confirmed the body, shared header and registration
still use their existing Arial stack. No runtime third-party font requests are needed.

## Hierarchy

| Element | Desktop/tablet | Narrow mobile | Weight |
| --- | --- | --- | --- |
| Welcome heading | 40–44px | About 28px at 320/375px | 650 |
| Login heading | 26–28px | 22px at 320; about 24px at 375 | 600 |
| Body/description | 15px | 15px | 400 |
| Labels | 14px | 14px | 500 |
| Inputs | 15px | 15px | 400 |
| Placeholders | 14px | 14px | 400 |
| Sign-in button | 15px | 15px | 600 |
| Sign-up row | 14px | 14px | 400, link 600 |
| Validation/loading text | 13px | 13px | 400 |
| Captions/eyebrows | 12px | 12px | 400–500 |

Headings use normal letter spacing with 1.16/1.25 line heights. Body text uses 1.6;
labels and captions use 1.5. The login heading has 12px before its description, and
the description has 28px before the fields. Existing colors, focus outlines,
control boundaries, minimum touch targets and font-size responsiveness are retained.

## Source files changed in this batch

Modified:
- `src/app/features/auth/pages/login/login.css` — scoped font stack, hierarchy and
  typography-related spacing
- `src/styles.css` — imports the font declaration only

Added:
- `src/styles/login-font.css` — local @font-face, normal weights 400–700, swap
- `public/fonts/inter/inter-latin-variable.woff2`
- `public/fonts/inter/OFL.txt`
- `public/fonts/inter/README.md`

Review artifacts added here: this report, `typography-checks.json`, desktop/mobile
screenshots and a narrow-screen validation screenshot.

No TypeScript, templates, auth logic, validation rules, layout structure, routing,
backend, database, shared form rules or application-wide font settings were changed.
This batch does not continue authentication implementation work.

## Verification

- Angular production build passed within the existing component stylesheet budget.
- All 25 existing frontend tests passed.
- Prettier and `git diff --check` passed for this change.
- Browser screenshots inspected at 320, 375, 768, 1024 and 1440px.
- At every width, the local Inter font reported loaded, computed sizes/weights
  matched the hierarchy, and there was no horizontal overflow.
- Mobile still shows the form first; desktop preserves its two-column presentation.
- Validation text remains 13px, readable at 320px, with first-invalid-field focus.
- Body/header/registration font scope was verified separately.

`typography-checks.json` records the browser's computed style measurements. The
screenshots are `login-1440.png`, `login-375.png`, `login-320.png`, and
`mobile-validation.png`.
