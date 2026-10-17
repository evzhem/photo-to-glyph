# M3 — основа сегментации и неполной модели набора

Дата базового M3-среза: **2026-10-16**. Его статус — частичная core-основа, не OCR. На 2026-10-17 добавлены два отдельных follow-up среза: ручной crop/label builder и раздельный uppercase/lowercase picker; см. [`M3-manual-builder-report.md`](M3-manual-builder-report.md) и [`M3-letter-case-picker-report.md`](M3-letter-case-picker-report.md). Автоматическое распознавание, разделение курсивных букв и ручное редактирование штрихов всё ещё не реализованы.

## Последующее обновление — локальная библиотека, 2026-10-16

После первоначальной проверки M3 добавлены `LocalGlyphSetStore` (app-private JSON persistence) и read-only Compose-экран списка. В CI run `38849746285` прошли полный Gradle/Android build и четыре unit-теста store. Пользовательское создание/редактирование наборов, OCR, присвоение меток и `.gfont` остаются не реализованы; фактическая приёмка на Android-устройстве не выполнялась. Подробности и полный Gradle output: [`docs/M5-report.md`](M5-report.md).

## 1) Файлы и назначение

- `core/src/main/kotlin/photoglyph/core/img/InkComponents.kt` — `connectedInkComponents` выделяет детерминированные 8-связные компоненты переднего плана; `cropBinaryGrid` создаёт pixel-exact crop без мутации источника. Компонент — лишь кандидатный фрагмент чернил, не распознанная буква.
- `core/src/main/kotlin/photoglyph/core/PipelineParams.kt`, `core/src/main/kotlin/photoglyph/core/img/PreparedImage.kt` — `pageMinComponentAreaPx` позволяет сохранять мелкие точки/диакритические элементы по умолчанию (`1 px`) и задавать явно проверяемый порог.
- `core/src/main/kotlin/photoglyph/core/model/GlyphSet.kt` — сериализуемые `Stroke`, `Glyph`, `GlyphSet`; частичный набор валиден и не дополняется отсутствующими символами. Проверяются ключ/символ, метрики, уникальный порядок штрихов и открытые полилинии.
- `core/src/main/kotlin/photoglyph/core/model/PolylineSerializer.kt`, `GlyphSetJson.kt` — собственная сериализация геометрии и JSON codec поверх `kotlinx.serialization`.
- `gradle/libs.versions.toml`, корневой и `core/build.gradle.kts` — версия/alias `kotlinx.serialization` и plugin применены в core.
- `core/src/test/kotlin/photoglyph/core/{InkComponentsChecks,InkComponentsTest,GlyphSetChecks,GlyphSetRoundTripTest}.kt` — проверки вызывают production-функции; PNG для round-trip создаётся в памяти теста, пользовательские фотографии не используются.
- `tools/M2StandaloneTests.kt`, `tools/build-sandbox.sh`, `tools/m0-check.sh`, `tools/m2-standalone-check.sh` — standalone runner компилирует реальные общие проверки, но намеренно исключает сериализуемый model-пакет; Gradle/CI отдельно компилирует и тестирует его.
- `README.md`, `ROADMAP.md`, `CHANGELOG.md`, `AGENTS.md`, `docs/01-требования-mvp.md`, `docs/05-рисунок-по-контуру.md` — уточнены статус, ограничения, CI и правила продолжения.

## 2) Полный вывод проверок

Локальный запуск `./tools/m2-standalone-check.sh` завершился с кодом `0`: 21 M0/core, 3 source-policy, 8 sandbox API, 17 preparation, 3 InkComponents, 4 contour, 5 preparation API checks и CLI smoke 5/5. Он **не** проверяет `core/model` и Android/Gradle.

Полный Gradle/Android build, core/JUnit tests, lint и CLI smoke прошли в GitHub Actions run [38799609923](https://github.com/evzhem/photo-to-glyph/actions/runs/38799609923), commit `e58d36d63fc78885a145c4f05500a12e72d50e26`. Опубликованы `verification-evidence` (ID `12003872624`) и debug APK `photoglyph-m1-debug-apk` (ID `12003392740`, 10,901,829 bytes). Полный captured Gradle output приведён после standalone вывода.

### Полный вывод `./tools/m2-standalone-check.sh`

```text
=== Compile the diagnostic core + CLI subset (not Android/Gradle) ===
info: kotlinc-jvm 2.1.21 (JRE 21.0.8+9-LTS)
PASS embedded sandbox resources (HTML/CSS/JS and ru/en strings.xml)
PASS diagnostic production compilation
PASS fixture: tools/fixtures/synthetic/m0/1046.png
PASS fixture: tools/fixtures/synthetic/m0/1041.png
PASS fixture: tools/fixtures/synthetic/m0/1074.png
PASS fixture: tools/fixtures/synthetic/m0/1064.png
PASS fixture: tools/fixtures/synthetic/m0/1092.png
PASS fixture: tools/fixtures/synthetic/m0/manifest.json
PASS fixture: tools/fixtures/synthetic/golden/bar-20-input.png
PASS fixture: tools/fixtures/synthetic/golden/bar-20-expected.png

=== Compile shared real test cases against the production JAR ===

=== Execute unchanged M0 + source policy + HTTP/core recovery checks ===
PASS GrayscaleTest.bt601 -> toGrayscale
PASS GrayscaleTest.alphaOnWhite -> toGrayscale
PASS AdaptiveThresholdTest.localMeanAndOffset -> adaptiveThreshold
PASS AdaptiveThresholdTest.clippedBorderWindow -> adaptiveThreshold
PASS DespeckleTest.strict12pxBoundary -> removeSmallComponents
PASS DespeckleTest.eightConnectivity -> removeSmallComponents
PASS ZhangSuenTest.golden20px -> zhangSuen
PASS ZhangSuenTest.fixedPointAndBorder -> zhangSuen
PASS ZhangSuenTest.preserveLoop -> zhangSuen, skeletonToPolylines
PASS SkeletonToPolylinesTest.zheAtLeast3Open -> vectorize, skeletonToPolylines
PASS SkeletonToPolylinesTest.cornerAndDiagonal -> skeletonToPolylines
PASS SkeletonToPolylinesTest.cycleEdgeCoverage -> skeletonToPolylines
PASS SkeletonToPolylinesTest.pruneStrictlyBelow2px -> skeletonToPolylines
PASS SkeletonToPolylinesTest.dotAndEmpty -> skeletonToPolylines, strokesToSvg, requireStrokeOnlySvg
PASS RdpTest.handSpecifiedSimplification -> simplifyRdp
PASS RdpTest.zeroEpsilonAndShortPaths -> simplifyRdp
PASS SvgNoFillTest.fiveActualExports -> vectorize, strokesToSvg, requireStrokeOnlySvg
PASS SvgNoFillTest.rejectCorruptedExport -> strokesToSvg, requireStrokeOnlySvg
PASS SvgNoFillTest.rejectClosureAndUseDecimalPoint -> strokesToSvg, requireStrokeOnlySvg
PASS PipelineTest.deterministicAndImmutable -> vectorize
PASS PipelineTest.blankImage -> vectorize, strokesToSvg, requireStrokeOnlySvg
Tests: 21, passed: 21, failed: 0
PASS CoreSourcePolicyTest.actualProductionSourcesHaveNoAndroidImports -> findForbiddenAndroidImports
PASS CoreSourcePolicyTest.rejectsQualifiedWildcardAndAliasedAndroidImports -> findForbiddenAndroidImports
PASS CoreSourcePolicyTest.ignoresNestedCommentsAndStringExamples -> findForbiddenAndroidImports
PASS SandboxApiTest.fiveFixturesExecuteActualCore -> HTTP + vectorize, strokesToSvg, requireStrokeOnlySvg
PASS SandboxApiTest.parameterChangesReachActualCore -> HTTP + vectorize, strokesToSvg, requireStrokeOnlySvg
PASS SandboxApiTest.uploadedPngExecutesActualCore -> HTTP + vectorize, strokesToSvg, requireStrokeOnlySvg
PASS SandboxApiTest.uploadedJpegExecutesActualCore -> HTTP + vectorize, strokesToSvg, requireStrokeOnlySvg
PASS SandboxApiTest.invalidParametersReleaseTheCoreForRecovery -> HTTP + vectorize, strokesToSvg, requireStrokeOnlySvg
PASS SandboxApiTest.badImagesReleaseTheCoreForRecovery -> HTTP + vectorize, strokesToSvg, requireStrokeOnlySvg
PASS SandboxApiTest.oversizedInputsReleaseTheCoreForRecovery -> HTTP + vectorize, strokesToSvg, requireStrokeOnlySvg
PASS SandboxApiTest.routesAndLocalesPreserveAWorkingCore -> HTTP + vectorize, strokesToSvg, requireStrokeOnlySvg
Standalone real checks: 21 existing core + 3 source policy + 8 API; all passed
This is NOT a Gradle/Android result. The mandatory Gradle command is reported separately.

=== CLI smoke using the same production JAR ===
INPUT: original synthetic 300x300 PNGs, 6 px strokes; not real handwriting
TIMING: PNG decode + core + SVG serialization/validation; excludes JVM startup/output writes; 2 warmups + 5 measured runs
char | strokes | closed | graph cycles | split chains | cold ms | median ms | max ms
Ж | 8 | 0 | 0 | 0 | 165.436 | 20.733 | 165.436
Б | 3 | 0 | 1 | 1 | 25.273 | 8.722 | 25.273
в | 3 | 0 | 2 | 0 | 10.800 | 9.429 | 11.139
Ш | 3 | 0 | 0 | 0 | 21.099 | 11.899 | 21.099
ф | 5 | 0 | 2 | 0 | 10.297 | 12.253 | 13.950
PASS smoke: 5/5 nonempty glyphs, zero closed polylines, explicit no-fill strokes, within budget
Outputs: build/sandbox/smoke/<codepoint>.svg, intermediate PNGs, metrics.csv

=== Browser-adapter syntax and diff checks ===
PASS standalone diagnostic checks; NOT the mandatory Gradle/Android build
PASS fixture: tools/fixtures/synthetic/m2/grid-reference.png
PASS fixture: tools/fixtures/synthetic/m2/grid-yaw30-side-light.png
PASS fixture: tools/fixtures/synthetic/m2/manifest.json

=== Compile shared M2 core and API cases against actual production JAR ===

=== Execute actual M2 preparation functions and HTTP recovery ===
PASS M2Test.transparentResizeHasNoHalo -> resizeToMaxEdge
PASS M2Test.mirrorIsPixelExactAndNonmutating -> mirrorHorizontal
PASS M2Test.suggestedSizeDoesNotGuessA4 -> suggestedRectifiedSize
PASS M2Test.quarterTurnsArePixelExact -> rotateQuarterTurns
PASS M2Test.cropIsPixelExactAndRejectsOutside -> cropImage
PASS M2Test.resizeIdentityAndAverageAreIndependentExpectations -> resizeToMaxEdge
PASS M2Test.perspectiveMapsKnownCornersAndCenter -> mapPagePoint
PASS M2Test.identityPerspectivePreservesArgb -> rectifyPerspective
PASS M2Test.rectangularPerspectiveMatchesHandSpecifiedCrop -> rectifyPerspective
PASS M2Test.degenerateQuadsAndAllocationBoundsAreRejected -> PageQuad, rectifyPerspective
PASS M2Test.otsuHasHandDefinedBimodalThreshold -> otsuThreshold
PASS M2Test.pagePreparationPreservesSmallMarksByDefault -> prepareImage, adaptiveThreshold, removeSmallComponents
PASS M2Test.ambiguousBlankPageDoesNotInventDetection -> detectPageQuad
PASS M2Test.detectAnalyticYaw30SideLightCorners -> detectPageQuad
PASS M2Test.rectifyAnalyticPhotoAndAlignKnownGridProfiles -> prepareImage, whiteProjectionColumns, whiteProjectionRows
PASS M2Test.autoDetectedRectificationAlsoAlignsKnownGrid -> detectPageQuad, prepareImage, whiteProjectionColumns, whiteProjectionRows
PASS M2Test.gridSheetHasOwnLabelsMarksAndEscaping -> gridSheetSvg, cyrillicGridCharacters
M2 real core checks: 17, failures: 0
PASS InkComponentsTest.componentsAreEightConnectedAndReadingOrdered -> connectedInkComponents
PASS InkComponentsTest.componentCropIsPixelExactAndNonmutating -> cropBinaryGrid
PASS InkComponentsTest.blankPageHasNoInkComponents -> connectedInkComponents
Ink component production checks: 3, failures: 0
PASS PictureContoursTest.straightStepProducesThinBlackContourWithoutMutatingSource -> blackContourImage
PASS PictureContoursTest.uniformImageProducesNoContours -> blackContourImage
PASS PictureContoursTest.thresholdControlsLowContrastEdges -> blackContourImage, PipelineParams
PASS PictureContoursTest.thresholdMustMatchNormalizedSobelRange -> PipelineParams
Picture contour production checks: 4, failures: 0
PASS PreparationApiTest.realPageDetectionAndPreparation -> HTTP + detectPageQuad, prepareImage; cropImage/rotateQuarterTurns/gridSheetSvg as applicable
PASS PreparationApiTest.manualCornersAndUploadedRasterReachCore -> HTTP + detectPageQuad, prepareImage; cropImage/rotateQuarterTurns/gridSheetSvg as applicable
PASS PreparationApiTest.pixelExactRotationAndCrop -> HTTP + detectPageQuad, prepareImage; cropImage/rotateQuarterTurns/gridSheetSvg as applicable
PASS PreparationApiTest.badQuadAndParametersRecoverActualPreparation -> HTTP + detectPageQuad, prepareImage; cropImage/rotateQuarterTurns/gridSheetSvg as applicable
PASS PreparationApiTest.printableSheetAndStaticRoutesDoNotFakeAnOutline -> HTTP + detectPageQuad, prepareImage; cropImage/rotateQuarterTurns/gridSheetSvg as applicable
M2 API checks: 5, failures: 0
PASS M2 standalone checks; NOT an Android/Gradle success
```

### Полный вывод Gradle/Android CI

Ниже приведён полный verification output из check-run `116646545470` (workflow run `38799609923`); статус GitHub Actions — success.

```text
To honour the JVM settings for this build a single-use Daemon process will be forked. For more on this, please refer to https://docs.gradle.org/8.11.1/userguide/gradle_daemon.html#sec:disabling_the_daemon in the Gradle documentation.
Daemon will be stopped at the end of the build
> Task :assemble UP-TO-DATE
> Task :app:detekt
> Task :core:checkKotlinGradlePluginConfigurationErrors SKIPPED
> Task :app:loadKtlintReporters
> Task :app:runKtlintCheckOverAndroidTestDebugSourceSet NO-SOURCE
> Task :app:ktlintAndroidTestDebugSourceSetCheck SKIPPED
> Task :app:runKtlintCheckOverAndroidTestReleaseSourceSet NO-SOURCE
> Task :app:ktlintAndroidTestReleaseSourceSetCheck SKIPPED
> Task :app:runKtlintCheckOverAndroidTestSourceSet NO-SOURCE
> Task :app:ktlintAndroidTestSourceSetCheck SKIPPED
> Task :app:runKtlintCheckOverDebugSourceSet NO-SOURCE
> Task :app:ktlintDebugSourceSetCheck SKIPPED
> Task :app:runKtlintCheckOverKotlinScripts
> Task :app:ktlintKotlinScriptCheck
> Task :app:runKtlintCheckOverMainSourceSet
> Task :app:ktlintMainSourceSetCheck
> Task :app:runKtlintCheckOverReleaseSourceSet NO-SOURCE
> Task :app:ktlintReleaseSourceSetCheck SKIPPED
> Task :app:runKtlintCheckOverTestDebugSourceSet NO-SOURCE
> Task :app:ktlintTestDebugSourceSetCheck SKIPPED
> Task :app:runKtlintCheckOverTestFixturesDebugSourceSet NO-SOURCE
> Task :app:ktlintTestFixturesDebugSourceSetCheck SKIPPED
> Task :app:runKtlintCheckOverTestFixturesReleaseSourceSet NO-SOURCE
> Task :app:ktlintTestFixturesReleaseSourceSetCheck SKIPPED
> Task :app:runKtlintCheckOverTestFixturesSourceSet NO-SOURCE
> Task :app:ktlintTestFixturesSourceSetCheck SKIPPED
> Task :app:runKtlintCheckOverTestReleaseSourceSet NO-SOURCE
> Task :app:ktlintTestReleaseSourceSetCheck SKIPPED
> Task :app:runKtlintCheckOverTestSourceSet NO-SOURCE
> Task :app:ktlintTestSourceSetCheck SKIPPED
> Task :core:processResources NO-SOURCE
> Task :app:checkKotlinGradlePluginConfigurationErrors SKIPPED
> Task :app:preBuild UP-TO-DATE
> Task :app:preDebugBuild UP-TO-DATE
> Task :core:compileKotlin
> Task :app:checkDebugAarMetadata
> Task :app:generateDebugResValues
> Task :app:mapDebugSourceSetPaths
> Task :app:generateDebugResources
> Task :app:mergeDebugResources
> Task :app:packageDebugResources
> Task :app:parseDebugLocalResources
> Task :app:createDebugCompatibleScreenManifests
> Task :app:extractDeepLinksDebug
> Task :app:processDebugMainManifest
> Task :app:processDebugManifest
> Task :app:processDebugManifestForPackage
> Task :app:processDebugResources
> Task :app:javaPreCompileDebug
> Task :app:preDebugAndroidTestBuild SKIPPED
> Task :app:generateDebugAndroidTestResValues
> Task :app:extractProguardFiles
> Task :app:preDebugUnitTestBuild UP-TO-DATE
> Task :app:javaPreCompileDebugUnitTest
> Task :app:buildKotlinToolingMetadata
> Task :app:preReleaseBuild UP-TO-DATE
> Task :app:checkReleaseAarMetadata
> Task :app:generateReleaseResValues
> Task :app:mapReleaseSourceSetPaths
> Task :app:generateReleaseResources
> Task :app:mergeReleaseResources
> Task :app:packageReleaseResources
> Task :app:parseReleaseLocalResources
> Task :app:createReleaseCompatibleScreenManifests
> Task :app:extractDeepLinksRelease
> Task :app:processReleaseMainManifest
> Task :app:processReleaseManifest
> Task :app:processReleaseManifestForPackage
> Task :app:processReleaseResources
> Task :app:javaPreCompileRelease
> Task :app:preReleaseUnitTestBuild UP-TO-DATE
> Task :app:javaPreCompileReleaseUnitTest
> Task :cli:detekt
> Task :cli:loadKtlintReporters
> Task :cli:runKtlintCheckOverKotlinScripts
> Task :cli:ktlintKotlinScriptCheck
> Task :cli:runKtlintCheckOverMainSourceSet
> Task :cli:ktlintMainSourceSetCheck
> Task :cli:runKtlintCheckOverTestSourceSet
> Task :core:compileJava NO-SOURCE
> Task :core:classes UP-TO-DATE
> Task :cli:ktlintTestSourceSetCheck
> Task :core:jar
> Task :app:compileDebugKotlin
> Task :app:compileDebugJavaWithJavac NO-SOURCE
> Task :app:bundleDebugClassesToCompileJar
> Task :app:compileReleaseKotlin
> Task :app:generateDebugAndroidTestLintModel
> Task :app:generateDebugLintReportModel
> Task :app:generateDebugUnitTestLintModel
> Task :app:lintAnalyzeDebugAndroidTest
> Task :app:lintAnalyzeDebugUnitTest
> Task :app:bundleDebugClassesToRuntimeJar
> Task :app:compileDebugUnitTestKotlin NO-SOURCE
> Task :app:compileDebugUnitTestJavaWithJavac NO-SOURCE
> Task :app:processDebugJavaRes
> Task :app:processDebugUnitTestJavaRes NO-SOURCE
> Task :app:testDebugUnitTest NO-SOURCE
> Task :app:compileReleaseJavaWithJavac NO-SOURCE
> Task :app:bundleReleaseClassesToRuntimeJar
> Task :app:bundleReleaseClassesToCompileJar
> Task :app:compileReleaseUnitTestKotlin NO-SOURCE
> Task :app:compileReleaseUnitTestJavaWithJavac NO-SOURCE
> Task :app:processReleaseJavaRes
> Task :app:processReleaseUnitTestJavaRes NO-SOURCE
> Task :app:testReleaseUnitTest NO-SOURCE
> Task :app:test UP-TO-DATE
> Task :cli:checkKotlinGradlePluginConfigurationErrors SKIPPED
> Task :cli:compileKotlin
> Task :cli:compileJava NO-SOURCE
> Task :cli:processResources
> Task :cli:classes
> Task :cli:jar
> Task :cli:compileTestKotlin
> Task :cli:compileTestJava NO-SOURCE
> Task :cli:processTestResources NO-SOURCE
> Task :cli:testClasses UP-TO-DATE

> Task :cli:test

PreparationApiTest > pixelExactRotationAndCrop PASSED

PreparationApiTest > realPageDetectionAndPreparation PASSED

PreparationApiTest > manualCornersAndUploadedRasterReachCore PASSED

PreparationApiTest > badQuadAndParametersRecoverActualPreparation PASSED

PreparationApiTest > printableSheetAndStaticRoutesDoNotFakeAnOutline PASSED

SandboxApiTest > oversizedInputsReleaseTheCoreForRecovery PASSED

SandboxApiTest > fiveFixturesExecuteActualCore PASSED

SandboxApiTest > uploadedPngExecutesActualCore PASSED

SandboxApiTest > invalidParametersReleaseTheCoreForRecovery PASSED

SandboxApiTest > badImagesReleaseTheCoreForRecovery PASSED

SandboxApiTest > uploadedJpegExecutesActualCore PASSED

SandboxApiTest > routesAndLocalesPreserveAWorkingCore PASSED

SandboxApiTest > parameterChangesReachActualCore PASSED

> Task :app:lintAnalyzeDebug
> Task :cli:check

> Task :app:lintReportDebug
Wrote HTML report to file:///home/runner/work/photo-to-glyph/photo-to-glyph/app/build/reports/lint-results-debug.html

> Task :core:detekt
> Task :app:lintDebug
> Task :app:lint
> Task :app:check
> Task :core:loadKtlintReporters
> Task :core:runKtlintCheckOverKotlinScripts
> Task :core:ktlintKotlinScriptCheck
> Task :core:runKtlintCheckOverMainSourceSet
> Task :core:ktlintMainSourceSetCheck
> Task :core:runKtlintCheckOverTestSourceSet
> Task :core:ktlintTestSourceSetCheck
> Task :core:processTestResources NO-SOURCE
> Task :tools:detekt NO-SOURCE
> Task :tools:loadKtlintReporters
> Task :tools:runKtlintCheckOverKotlinScripts
> Task :tools:ktlintKotlinScriptCheck
> Task :core:compileTestKotlin

> Task :tools:verifyFixtures
PASS fixture: tools/fixtures/synthetic/m0/1046.png
PASS fixture: tools/fixtures/synthetic/m0/1041.png
PASS fixture: tools/fixtures/synthetic/m0/1074.png
PASS fixture: tools/fixtures/synthetic/m0/1064.png
PASS fixture: tools/fixtures/synthetic/m0/1092.png
PASS fixture: tools/fixtures/synthetic/m0/manifest.json
PASS fixture: tools/fixtures/synthetic/golden/bar-20-input.png
PASS fixture: tools/fixtures/synthetic/golden/bar-20-expected.png

> Task :core:compileTestJava NO-SOURCE
> Task :core:testClasses UP-TO-DATE

> Task :core:test

CoreSourcePolicyTest > actualProductionSourcesHaveNoAndroidImports PASSED

CoreSourcePolicyTest > ignoresNestedCommentsAndStringExamples PASSED

CoreSourcePolicyTest > rejectsQualifiedWildcardAndAliasedAndroidImports PASSED

GlyphSetRoundTripTest > partialSetJsonRoundTripPreservesModelAndPngBytes PASSED

GlyphSetRoundTripTest > closedGlyphStrokesAndMismatchedKeysAreRejected PASSED

InkComponentsTest > componentsAreEightConnectedAndReadingOrdered PASSED

InkComponentsTest > blankPageHasNoInkComponents PASSED

InkComponentsTest > componentCropIsPixelExactAndNonmutating PASSED

LegacyCoreSuiteTest > runThe21ExistingProductionCoreChecks STANDARD_OUT
    PASS GrayscaleTest.bt601 -> toGrayscale
    PASS GrayscaleTest.alphaOnWhite -> toGrayscale
    PASS AdaptiveThresholdTest.localMeanAndOffset -> adaptiveThreshold
    PASS AdaptiveThresholdTest.clippedBorderWindow -> adaptiveThreshold
    PASS DespeckleTest.strict12pxBoundary -> removeSmallComponents
    PASS DespeckleTest.eightConnectivity -> removeSmallComponents
    PASS ZhangSuenTest.golden20px -> zhangSuen
    PASS ZhangSuenTest.fixedPointAndBorder -> zhangSuen
    PASS ZhangSuenTest.preserveLoop -> zhangSuen, skeletonToPolylines
    PASS SkeletonToPolylinesTest.zheAtLeast3Open -> vectorize, skeletonToPolylines
    PASS SkeletonToPolylinesTest.cornerAndDiagonal -> skeletonToPolylines
    PASS SkeletonToPolylinesTest.cycleEdgeCoverage -> skeletonToPolylines
    PASS SkeletonToPolylinesTest.pruneStrictlyBelow2px -> skeletonToPolylines
    PASS SkeletonToPolylinesTest.dotAndEmpty -> skeletonToPolylines, strokesToSvg, requireStrokeOnlySvg
    PASS RdpTest.handSpecifiedSimplification -> simplifyRdp
    PASS RdpTest.zeroEpsilonAndShortPaths -> simplifyRdp
    PASS SvgNoFillTest.fiveActualExports -> vectorize, strokesToSvg, requireStrokeOnlySvg
    PASS SvgNoFillTest.rejectCorruptedExport -> strokesToSvg, requireStrokeOnlySvg
    PASS SvgNoFillTest.rejectClosureAndUseDecimalPoint -> strokesToSvg, requireStrokeOnlySvg
    PASS PipelineTest.deterministicAndImmutable -> vectorize
    PASS PipelineTest.blankImage -> vectorize, strokesToSvg, requireStrokeOnlySvg
    Tests: 21, passed: 21, failed: 0

LegacyCoreSuiteTest > runThe21ExistingProductionCoreChecks PASSED

M2Test > rectangularPerspectiveMatchesHandSpecifiedCrop PASSED

M2Test > gridSheetHasOwnLabelsMarksAndEscaping PASSED

M2Test > rectifyAnalyticPhotoAndAlignKnownGridProfiles PASSED

M2Test > suggestedSizeDoesNotGuessA4 PASSED

M2Test > transparentResizeHasNoHalo PASSED

M2Test > resizeIdentityAndAverageAreIndependentExpectations PASSED

M2Test > otsuHasHandDefinedBimodalThreshold PASSED

M2Test > degenerateQuadsAndAllocationBoundsAreRejected PASSED

M2Test > cropIsPixelExactAndRejectsOutside PASSED

M2Test > detectAnalyticYaw30SideLightCorners PASSED

M2Test > identityPerspectivePreservesArgb PASSED

M2Test > perspectiveMapsKnownCornersAndCenter PASSED

M2Test > mirrorIsPixelExactAndNonmutating PASSED

M2Test > pagePreparationPreservesSmallMarksByDefault PASSED

M2Test > quarterTurnsArePixelExact PASSED

M2Test > ambiguousBlankPageDoesNotInventDetection PASSED

M2Test > autoDetectedRectificationAlsoAlignsKnownGrid PASSED

PictureContoursTest > thresholdMustMatchNormalizedSobelRange PASSED

PictureContoursTest > thresholdControlsLowContrastEdges PASSED

PictureContoursTest > straightStepProducesThinBlackContourWithoutMutatingSource PASSED

PictureContoursTest > uniformImageProducesNoContours PASSED

> Task :core:check
> Task :tools:check
> Task :check
> Task :app:mergeDebugNativeDebugMetadata NO-SOURCE
> Task :app:mergeDebugShaders
> Task :app:compileDebugShaders NO-SOURCE
> Task :app:generateDebugAssets UP-TO-DATE
> Task :app:mergeDebugAssets
> Task :app:compressDebugAssets
> Task :app:desugarDebugFileDependencies
> Task :app:dexBuilderDebug
> Task :app:mergeDebugGlobalSynthetics
> Task :app:checkDebugDuplicateClasses
> Task :app:mergeDebugJavaResource
> Task :app:mergeExtDexDebug
> Task :app:mergeLibDexDebug
> Task :app:mergeDebugJniLibFolders
> Task :app:mergeProjectDexDebug
> Task :app:mergeDebugNativeLibs
> Task :app:validateSigningDebug
> Task :app:writeDebugAppMetadata
> Task :app:writeDebugSigningConfigVersions

> Task :app:stripDebugDebugSymbols
Unable to strip the following libraries, packaging them as they are: libandroidx.graphics.path.so, libimage_processing_util_jni.so, libsurface_util_jni.so.

> Task :app:generateReleaseLintVitalReportModel
> Task :app:packageDebug
> Task :app:createDebugApkListingFileRedirect
> Task :app:assembleDebug
> Task :app:mergeReleaseJniLibFolders
> Task :app:mergeReleaseNativeLibs

> Task :app:stripReleaseDebugSymbols
Unable to strip the following libraries, packaging them as they are: libandroidx.graphics.path.so, libimage_processing_util_jni.so, libsurface_util_jni.so.

> Task :app:extractReleaseNativeSymbolTables
> Task :app:mergeReleaseNativeDebugMetadata NO-SOURCE
> Task :app:checkReleaseDuplicateClasses
> Task :app:dexBuilderRelease
> Task :app:desugarReleaseFileDependencies
> Task :app:mergeReleaseStartupProfile
> Task :app:lintVitalAnalyzeRelease
> Task :app:lintVitalReportRelease SKIPPED
> Task :app:lintVitalRelease SKIPPED
> Task :app:mergeReleaseArtProfile
> Task :app:mergeExtDexRelease
> Task :app:mergeReleaseGlobalSynthetics
> Task :app:mergeReleaseShaders
> Task :app:compileReleaseShaders NO-SOURCE
> Task :app:generateReleaseAssets UP-TO-DATE
> Task :app:mergeReleaseAssets
> Task :app:compressReleaseAssets
> Task :app:extractReleaseVersionControlInfo
> Task :app:mergeDexRelease
> Task :app:mergeReleaseJavaResource
> Task :app:optimizeReleaseResources
> Task :app:collectReleaseDependencies
> Task :app:sdkReleaseDependencyData
> Task :app:writeReleaseAppMetadata
> Task :app:writeReleaseSigningConfigVersions
> Task :app:compileReleaseArtProfile
> Task :cli:startScripts
> Task :cli:distTar
> Task :cli:distZip
> Task :cli:assemble
> Task :cli:build
> Task :core:assemble
> Task :core:build
> Task :tools:assemble UP-TO-DATE
> Task :tools:build

> Task :cli:smoke
INPUT: original synthetic 300x300 PNGs, 6 px strokes; not real handwriting
TIMING: PNG decode + core + SVG serialization/validation; excludes JVM startup/output writes; 2 warmups + 5 measured runs
char | strokes | closed | graph cycles | split chains | cold ms | median ms | max ms
Ж | 8 | 0 | 0 | 0 | 298.865 | 27.978 | 298.865
Б | 3 | 0 | 1 | 1 | 22.933 | 12.876 | 22.933
в | 3 | 0 | 2 | 0 | 12.108 | 11.369 | 12.822
Ш | 3 | 0 | 0 | 0 | 8.999 | 8.164 | 16.751
ф | 5 | 0 | 2 | 0 | 14.084 | 11.790 | 14.084
PASS smoke: 5/5 nonempty glyphs, zero closed polylines, explicit no-fill strokes, within budget
Outputs: /home/runner/work/photo-to-glyph/photo-to-glyph/cli/build/smoke/<codepoint>.svg, intermediate PNGs, metrics.csv

> Task :app:packageRelease
> Task :app:createReleaseApkListingFileRedirect
> Task :app:assembleRelease
> Task :app:assemble
> Task :app:build
> Task :build
gradle/actions: Writing build results to /home/runner/work/_temp/.gradle-actions/build-results/verification-1792094955608.json

BUILD SUCCESSFUL in 3m 27s
134 actionable tasks: 134 executed

```

## 3) Реальные функции, вызываемые тестами

- `InkComponentsTest.componentsAreEightConnectedAndReadingOrdered` → production `connectedInkComponents`.
- `InkComponentsTest.componentCropIsPixelExactAndNonmutating` → production `cropBinaryGrid`.
- `InkComponentsTest.blankPageHasNoInkComponents` → production `connectedInkComponents`.
- `GlyphSetRoundTripTest.partialSetJsonRoundTripPreservesModelAndPngBytes` → `GlyphSetJson.encode`, `GlyphSetJson.decode`, `PolylineSerializer`, сериализуемые model-типы и их `init`-валидации.
- `GlyphSetRoundTripTest.closedGlyphStrokesAndMismatchedKeysAreRejected` → конструкторы `Stroke` и `GlyphSet` с production-инвариантами.
- M2 preparation checks, выполняемые standalone runner и Gradle, перечисляют `prepareImage`, `adaptiveThreshold`, `removeSmallComponents`, `detectPageQuad`, `rectifyPerspective`, `mapPagePoint`, `cropImage`, `rotateQuarterTurns`, `resizeToMaxEdge`, projection helpers и `gridSheetSvg`.
- `PictureContoursTest` вызывает `blackContourImage` и validation `PipelineParams`; контурный raster path остаётся отдельным от осевых handwriting strokes.
- CLI smoke вызывает настоящий core vectorization и SVG serializer/validator; его синтетические источники не являются фотографиями реального почерка.

## 4) Приёмка

| Статус | Результат |
| --- | --- |
| Проверено автоматически | 8-связные компоненты и их reading order; pixel-exact crop без изменения источника; partial-set JSON round-trip, PNG bytes и отклонение выбранных неверных инвариантов; тест сохранения мелких отметок; Gradle build, core tests, Android lint и CLI smoke — CI run `38799609923`. |
| Требует проверки человеком | Реальная Android camera permission/capture/import и подготовка фото на устройстве/эмуляторе; качество обнаружения чернил на согласованных реальных страницах; контурный режим на реальных фото. Физическое устройство здесь не проверялось. |
| Не сделано | OCR/назначение символов, группировка отдельных компонент в буквы и диакритики, разделение слитного курсивного текста, экран коррекции, Android local persistence/library, `.gfont` export и KDraw validation. Это foundation-only отчёт, не завершение M3/MVP. |

## 5) Допущения и неизвестное

- 8-связность и reading order дают детерминированные **компоненты**, но одна буква может иметь несколько компонентов; связанная cursive-линия может содержать несколько букв. Здесь это намеренно не решается и не выдаётся за OCR.
- Порог сохранения мелких компонент `1 px` выбран ради точек и диакритики; реальные шумы снимка могут потребовать отдельной фильтрации с интерфейсной настройкой.
- JSON — внутренний core codec/round-trip foundation, не Android storage schema, не доказательство совместимости `.gfont` или KDraw.
- CI APK собирается; permission/camera behavior, фото и hardware QA вручную не выполнялись. Фотографии пользователя в Git не добавлялись.

## 6) Запись CHANGELOG

В `CHANGELOG.md` зафиксированы deterministic 8-connected candidate segmentation/crops и serializable partial `GlyphSet` with JSON tests; отдельно указано, что full Gradle/Android CI run `38799609923` прошёл, а OCR/assignment, correction UI, local storage и `.gfont` остаются незавершёнными.
