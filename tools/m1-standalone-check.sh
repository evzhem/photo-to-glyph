#!/usr/bin/env bash
# Additional real checks when Gradle downloads are blocked. Never claims an Android build.
set -euo pipefail
ROOT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"
./tools/build-sandbox.sh
JAVA="${JAVA_HOME:+$JAVA_HOME/bin/}java"
KOTLINC="${KOTLINC:-$(command -v kotlinc || true)}"
if [[ -z "$KOTLINC" ]]; then KOTLINC="$ROOT/.cache/m0-toolchain/kotlin/package/bin/kotlinc"; fi
if ! command -v "$JAVA" >/dev/null 2>&1; then
    export JAVA_HOME="$ROOT/.cache/m0-toolchain/java/jdk4py/java-runtime"
    export PATH="$JAVA_HOME/bin:$PATH"
    JAVA="$JAVA_HOME/bin/java"
fi
mkdir -p build/sandbox
python3 tools/generate_m0_fixtures.py --check
printf '\n%s\n' '=== Compile shared real test cases against the production JAR ==='
"$KOTLINC" core/src/test/kotlin/photoglyph/core/M0Tests.kt \
    core/src/test/kotlin/photoglyph/core/CoreSourcePolicyChecks.kt \
    cli/src/test/kotlin/photoglyph/cli/sandbox/SandboxApiChecks.kt tools/M1StandaloneTests.kt \
    -jvm-target 17 -Werror -classpath build/sandbox/photoglyph-sandbox.jar -d build/sandbox/real-tests.jar
printf '\n%s\n' '=== Execute unchanged M0 + source policy + HTTP/core recovery checks ==='
"$JAVA" -Djava.awt.headless=true -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8 \
    -Dphotoglyph.projectRoot="$ROOT" \
    -cp build/sandbox/photoglyph-sandbox.jar:build/sandbox/real-tests.jar photoglyph.tools.M1StandaloneTestsKt
printf '\n%s\n' '=== CLI smoke using the same production JAR ==='
"$JAVA" -Djava.awt.headless=true -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 \
    -cp build/sandbox/photoglyph-sandbox.jar photoglyph.cli.MainKt smoke tools/fixtures/synthetic/m0 build/sandbox/smoke
printf '\n%s\n' '=== Browser-adapter syntax and diff checks ==='
node --check cli/src/main/resources/sandbox/app.js
git diff --check
git diff --cached --check
echo 'PASS standalone diagnostic checks; NOT the mandatory Gradle/Android build'
