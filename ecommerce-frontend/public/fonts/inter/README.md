# Inter — login typography

One unmodified Latin variable WOFF2 file, downloaded from Google's official font
service. Requested normal weights 400–700; no italic files or additional script
subsets are loaded. The font asset is 48,256 bytes.

- Font: https://fonts.google.com/specimen/Inter
- Source CSS: https://fonts.googleapis.com/css2?family=Inter:wght@400..700&display=swap
- Binary: https://fonts.gstatic.com/s/inter/v20/UcC73FwrK3iLTeHuS_nVMrMxCp50SjIa1ZL7.woff2
- License: `OFL.txt`, from https://raw.githubusercontent.com/rsms/inter/master/LICENSE.txt

The SIL Open Font License permits redistribution with the copyright notice and
license included. This folder retains that license alongside the font.

`src/styles/login-font.css` declares the face with `font-display: swap`. The login
component alone selects the Inter font stack. Unsupported glyphs use system
fallbacks. There are no runtime Google Fonts requests or app-wide font assignments.
