# Overload UI and ADB review

## Scope

Ink-black and acid-yellow visual refresh using the existing Compose Material 3 stack: shared color roles, typography, shapes, navigation, dashboard, training, nutrition, history, and settings. No new app dependencies. Barlow Condensed Bold is bundled locally under its SIL Open Font License, included in `shared/src/commonMain/composeResources/files/licenses/BarlowCondensed-OFL.txt`.

The dashboard leads with a training hero and a native Canvas weight-plate illustration, followed by a dated activity strip and compact nutrition metrics. Nutrition uses colored macro tiles and clearly labels remaining or over-target calories. Training remains accessible after a completed workout; food logging has a direct entry point. History has a useful empty state.

## Corrections

- Wait for persisted rest-timer restoration before saving timer state. Previously the initial null state could overwrite the saved deadline during startup.
- Restore the active-workout screen-awake flag during startup, even before opening Train.
- Extending an expired timer clears its expired state, and invalid starting durations cannot produce a zero-length denominator.
- Clamp progress for zero, negative, or non-finite goals. Zero-carb targets previously fed `0 / 0` into a layout width.
- Group dashboard workouts by local calendar date, matching the food diary and calendar instead of UTC midnight.
- Confirm program removal and enlarge its touch target.
- Preserve the selected main tab across activity recreation.
- Replace the dashboard menu's misleading photo and weight shortcuts (which only opened Food) with a clearly labeled Food entry point.

## ADB setup

Device: Samsung Galaxy S24 Ultra, SM-S928U1. Existing app: `dev.dwm.liftlog`, version 0.10.0 (15).

The locally built preview uses `dev.dwm.liftlog.preview` and the launcher label **Overload Preview**. It has a separate database and does not replace the release. Updating the existing release requires its original signing key; do not uninstall it to work around a signature mismatch.

```sh
ANDROID_HOME=/home/dwm/.local/share/android-sdk mise exec java@temurin-17 -- ./gradlew :shared:desktopTest :androidApp:assembleDebug -Poverload.preview=true
/home/dwm/.local/share/android-sdk/platform-tools/adb install -r androidApp/build/outputs/apk/debug/androidApp-debug.apk
```

## Follow-up candidates

1. **Data protection:** replace destructive migration fallback with complete migration coverage and test upgrades from older schemas. The current database builders can erase data when no migration path exists.
2. **Input validation:** validate macro percentages as a combined total and report invalid calorie/weight values next to their fields. Rendering is guarded in this pass; settings currently accept inconsistent target combinations.
3. **Day rollover:** refresh the current date when returning to the app after midnight. Dashboard and nutrition currently remember their initial date.
4. **Logging shortcuts:** implement real deep links into food capture and weight entry, then reuse them in the widget and dashboard.
5. **Performance:** profile a repeatable workout interaction on a release build. The installed app's historical graphics counters showed 19.91% janky frames across 61,747 frames; that accumulated sample is not a controlled before/after benchmark.

Existing AI meal entry, barcode scanning, program generation, cardio, recovery, exports, and cloud sync are retained. Live AI, cloud-sync, and Wear OS integration need their own connected-service/device tests.

## Verification — September 11, 2026

- `:shared:desktopTest`: 31 tests passed, including progress edge cases and timer restoration/extension.
- `:androidApp:assembleDebug -Poverload.preview=true`: passed; installed and updated successfully over ADB.
- `:androidApp:lintDebug`: zero errors, 36 warnings (mostly existing dependency-version notices; also backup extraction rules, an obsolete SDK check, and version-catalog suggestions).
- Phone: visited all five main tabs; added a sample StrongLifts program; cancelled its removal; completed one sample set and workout; confirmed the session and PR in History.
- Phone: started a 90-second rest, switched to Food, force-stopped the preview, and relaunched. The remaining countdown resumed, and `dumpsys window` confirmed the preview held the screen awake.
- Phone: saved 100% protein / 0% fat targets and opened Food with zero carbohydrate/fat goals successfully. Restored the preview to 30% protein / 30% fat afterward.
- Phone: logged a 600-kcal sample breakfast through Quick Add; verified the dashboard showed 1,400 kcal remaining and the entered macros after updating the preview APK.
- Final preview process log inspection found no matching runtime exception or ANR. This is a smoke test, not a claim that every runtime path is bug-free.
- Final screenshots inspected: `build/screenshots/today.png`, `train.png`, `food.png`, `history.png`, `settings.png`.

The preview contains sample test data. The original app's data and credentials were not copied or modified. Changes are local and uncommitted; nothing has been pushed or released.

## Second visual pass

Replaced the restrained first design with athletic display typography, a custom illustrated training hero shared by Dashboard and Train, a high-contrast action strip, a seven-day calendar strip, colored macro tiles, and square navigation highlights. Existing timer and data fixes remain in place.

Rebuilt Android and reran all 31 desktop tests successfully. Installed over the preview without removing its data. Inspected the actual phone renders in `build/screenshots/today-v2.png`, `train-v2.png`, `food-v2.png`, and `fuel-v2.png`. Verified the dashboard training action, tab navigation, and program-removal confirmation; no matching runtime error was found in the inspected preview process log.
