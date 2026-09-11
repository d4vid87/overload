# Overload

Personal, local-first fitness app: workout tracking + programs with auto-progression + nutrition with an adherence-neutral TDEE algorithm. One Kotlin Multiplatform codebase → Android (S24 Ultra), Wear OS, Linux/Windows desktop, with optional cloud sync through a Netlify function.

## Features

- **Workout logging** (Strong/Hevy-style): weight/reps/RPE, previous-session hints, rest timer, plate calculator, 400-exercise library (wger.de seed)
- **Programs** (Boostcamp-style): 5/3/1 BBB, nSuns LP, GZCLP, PPL templates; linear / double / percent-of-TM progression engines with AMRAP-keyed TM bumps
- **Nutrition**: Open Food Facts search + barcode scan, custom foods, per-meal daily log, macros
- **TDEE** (MacroFactor-style): EMA trend weight + intake → expenditure estimate + auto-adjusted calorie target
- **Cloud sync**: LWW row replication via Netlify Function + Netlify Blobs, single bearer token
- **Wear OS**: log sets + rest timer from the wrist (messages the phone app)
- **AI** (optional): natural-language food entry + workout suggestions via any OpenAI-compatible endpoint (local Ollama works)
- **Open Food Facts contribution**: unknown barcodes can be submitted upstream with your OFF account
- **History**: workout log + e1RM progress charts; full JSON export

## Build

Requires JDK 17 + Android SDK (set `sdk.dir` in `local.properties`).

```bash
./gradlew :androidApp:assembleDebug          # phone APK
./gradlew :wearApp:assembleDebug             # watch APK
./gradlew :desktopApp:packageDistributionForCurrentOS   # deb/msi
./gradlew testDebugUnitTest                  # unit tests (TDEE, progression, e1RM)
```

Tag `v*` → GitHub Actions builds release APK + deb + msi and attaches to the Release.

## Sync backend

`netlify/` deploys to a Netlify site (functions + blobs). Set env var `SYNC_TOKEN`, then in the app's **More** tab enter the site URL + token and hit Sync Now.

```bash
netlify deploy --prod
```
