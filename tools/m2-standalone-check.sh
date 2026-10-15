#!/usr/bin/env bash
# Additional real M2 checks, not a substitute for mandatory Gradle/Android CI.
set -euo pipefail
ROOT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"
./tools/m1-standalone-check.sh
if ! command -v java >/dev/null 2>&1; then
    export JAVA_HOME="$ROOT/.cache/m0-toolchain/java/jdk4py/java-runtime"
    export PATH="$JAVA_HOME/bin:$PATH"
fi
KOTLINC="${KOTLINC:-$(command -v kotlinc || true)}"
if [[ -z "$KOTLINC" ]]; then KOTLINC="$ROOT/.cache/m0-toolchain/kotlin/package/bin/kotlinc"; fi
python3 tools/generate_m2_fixtures.py --check
printf '\n%s\n' '=== Compile shared M2 core and API cases against actual production JAR ==='
"$KOTLINC" core/src/test/kotlin/photoglyph/core/M2Checks.kt \
    core/src/test/kotlin/photoglyph/core/PictureContoursChecks.kt \
    cli/src/test/kotlin/photoglyph/cli/sandbox/PreparationApiChecks.kt tools/M2StandaloneTests.kt \
    -jvm-target 17 -Werror -cp build/sandbox/photoglyph-sandbox.jar -d build/sandbox/m2-tests.jar
printf '\n%s\n' '=== Execute actual M2 preparation functions and HTTP recovery ==='
java -Djava.awt.headless=true -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8 \
    -Dphotoglyph.projectRoot="$ROOT" -cp build/sandbox/photoglyph-sandbox.jar:build/sandbox/m2-tests.jar photoglyph.tools.M2StandaloneTestsKt
node --check cli/src/main/resources/sandbox/prepare.js
git diff --check
git diff --cached --check
echo 'PASS M2 standalone checks; NOT an Android/Gradle success'
