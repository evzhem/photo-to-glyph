#!/usr/bin/env bash
# Standalone M0 only. This is not a Gradle wrapper or a substitute for M1 CI.
set -euo pipefail
ROOT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"
CACHE="$ROOT/.cache/m0-toolchain"
if [[ -z "${JAVA_HOME:-}" ]] && [[ -x "$CACHE/java/jdk4py/java-runtime/bin/java" ]]; then
    export JAVA_HOME="$CACHE/java/jdk4py/java-runtime"
fi
if [[ -n "${JAVA_HOME:-}" ]]; then
    export PATH="$JAVA_HOME/bin:$PATH"
fi
if [[ -z "${KOTLINC:-}" ]]; then
    if command -v kotlinc >/dev/null 2>&1; then
        KOTLINC="$(command -v kotlinc)"
    else
        KOTLINC="$CACHE/kotlin/package/bin/kotlinc"
    fi
fi
if ! command -v java >/dev/null 2>&1 || [[ ! -x "$KOTLINC" ]]; then
    echo "Java 17+ / Kotlin compiler not found. Run python3 tools/bootstrap-m0.py" >&2
    echo "or set JAVA_HOME and KOTLINC to existing installations." >&2
    exit 1
fi
WORK="$ROOT/.m0-work"
BUILD="$ROOT/build/m0"
mkdir -p "$BUILD" "$WORK"
printf '%s\n' '=== M0 standalone verification (no Gradle; M1 not started) ==='
java -version
"$KOTLINC" -version
printf '\n%s\n' '=== Verify original synthetic fixtures ==='
python3 tools/generate_m0_fixtures.py --check
printf '\n%s\n' '=== Compile production core + CLI (JVM target 17) ==='
mapfile -d '' SOURCES < <(find core/src/main/kotlin cli/src/main/kotlin -name '*.kt' -print0 | sort -z)
"$KOTLINC" "${SOURCES[@]}" -jvm-target 17 -Werror -include-runtime -d "$BUILD/photoglyph-m0.jar"
echo 'PASS production compilation'
printf '\n%s\n' '=== Compile tests against production JAR ==='
# M1 adds JUnit adapters; this archival M0 command retains its original 21-case suite.
TESTS=(core/src/test/kotlin/photoglyph/core/M0Tests.kt)
"$KOTLINC" "${TESTS[@]}" -jvm-target 17 -Werror -classpath "$BUILD/photoglyph-m0.jar" -d "$BUILD/photoglyph-m0-tests.jar"
echo 'PASS test compilation'
printf '\n%s\n' '=== Real core tests ==='
JAVA_FLAGS=(-Djava.awt.headless=true -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8)
java "${JAVA_FLAGS[@]}" -cp "$BUILD/photoglyph-m0.jar:$BUILD/photoglyph-m0-tests.jar" photoglyph.core.M0TestsKt
printf '\n%s\n' '=== CLI smoke: five original synthetic glyphs ==='
java "${JAVA_FLAGS[@]}" -cp "$BUILD/photoglyph-m0.jar" photoglyph.cli.MainKt smoke tools/fixtures/synthetic/m0 "$WORK"
printf '\n%s\n' '=== Repository whitespace check ==='
git diff --check
git diff --cached --check
echo 'PASS git diff --check + git diff --cached --check'
echo 'M0 CHECK PASSED (synthetic evidence only; Inkscape/device not verified)'
