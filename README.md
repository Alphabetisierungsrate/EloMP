# EloMP

An Android app for tracking Elo-style rankings for a boardgame group.

- Add/remove players.
- Create a separate ranking (leaderboard) per game.
- Record a result by picking who played and who won — supports "1 out of n
  won" and "n out of m won" results, not just 1-on-1.
- Every recorded result is timestamped, and can be deleted later (ratings are
  recalculated automatically).
- A combined "Total Ranking" screen aggregates each player's rating across
  every game they've played.

See [ELO_METHOD.md](ELO_METHOD.md) for how the multiplayer rating math works
and the sources it's based on.

## Installing the APK on your phone

Grab `dist/EloMP-debug.apk` from this branch/repo, download it on your phone
(e.g. from the GitHub file view), then open the downloaded file. Android will
ask you to allow installing from that source (Files app / browser) the first
time — allow it, then install. The app needs Android 5.0 (API 21) or newer.

This is a self-signed debug build (not from the Play Store), so Android will
label it as being from an "unknown source" — that's expected for
side-loading your own app.

## Building it yourself

This project intentionally does **not** use Gradle's Android plugin or the
official Google-hosted Android SDK — see the note at the top of
`scripts/build_apk.sh` for why. Instead it's built with:

- `aapt`, `zipalign`, `apksigner`, and the `android-23` platform jar, all
  from Ubuntu/Debian's own `android-sdk-*` apt packages (rebuilt from AOSP
  source, no Google download required)
- Google's R8 compiler (which bundles the D8 dexer), fetched from R8's
  public release bucket (`storage.googleapis.com/r8-releases`)

```
sudo apt-get install -y android-sdk-platform-23 android-sdk-build-tools apksigner zipalign
scripts/build_apk.sh
```

Output: `dist/EloMP-debug.apk`.

If you'd rather build with Android Studio / Gradle on a normal machine with
full access to Google's servers, the sources under `app/src/main` are a
standard Android project layout (`AndroidManifest.xml`, `java/`, `res/`) —
you would just need to wrap them in a `build.gradle` with
`compileSdk`/`targetSdk` 23+ (or newer) and drop this repo's manual build
script.

## Project layout

```
app/src/main/AndroidManifest.xml
app/src/main/java/com/elomp/app/
  data/    – SQLite storage (players, rankings, match entries)
  elo/     – EloEngine: the rating math, framework-independent
  ui/      – Activities (Players, Games/Rankings, Ranking detail,
             Record Result, Total Ranking)
app/src/main/res/       – layouts, strings
scripts/build_apk.sh    – the manual build pipeline described above
dist/EloMP-debug.apk    – the built, signed APK
```
