# Stopwatch FTC

High-precision Android stopwatch with lap timing, fastest/slowest lap comparison, light and dark
themes, and 11 languages.

## Build and run

**Prerequisites:** [Android Studio](https://developer.android.com/studio) and JDK 17 or newer.

1. Open Android Studio, choose **Open**, and select this directory.
2. Run the app on an emulator or a physical device.

No extra setup, keys, or config files are needed for a debug build.

From the command line:

```bash
./gradlew installDebug
```

## Tests

Everything runs on the JVM — no emulator required.

```bash
./gradlew test
```

Covered: the timing engine (including reboot recovery), duration formatting, lap highlighting, and
Compose UI behaviour under Robolectric. `StopwatchScreenshotTest` writes a reference image to
`app/src/test/screenshots/` via Roborazzi.

## Release build

Signing is wired to environment variables so no key material lives in the repository:

| Variable         | Meaning                                        |
| ---------------- | ---------------------------------------------- |
| `KEYSTORE_PATH`  | Keystore location, defaults to `upload-key.jks` |
| `STORE_PASSWORD` | Keystore password                              |
| `KEY_ALIAS`      | Key alias, defaults to `upload`                |
| `KEY_PASSWORD`   | Key password                                   |

With `STORE_PASSWORD` unset, `assembleRelease` still produces an unsigned build.

## How it works

`Stopwatch` derives the elapsed time from `SystemClock.elapsedRealtime()` on demand rather than
counting up, so the reading cannot drift and is immune to the system clock changing. State is
snapshotted to DataStore on every action and every 10 seconds while running, which lets a
measurement survive the process being killed.

Because `elapsedRealtime()` resets on reboot, each snapshot also records the offset between
wall-clock time and uptime. If that offset has moved on restore, the stopwatch freezes at its last
checkpoint instead of reporting a nonsensical duration.

The UI samples the stopwatch through `withFrameMillis`, so it updates once per rendered frame and
stops entirely when the app is not on screen.

The launch window holds until the saved state has been read back, so the first frame already shows
the real measurement instead of a zero that jumps a moment later. Its background is the same colour
the app draws, which leaves nothing visible between the two.

## Colour

Colours come from the wallpaper on Android 12 and later, with the brand palette as the fallback
below it and whenever dynamic colour is switched off. Fastest and slowest laps stay green and red
either way: they carry meaning, and Material has no colour role for "success".

## Window sizes

Every layout decision is made from the current window rather than the device, so a resized desktop
window, a folding screen and a split-screen pane all get what fits them:

| Window | Layout |
| --- | --- |
| Narrower than 600dp | Dial above the lap list |
| Taller than it is wide | Dial above the lap list, whatever the width |
| 600dp or wider, and roughly square or wider | Dial and lap list side by side |

The dial is sized from whichever axis runs out first and the readout scales with it, so the lap list
always keeps usable space and the time never wraps. Content stops stretching at 1280dp.

## Layout

```
app/src/main/java/com/stopwatch/ftc/
├── MainActivity.kt
├── data/StopwatchStore.kt      persistence (DataStore + kotlinx.serialization)
├── domain/                     timing engine, no Compose or Android UI dependencies
└── ui/                         Compose screen, ViewModel, theme
```
