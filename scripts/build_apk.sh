#!/usr/bin/env bash
# Builds a signed, installable debug APK for EloMP without Android Studio,
# Gradle's Android plugin, or any Google-hosted SDK download (dl.google.com
# is unreachable from this build environment). It uses:
#   - Ubuntu's own rebuilt-from-AOSP-source `android-sdk-platform-23` /
#     `android-sdk-build-tools` packages (aapt, android.jar) via apt
#   - `zipalign` / `apksigner` (also from apt)
#   - Google's R8 compiler jar (bundles the D8 dexer), fetched from the
#     public, non-Google-Play "r8-releases" GCS bucket that the R8 project
#     itself publishes releases to (storage.googleapis.com, unrelated to
#     the blocked dl.google.com host)
#
# Usage: scripts/build_apk.sh
# Output: dist/EloMP-debug.apk

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
APP_DIR="$ROOT_DIR/app/src/main"
BUILD_DIR="$ROOT_DIR/build"
DIST_DIR="$ROOT_DIR/dist"
CACHE_DIR="$ROOT_DIR/.buildcache"

ANDROID_JAR="/usr/lib/android-sdk/platforms/android-23/android.jar"
R8_VERSION="8.10.40"
R8_URL="https://storage.googleapis.com/r8-releases/raw/${R8_VERSION}/r8.jar"
R8_JAR="$CACHE_DIR/r8-${R8_VERSION}.jar"

MIN_API=21
PACKAGE_NAME="com.elomp.app"
APK_NAME="EloMP-debug.apk"

mkdir -p "$CACHE_DIR" "$DIST_DIR"

if [ ! -f "$ANDROID_JAR" ]; then
  echo "error: $ANDROID_JAR not found. Install it with:" >&2
  echo "  sudo apt-get install -y android-sdk-platform-23 android-sdk-build-tools apksigner zipalign" >&2
  exit 1
fi

for tool in aapt zipalign apksigner keytool javac java; do
  command -v "$tool" >/dev/null 2>&1 || { echo "error: required tool '$tool' not found on PATH" >&2; exit 1; }
done

if [ ! -f "$R8_JAR" ]; then
  echo "Fetching R8/D8 ${R8_VERSION} (dexer)..."
  curl -fsSL --max-time 300 -o "$R8_JAR.tmp" "$R8_URL"
  mv "$R8_JAR.tmp" "$R8_JAR"
fi

echo "Cleaning previous build output..."
rm -rf "$BUILD_DIR"
mkdir -p "$BUILD_DIR/gen" "$BUILD_DIR/obj" "$BUILD_DIR/dex"

echo "Generating R.java from resources (aapt)..."
aapt package -f -m \
  -J "$BUILD_DIR/gen" \
  -M "$APP_DIR/AndroidManifest.xml" \
  -S "$APP_DIR/res" \
  -I "$ANDROID_JAR"

echo "Compiling Java sources (javac)..."
JAVA_SOURCES_LIST="$BUILD_DIR/sources.txt"
find "$APP_DIR/java" "$BUILD_DIR/gen" -name "*.java" > "$JAVA_SOURCES_LIST"
javac -nowarn -encoding UTF-8 \
  -cp "$ANDROID_JAR" \
  -d "$BUILD_DIR/obj" \
  "@$JAVA_SOURCES_LIST"

echo "Dexing (D8, min-api $MIN_API)..."
find "$BUILD_DIR/obj" -name "*.class" > "$BUILD_DIR/classes.txt"
java -cp "$R8_JAR" com.android.tools.r8.D8 \
  --min-api "$MIN_API" \
  --output "$BUILD_DIR/dex" \
  --lib "$ANDROID_JAR" \
  "@$BUILD_DIR/classes.txt"

echo "Packaging resources + manifest (aapt)..."
UNSIGNED_APK="$BUILD_DIR/app-unsigned.apk"
aapt package -f \
  -M "$APP_DIR/AndroidManifest.xml" \
  -S "$APP_DIR/res" \
  -I "$ANDROID_JAR" \
  -F "$UNSIGNED_APK"

echo "Adding classes.dex to APK..."
WITH_DEX_APK="$BUILD_DIR/app-with-dex.apk"
cp "$UNSIGNED_APK" "$WITH_DEX_APK"
( cd "$BUILD_DIR/dex" && zip -q "$WITH_DEX_APK" classes.dex )

echo "Zipaligning..."
ALIGNED_APK="$BUILD_DIR/app-aligned.apk"
zipalign -f 4 "$WITH_DEX_APK" "$ALIGNED_APK"

echo "Signing (debug key)..."
DEBUG_KEYSTORE="$CACHE_DIR/debug.keystore"
if [ ! -f "$DEBUG_KEYSTORE" ]; then
  keytool -genkeypair -v -keystore "$DEBUG_KEYSTORE" -storepass android -keypass android \
    -alias androiddebugkey -keyalg RSA -keysize 2048 -validity 10000 \
    -dname "CN=EloMP Debug,O=EloMP,C=US" >/dev/null
fi

SIGNED_APK="$DIST_DIR/$APK_NAME"
apksigner sign --ks "$DEBUG_KEYSTORE" --ks-pass pass:android --key-pass pass:android \
  --out "$SIGNED_APK" "$ALIGNED_APK"

apksigner verify "$SIGNED_APK"
echo
echo "Built $SIGNED_APK"
aapt dump badging "$SIGNED_APK" | grep -E "^package|sdkVersion|targetSdkVersion|application-label"
