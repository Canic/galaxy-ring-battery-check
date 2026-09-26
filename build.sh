#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
sdk=.tools/android/usr/lib/android-sdk
build=ring-battery-test/build
rm -rf "$build/classes" "$build/gen"
mkdir -p "$build/classes" "$build/gen"
sed -e 's/<manifest /<manifest package="dev.galaxydiagnostic.ringbattery" /' \
    -e 's/<application /<uses-sdk android:minSdkVersion="31" android:targetSdkVersion="35"\/><application /' \
  ring-battery-test/AndroidManifest.xml > "$build/AndroidManifest.xml"
LD_LIBRARY_PATH="$PWD/.tools/android/usr/lib/x86_64-linux-gnu/android:/usr/lib/x86_64-linux-gnu/android" \
  "$sdk/build-tools/debian/aapt" package -f -m \
  -M "$build/AndroidManifest.xml" \
  -S ring-battery-test/res \
  -I "$sdk/platforms/android-23/android.jar" \
  -J "$build/gen" -F "$build/unsigned.apk"
java -cp .tools/android/usr/share/java/eclipse-jdt-core.jar \
  org.eclipse.jdt.internal.compiler.batch.Main -proc:none -1.8 \
  -classpath "$sdk/platforms/android-23/android.jar" \
  -d "$build/classes" \
  ring-battery-test/src/dev/galaxydiagnostic/ringbattery/MainActivity.java \
  "$build/gen/dev/galaxydiagnostic/ringbattery/R.java"
java -cp "$sdk/build-tools/debian/lib/dx.jar" \
  com.android.dx.command.Main --dex --output="$build/classes.dex" "$build/classes"
(cd "$build" && zip -q -u unsigned.apk classes.dex)
zipalign -f 4 "$build/unsigned.apk" "$build/aligned.apk"
if [ ! -f "$build/debug.keystore" ]; then
  keytool -genkeypair -keystore "$build/debug.keystore" \
    -storepass android -keypass android -alias ringbattery \
    -dname 'CN=Ring Battery Test' -keyalg RSA -keysize 2048 -validity 3650
fi
java -cp .tools/android/usr/share/java/apksigner.jar \
  com.android.apksigner.ApkSignerTool sign \
  --ks "$build/debug.keystore" --ks-pass pass:android \
  --key-pass pass:android --ks-key-alias ringbattery \
  --out "$build/ring-battery-test.apk" "$build/aligned.apk"
echo "$build/ring-battery-test.apk"
