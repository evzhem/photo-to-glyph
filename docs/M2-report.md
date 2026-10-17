# M2 — камера, импорт и подготовка страницы (частично проверено на устройстве)

Последнее обновление: **2026-10-17**. Код M2 прошёл full Gradle/Android CI run [38883213683](https://github.com/evzhem/photo-to-glyph/actions/runs/38883213683) на commit `2aedf07e9270e1200f767d97d4b87c774fad5504`; `./tools/m2-standalone-check.sh` завершился с кодом `0`. Пользователь проверил приложение на Android и сообщил, что камера работает и фотографии из галереи импортируются. Приложенные скриншоты показывают загруженную страницу и подготовленный ч/б raster размером 601 × 800 px. Модель телефона/версия Android и отдельный сценарий отказа permission не указаны.

M2 **остаётся частичным**: в скриншотах зелёная рамка совпадает с границами фотографии, а не листа; в ч/б результате видна полоса стола/шума над страницей. Поэтому точное определение углов и ручная правка рамки на реальном фото ещё не подтверждены. В этой среде нет `adb`, `emulator`, `ANDROID_HOME` и `ANDROID_SDK_ROOT`, поэтому мы не можем повторить device QA самостоятельно. До проверки, что рамку можно довести до границ листа и получить пригодное выравнивание, следующий milestone по порядку `ROADMAP.md` не закрывается.

## 1. Файлы и назначение

- `core/src/main/kotlin/photoglyph/core/img/Raster.kt` — bounded immutable ARGB/grayscale/binary raster types.
- `ImageTransforms.kt`, `PageQuad.kt`, `PageDetection.kt`, `Perspective.kt`, `PreparedImage.kt`, `Binarization.kt` — pixel-exact crop/quarter-turn/mirror, resize, проверяемый четырёхугольник, поиск углов, perspective rectification, projection-profile alignment и ч/б подготовка страницы. Размер не трактуется как физический A4 без данных.
- `app/src/main/kotlin/photoglyph/app/PreparationScreen.kt`, `PagePreparationContent.kt` — камера-first entry, import fallback, режим страницы, ручная правка углов и controls подготовки.
- `CameraCapture.kt`, `ImageImporter.kt`, `AndroidManifest.xml` — CameraX preview/capture, импорт raster-файла, permission и необязательная camera feature declaration.
- `MainActivity.kt`, `app/src/main/res/values*/strings.xml` — Compose entry/resources; camera-first сценарий остаётся главным, тексты локализованы.
- `core/src/test/kotlin/photoglyph/core/M2Checks.kt`, `M2Test.kt` — 17 тестов настоящих core image/preparation functions.
- `cli/src/test/kotlin/photoglyph/cli/sandbox/PreparationApiChecks.kt`, `PreparationApiTest.kt` — пять HTTP/API tests, доходящих до реального core.
- `tools/m2-standalone-check.sh`, `tools/M2StandaloneTests.kt` — дополнительная Kotlin/JVM проверка production subset и HTTP API; не является Android/Gradle build.

Отдельные supporting checks в этой команде также запускают M0, source policy, InkComponents и уже реализованный независимый picture-contour pipeline. Ни один из них не доказывает распознавание handwriting page: в M2 результат — подготовленный raster, не распознанные буквы.

## 2. Проверки и полный вывод

### Локальная standalone-проверка

Команда `./tools/m2-standalone-check.sh` повторно запущена после M0 graph-эксперимента и после документационной правки; exit code `0`. Результат включает: 21 существующую M0 production check, 3 source-policy checks, 8 sandbox/API recovery checks, 17 M2 preparation checks, 3 InkComponents checks, 4 picture-contour checks, 5 preparation API checks и smoke 5/5 синтетических glyphs. Это не компиляция Android-приложения, не Gradle build и не device test.

Полный stdout/stderr этой standalone-команды:

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
Ж | 8 | 0 | 0 | 0 | 258.062 | 22.542 | 258.062
Б | 3 | 0 | 1 | 1 | 20.438 | 14.403 | 20.438
в | 3 | 0 | 2 | 0 | 24.450 | 17.801 | 26.172
Ш | 3 | 0 | 0 | 0 | 13.032 | 12.327 | 27.317
ф | 5 | 0 | 2 | 0 | 12.828 | 15.996 | 30.554
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

### Полный Gradle/Android CI

GitHub Actions run [38883213683](https://github.com/evzhem/photo-to-glyph/actions/runs/38883213683) завершился успешно (`BUILD SUCCESSFUL`, 142 actionable tasks): build, core/CLI tests, app Debug/Release unit tests, lint, formatting и CLI synthetic smoke. В нём прошли 17 M2 core tests и пять `PreparationApiTest`; это автоматическая сборка и тесты, а не permission/camera/device acceptance.

Полный captured output check-run «Verification output» (`116915644023`):

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
> Task :app:runKtlintCheckOverTestSourceSet
> Task :app:ktlintTestSourceSetCheck
> Task :core:processResources NO-SOURCE
> Task :app:checkKotlinGradlePluginConfigurationErrors SKIPPED
> Task :app:preBuild UP-TO-DATE
> Task :app:preDebugBuild UP-TO-DATE
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
> Task :core:compileKotlin
> Task :core:compileJava NO-SOURCE
> Task :core:classes UP-TO-DATE
> Task :core:jar
> Task :cli:runKtlintCheckOverTestSourceSet
> Task :app:compileReleaseKotlin
> Task :app:compileReleaseJavaWithJavac NO-SOURCE
> Task :app:compileDebugKotlin
> Task :app:compileDebugJavaWithJavac NO-SOURCE
> Task :app:bundleReleaseClassesToRuntimeJar
> Task :app:bundleDebugClassesToCompileJar
> Task :app:generateDebugAndroidTestLintModel
> Task :app:generateDebugLintReportModel
> Task :app:generateDebugUnitTestLintModel
> Task :app:lintAnalyzeDebugAndroidTest
> Task :app:lintAnalyzeDebugUnitTest
> Task :app:bundleDebugClassesToRuntimeJar
> Task :app:compileDebugUnitTestKotlin
> Task :app:compileDebugUnitTestJavaWithJavac NO-SOURCE
> Task :app:processDebugJavaRes
> Task :app:processDebugUnitTestJavaRes

> Task :app:testDebugUnitTest

LocalGlyphSetStoreTest > hashesIdsBeforeUsingThemAsFileNames PASSED

LocalGlyphSetStoreTest > malformedStoredJsonIsReportedInsteadOfSilentlyDropped PASSED

LocalGlyphSetStoreTest > storesIndependentPartialSetsAcrossStoreInstances PASSED

LocalGlyphSetStoreTest > replacesAndDeletesOnlyTheRequestedSet PASSED

> Task :app:bundleReleaseClassesToCompileJar
> Task :app:compileReleaseUnitTestKotlin
> Task :app:compileReleaseUnitTestJavaWithJavac NO-SOURCE
> Task :app:processReleaseJavaRes
> Task :app:processReleaseUnitTestJavaRes

> Task :app:testReleaseUnitTest

LocalGlyphSetStoreTest > hashesIdsBeforeUsingThemAsFileNames PASSED

LocalGlyphSetStoreTest > malformedStoredJsonIsReportedInsteadOfSilentlyDropped PASSED

LocalGlyphSetStoreTest > storesIndependentPartialSetsAcrossStoreInstances PASSED

LocalGlyphSetStoreTest > replacesAndDeletesOnlyTheRequestedSet PASSED

> Task :app:test
> Task :cli:ktlintTestSourceSetCheck
> Task :cli:checkKotlinGradlePluginConfigurationErrors SKIPPED
> Task :app:lintAnalyzeDebug

> Task :app:lintReportDebug
Wrote HTML report to file:///home/runner/work/photo-to-glyph/photo-to-glyph/app/build/reports/lint-results-debug.html

> Task :app:lintDebug
> Task :app:lint
> Task :app:check
> Task :cli:processResources
> Task :cli:processTestResources NO-SOURCE
> Task :cli:compileKotlin
> Task :core:detekt
> Task :cli:compileJava NO-SOURCE
> Task :cli:classes
> Task :cli:jar
> Task :core:loadKtlintReporters
> Task :core:runKtlintCheckOverKotlinScripts
> Task :core:ktlintKotlinScriptCheck
> Task :cli:compileTestKotlin
> Task :cli:compileTestJava NO-SOURCE
> Task :cli:testClasses UP-TO-DATE
> Task :core:runKtlintCheckOverMainSourceSet

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

> Task :cli:check
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

GlyphSetNormalizerTest > descendersUseTheSharedBaselineAndReferenceScale PASSED

GlyphSetNormalizerTest > invalidLabelsAndMetricsAreRejected PASSED

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
> Task :app:generateReleaseLintVitalReportModel

> Task :app:stripDebugDebugSymbols
Unable to strip the following libraries, packaging them as they are: libandroidx.graphics.path.so, libimage_processing_util_jni.so, libsurface_util_jni.so.

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
Ж | 8 | 0 | 0 | 0 | 200.644 | 13.886 | 200.644
Б | 3 | 0 | 1 | 1 | 11.300 | 9.377 | 12.326
в | 3 | 0 | 2 | 0 | 8.574 | 7.917 | 8.574
Ш | 3 | 0 | 0 | 0 | 12.000 | 6.632 | 12.000
ф | 5 | 0 | 2 | 0 | 9.595 | 6.849 | 9.595
PASS smoke: 5/5 nonempty glyphs, zero closed polylines, explicit no-fill strokes, within budget
Outputs: /home/runner/work/photo-to-glyph/photo-to-glyph/cli/build/smoke/<codepoint>.svg, intermediate PNGs, metrics.csv

> Task :app:packageRelease
> Task :app:createReleaseApkListingFileRedirect
> Task :app:assembleRelease
> Task :app:assemble
> Task :app:build
> Task :build
gradle/actions: Writing build results to /home/runner/work/_temp/.gradle-actions/build-results/verification-1792144251545.json

BUILD SUCCESSFUL in 2m 26s
142 actionable tasks: 142 executed

```

## 3. Тесты и production-функции

В `M2Test` все 17 проверок исполняют production-функции из `core/src/main/kotlin/photoglyph/core/img`:

| Тест | Production-функции |
| --- | --- |
| `transparentResizeHasNoHalo` | `resizeToMaxEdge` |
| `mirrorIsPixelExactAndNonmutating` | `mirrorHorizontal` |
| `suggestedSizeDoesNotGuessA4` | `suggestedRectifiedSize` |
| `quarterTurnsArePixelExact` | `rotateQuarterTurns` |
| `cropIsPixelExactAndRejectsOutside` | `cropImage` |
| `resizeIdentityAndAverageAreIndependentExpectations` | `resizeToMaxEdge` |
| `perspectiveMapsKnownCornersAndCenter` | `mapPagePoint` |
| `identityPerspectivePreservesArgb` | `rectifyPerspective` |
| `rectangularPerspectiveMatchesHandSpecifiedCrop` | `rectifyPerspective` |
| `degenerateQuadsAndAllocationBoundsAreRejected` | `PageQuad`, `rectifyPerspective` |
| `otsuHasHandDefinedBimodalThreshold` | `otsuThreshold` |
| `pagePreparationPreservesSmallMarksByDefault` | `prepareImage`, `adaptiveThreshold`, `removeSmallComponents` |
| `ambiguousBlankPageDoesNotInventDetection` | `detectPageQuad` |
| `detectAnalyticYaw30SideLightCorners` | `detectPageQuad` |
| `rectifyAnalyticPhotoAndAlignKnownGridProfiles` | `prepareImage`, `whiteProjectionColumns`, `whiteProjectionRows` |
| `autoDetectedRectificationAlsoAlignsKnownGrid` | `detectPageQuad`, `prepareImage`, `whiteProjectionColumns`, `whiteProjectionRows` |
| `gridSheetHasOwnLabelsMarksAndEscaping` | `gridSheetSvg`, `cyrillicGridCharacters` |

Пять `PreparationApiTest` исполняют server/API route и реальный core: `realPageDetectionAndPreparation`, `manualCornersAndUploadedRasterReachCore`, `pixelExactRotationAndCrop`, `badQuadAndParametersRecoverActualPreparation`, `printableSheetAndStaticRoutesDoNotFakeAnOutline`. В зависимости от случая это вызывает `detectPageQuad`, `prepareImage`, `cropImage`, `rotateQuarterTurns` и `gridSheetSvg`; exact per-test trace напечатан в полном standalone output выше и в CI output.

Supporting suites также вызывают production-код: 21 legacy M0 checks (функции перечислены в standalone output); 3 `InkComponentsTest` → `connectedInkComponents`/`cropBinaryGrid`; 4 `PictureContoursTest` → `blackContourImage`/`PipelineParams`; 8 `SandboxApiTest` → реальный HTTP + `vectorize`/SVG validation. Эти тесты не являются доказательством распознавания букв или реальной работы Android camera.

Статический просмотр приложения подтверждает только wiring: `PreparationScreen` на первом `LaunchedEffect` запрашивает системный CAMERA permission и открывает `CameraCaptureDialog` после grant; dialog использует full-window sizing и CameraX preview/capture. При отказе permission или ошибке CameraX отображается сообщение; после dismiss остаётся доступен импорт фото. CI компилирует app и выполняет app unit tests, но не содержит Android instrumentation tests для этих ветвей.

## 4. Приёмка

| Проверено автоматически | Требует проверки человеком | Не сделано |
| --- | --- | --- |
| Pixel-exact crop/rotation/mirror; bounded resize; ручной page quad; analytic page detection; perspective mapping/rectification; threshold/mark preservation; projection alignment; подготовка API. Все 17 core + 5 API checks прошли standalone и current full CI. Android Debug/Release compile, app unit tests, lint и formatting прошли CI. | Пользователь подтвердил, что на Android камера работает, фото импортируются из галереи; screenshots показывают подготовленный ч/б raster 601 × 800. Не указаны модель/API и сценарий permission denial. Нужно проверить углы: зелёная рамка на скриншотах обводит всю фотографию и оставляет стол в итоговом изображении; ручное перемещение углов к листу и результат повторной подготовки не показаны. | Полная device acceptance подготовки страницы (точное выделение листа/восстановление при неудачной автодетекции); OCR, letter segmentation/recognition/labels, correction UI, автоматический baseline inference, сохранение результата распознавания и `.gfont` export — это следующие этапы. |

## 5. Предположения и неизвестное

- Основной input — свободная фотография страницы с рукописными буквами; сетка остается дополнительным тестовым/печатаемым путём, не обязательным форматом.
- Четыре угла можно определить автоматически на некоторых страницах; `PageQuad` можно задать/исправить вручную. Аналитическая синтетическая фотография не эквивалентна реальной съемке.
- `PreparedImage` содержит rectified raster, grayscale и binary; оно не содержит буквенных labels и не утверждает, что текст распознан.
- Команда standalone использует только Kotlin/JVM production subset и HTTP test adapter. Успешный standalone run не заменяет обязательный full Gradle/Android CI, который прошёл отдельно.
- Пользователь подтвердил работу камеры и импорта из галереи; детали устройства, permission denial и camera-unavailable fallback неизвестны.
- Скриншоты показывают, что исходная рамка совпадает с внешними границами фото, а подготовленный raster содержит верхнюю полосу стола/шум. Неясно, нажималась ли «Найти углы» и пробовалась ли ручная правка; страничная rectification на этом примере не принимается как подтверждённая.
- Для завершения M2 достаточно повторить подготовку на этом фото после нажатия «Найти углы» и, если нужно, перетаскивания четырёх зелёных маркеров к углам листа. Пользовательские фотографии в репозиторий не добавлять.

## 6. Запись CHANGELOG

В `CHANGELOG.md` сохранена историческая запись о M2 camera-first/photo preparation, автоматические проверки и user-reported manual result (camera/gallery работают). Обнаруженная на screenshot проблема с рамкой/шумной полосой явно оставлена непроверенной; см. соответствующий пункт `CHANGELOG.md`.
