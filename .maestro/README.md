# Maestro flows

E2E flows land starting Phase 4 (`E2E-1`, `E2E-2` — see `TESTCASES.md`).
Phase 5 added `E2E-3`/`E2E-4`/`E2E-5`.

Phase 0 has no screen content yet, so there is nothing to test here — this
directory exists so the CI `e2e` job and `./gradlew verify` wiring have a
stable path to point at from day one.

`scripts/e2e5-dates.js` is a `runScript` helper used by `E2E-5-week-start-day.yaml`.
The emulator has no root, so there's no way to pin the device's system clock to
the fixed calendar dates `TESTCASES.md` uses as an example — this script computes
"this week's Monday" and "the Sunday right before it" relative to whatever day
the flow actually runs on, which reproduces the same relative structure
(Monday belongs to the current week, Sunday belongs to last week) regardless
of the real date.

Run locally (much faster than CI — see the root `README.md` for how to get a
working arm64 emulator on Apple Silicon):

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
maestro test .maestro/
```
