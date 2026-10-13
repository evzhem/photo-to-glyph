#!/usr/bin/env bash
# Real standalone compiler for the diagnostic tool in the network-restricted sandbox.
# NOT Gradle, NOT an Android build, and NOT a replacement for required M1 verification.
set -euo pipefail
ROOT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"
CACHE="$ROOT/.cache/m0-toolchain"
if [[ -z "${JAVA_HOME:-}" && -x "$CACHE/java/jdk4py/java-runtime/bin/java" ]]; then
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
    echo 'Install Java 17+ / Kotlin 2.1.21, or run python3 tools/bootstrap-m0.py' >&2
    exit 1
fi
mkdir -p build/sandbox
printf '%s\n' '=== Compile the actual Kotlin core + diagnostic CLI (not Android/Gradle) ==='
"$KOTLINC" -version
mapfile -d '' SOURCES < <(find core/src/main/kotlin cli/src/main/kotlin -name '*.kt' -print0 | sort -z)
"$KOTLINC" "${SOURCES[@]}" -jvm-target 17 -Werror -include-runtime -d build/sandbox/photoglyph-sandbox.jar
python3 - <<'PY'
from pathlib import Path
from zipfile import ZipFile, ZIP_DEFLATED
resources = Path('cli/src/main/resources')
with ZipFile('build/sandbox/photoglyph-sandbox.jar', 'a', compression=ZIP_DEFLATED) as archive:
    for resource in sorted(resources.rglob('*')):
        if resource.is_file():
            archive.write(resource, resource.relative_to(resources).as_posix())
print('PASS embedded sandbox resources (HTML/CSS/JS and ru/en strings.xml)')
PY
echo 'PASS diagnostic production compilation'
