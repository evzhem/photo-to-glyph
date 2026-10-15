plugins {
    base
}

val verifyFixtures by tasks.registering(Exec::class) {
    group = "verification"
    description = "Check the provenance and deterministic bytes of our original synthetic fixtures."
    workingDir(rootProject.projectDir)
    commandLine("python3", "tools/generate_m0_fixtures.py", "--check")
}

tasks.named("check") {
    dependsOn(verifyFixtures)
}
