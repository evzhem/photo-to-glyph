#!/usr/bin/env python3
"""Install isolated, checksum-pinned M0 compiler/runtime tools, not product dependencies.

Only Linux x86_64 is supported by this optional convenience bootstrap. Other
platforms should provide their own Java 17+ and Kotlin 2.1.21 via JAVA_HOME/KOTLINC.
The sandbox blocks GitHub's release-asset redirect host. The official JetBrains
npm compiler distribution and a PyPI-packaged OpenJDK runtime are reachable.
No npm/pip dependencies are installed, imported by core, or packaged in the app.
"""

import base64
import hashlib
import io
from pathlib import Path
import platform
import tarfile
import urllib.request
import zipfile

ROOT = Path(__file__).resolve().parent.parent
CACHE = ROOT / ".cache" / "m0-toolchain"
KOTLIN_URL = "https://registry.npmjs.org/kotlin-compiler/-/kotlin-compiler-2.1.21.tgz"
KOTLIN_SHA512 = "PvXicARwvQ6O0KBTnJnXOnxSjWbNnRBeKnucK7MM4aOVF3pMHY5llC6Ibh+tAORSuq81Gn/X7WmYkOKkp0/23g=="
JAVA_URL = (
    "https://files.pythonhosted.org/packages/a2/61/"
    "f3b5936908ff6de66c61aef69bf1074cb468fb883f5a0bb9e15e67a6b484/"
    "jdk4py-21.0.8.2-py3-none-manylinux_2_17_x86_64.whl"
)
JAVA_SHA256 = "85addfcb57c7051dad6145b9f816fc519337e9a0c705ef01edc9dc7818ee0356"


def download(url, name, algorithm, digest):
    path = CACHE / "downloads" / name
    path.parent.mkdir(parents=True, exist_ok=True)
    if not path.exists():
        print(f"Downloading {name}", flush=True)
        with urllib.request.urlopen(url, timeout=120) as response:
            data = response.read()
        actual = hashlib.new(algorithm, data).digest()
        if actual != digest:
            raise RuntimeError(f"Checksum mismatch: {name}")
        path.write_bytes(data)
    data = path.read_bytes()
    if hashlib.new(algorithm, data).digest() != digest:
        raise RuntimeError(f"Cached checksum mismatch: {name}; remove the bad file")
    print(f"PASS pinned {algorithm}: {name}", flush=True)
    return data


def safe_destination(root, name):
    destination = (root / name).resolve()
    if not destination.is_relative_to(root.resolve()):
        raise RuntimeError(f"Unsafe archive path: {name}")
    return destination


def main():
    if platform.system() != "Linux" or platform.machine() != "x86_64":
        raise SystemExit("Provide Java 17+ and Kotlin 2.1.21 manually on this platform.")
    kotlin = download(KOTLIN_URL, "kotlin-compiler-2.1.21.tgz", "sha512", base64.b64decode(KOTLIN_SHA512))
    java = download(JAVA_URL, "jdk4py-21.0.8.2.whl", "sha256", bytes.fromhex(JAVA_SHA256))
    with tarfile.open(fileobj=io.BytesIO(kotlin), mode="r:gz") as archive:
        for member in archive.getmembers():
            if member.issym() or member.islnk():
                raise RuntimeError(f"Unexpected compiler archive link: {member.name}")
            destination = safe_destination(CACHE / "kotlin", member.name)
            if member.isdir():
                destination.mkdir(parents=True, exist_ok=True)
            elif member.isfile():
                destination.parent.mkdir(parents=True, exist_ok=True)
                destination.write_bytes(archive.extractfile(member).read())
                destination.chmod(member.mode & 0o777)
    with zipfile.ZipFile(io.BytesIO(java)) as archive:
        for member in archive.infolist():
            destination = safe_destination(CACHE / "java", member.filename)
            if member.is_dir():
                destination.mkdir(parents=True, exist_ok=True)
            else:
                destination.parent.mkdir(parents=True, exist_ok=True)
                destination.write_bytes(archive.read(member))
                mode = (member.external_attr >> 16) & 0o777
                destination.chmod(mode or 0o644)
    java_home = CACHE / "java" / "jdk4py" / "java-runtime"
    (java_home / "bin" / "java").chmod(0o755)
    print(f"JAVA_HOME={java_home}")
    print(f"KOTLINC={CACHE / 'kotlin' / 'package' / 'bin' / 'kotlinc'}")
    print("Toolchain ready; run ./tools/m0-check.sh")


if __name__ == "__main__":
    main()
