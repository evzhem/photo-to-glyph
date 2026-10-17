# M9 — документированный `.gfont` codec foundation (не принят по KDraw)

Дата: **2026-10-17**. Статус: **синтетический round-trip документированного поднабора и Android export реализованы; M9 не завершён**. Финальный совместный build с manual builder прошёл в ветке `arena/6d0fb259-photo-to-glyph`, commit `61a5ab7fd0d725ea0851dfa8e57fb8787e24c319`, GitHub Actions run [39017154083](https://github.com/evzhem/photo-to-glyph/actions/runs/39017154083). Реальный `.gfont`, KDraw и plotter не проверялись.

## 1. Файлы и назначение

- `core/src/main/kotlin/photoglyph/core/export/GfontCodec.kt` — pure Kotlin/JVM ZIP writer/reader для документированной записи glyph: big-endian fields, decimal Unicode entry, Y-up → Y-down, bounded archive/records, finite coordinates и strict flags.
- `core/src/test/kotlin/photoglyph/core/GfontCodecChecks.kt`, `GfontRoundTripTest.kt` — production-codec round-trip и malformed-record/unsupported-codepoint проверки на оригинальных синтетических наборах.
- `app/src/main/kotlin/photoglyph/app/GlyphSetLibraryScreen.kt` — Android SAF `.gfont` export для одного сохранённого набора за раз; sets не объединяются.
- `app/src/main/res/values/strings.xml`, `values-ru/strings.xml` — экспортные подписи и явное предупреждение о непроверенном KDraw scale/compatibility.
- `docs/04-формат-gfont.md`, `AGENTS.md`, `CHANGELOG.md`, `README.md`, `ROADMAP.md` — задокументированные допущения и неизвестные.
- Позднее добавленный `AlphabetBuilderScreen.kt` соединяет manual page crops с этим export через `LocalGlyphSetStore`; он не добавляет OCR.

## 2. Полный вывод build / tests / lint / smoke

Весь workflow запускает `./gradlew --console=plain --no-daemon build :core:test :cli:smoke`. Полный captured output того же успешного CI run `39017154083` опубликован дословно в [`M3-manual-builder-report.md`](M3-manual-builder-report.md), раздел 2; там же есть тестовые результаты builder-а. Соответствующие строки для этого codec — `GfontRoundTripTest` и общий `BUILD SUCCESSFUL`.

## 3. Какие production-функции вызвали тесты

- `GfontRoundTripTest.partialGlyphSetRoundTripsDocumentedCoordinatesAndFlags` вызывает `GfontCodec.encode` и `GfontCodec.decode` на partial `GlyphSet`; проверяет ZIP entry names, big-endian code/coordinate/flag layout, baseline/Y inversion, flags и повторяемость output.
- `malformedRecordsAndUnsupportedEntriesAreRejected` вызывает `GfontCodec.decode` на самостоятельно собранных ZIP/binary byte arrays с неверными count/name/code/flags, NaN и trailing bytes.
- `supplementaryPlaneCharactersAreRejectedByTheDocumentedUint16Field` вызывает production `encode` с emoji и проверяет отказ для codepoint за пределами `uint16`.
- В том же CI проходят `BinaryVectorizeTest`, `GlyphSetNormalizerTest`, `GlyphSetRoundTripTest`, `InkComponentsTest`, app store tests и полный M0/core/CLI suite; все детали приведены в общей полной распечатке из раздела 2.
- Android SAF экран компилируется и проходит lint, но реальное сохранение файла через picker не нажималось на устройстве.

## 4. Приёмка

| Критерий | Проверено автоматически | Требует проверки человеком | Не сделано |
| --- | --- | --- | --- |
| Документированная ZIP glyph-record структура | Synthetic production round-trip, exact stroke flags, Y conversion и malformed-record rejection проходят | Сверить с разрешённым реальным `.gfont` | Дополнительные необязательные записи/sidecar неизвестны |
| Библиотечный export | Compose/SAF code compiles; codec core tests pass | Создать файл на Android и открыть его в KDraw | KDraw import/scale/plotter output acceptance |
| Независимость частичных наборов | Codec пишет glyphs одного переданного `GlyphSet`; local store tests подтверждают отдельные наборы | Проверить KDraw substitution между двумя импортированными set files | PhotoGlyph merge/fallback отсутствуют намеренно |
| Границы/злоупотребление | Пределы ZIP/entry/points, NaN, invalid flags и unsupported BMP range валидируются | Проверить ожидаемые лимиты на реальных файлах | Совместимость со всеми историческими вариантами формата |

## 5. Допущения и неизвестные

- Неизвестны реальные координатные единицы, relation к KDraw font size, служебные ZIP entries и необходимость `.gfont.txt`. `GfontCodec` сознательно не выдумывает эти записи.
- Кодировщик сохраняет единицы модели без scale conversion. Текущая ручная нормализация использует target cap-height 700/1000; физическое значение этого масштаба неизвестно.
- Предполагается один flag на точку (`flagCount == pointCount`) и первый flag `0`; структура/первая точка валидируются по документированному поднабору, но это допущение не подтверждено реальным KDraw файлом.
- `uint16` ограничивает текущую реализацию BMP; supplementary-plane symbols отклоняются.
- Round-trip synthetic data подтверждает внутреннюю согласованность codec, но не KDraw/plotter compatibility. UI показывает предупреждение; реальный файл/устройство не использовались.

## 6. Запись CHANGELOG

`M9 — 2026-10-17 (documented .gfont codec foundation; incomplete)`: pure Kotlin/JVM ZIP record encode/decode; big-endian code/coords/flags; Y conversion; validation; production round-trip/malformed-record tests; отдельный библиотечный SAF export; no guessed service entries/sidecar; KDraw scale and real-file compatibility remain unverified.

### Полный CI output

Все строки captured output из check-run `117352204747` сохранены; единственный завершающий пробел в строке Gradle `Daemon will be stopped at the end of the build` удалён для `git diff --check`.

```textTo honour the JVM settings for this build a single-use Daemon process will be forked. For more on this, please refer to https://docs.gradle.org/8.11.1/userguide/gradle_daemon.html#sec:disabling_the_daemon in the Gradle documentation.
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
> Task :cli:runKtlintCheckOverTestSourceSet
> Task :cli:ktlintTestSourceSetCheck
> Task :cli:checkKotlinGradlePluginConfigurationErrors SKIPPED
> Task :cli:processResources
> Task :cli:processTestResources NO-SOURCE
> Task :core:compileKotlin
> Task :core:detekt
> Task :core:compileJava NO-SOURCE
> Task :core:classes UP-TO-DATE
> Task :core:jar
> Task :app:compileDebugKotlin
> Task :app:compileReleaseKotlin
> Task :app:compileDebugJavaWithJavac NO-SOURCE
> Task :app:bundleDebugClassesToCompileJar
> Task :app:generateDebugLintReportModel
> Task :app:generateDebugAndroidTestLintModel
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

> Task :app:compileReleaseJavaWithJavac NO-SOURCE
> Task :app:bundleReleaseClassesToRuntimeJar
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
> Task :cli:compileKotlin
> Task :cli:compileJava NO-SOURCE
> Task :cli:classes
> Task :cli:jar
> Task :app:lintAnalyzeDebug
> Task :cli:compileTestKotlin
> Task :cli:compileTestJava NO-SOURCE
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

> Task :cli:check

> Task :app:lintReportDebug
Wrote HTML report to file:///home/runner/work/photo-to-glyph/photo-to-glyph/app/build/reports/lint-results-debug.html

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

BinaryVectorizeTest > preparedOnePixelLineProducesAnOpenCenterlineWithoutASecondThresholdPass PASSED

BinaryVectorizeTest > defaultBinaryPathPreservesSmallDetachedMarks PASSED

CoreSourcePolicyTest > actualProductionSourcesHaveNoAndroidImports PASSED

CoreSourcePolicyTest > ignoresNestedCommentsAndStringExamples PASSED

CoreSourcePolicyTest > rejectsQualifiedWildcardAndAliasedAndroidImports PASSED

GfontRoundTripTest > supplementaryPlaneCharactersAreRejectedByTheDocumentedUint16Field PASSED

GfontRoundTripTest > partialGlyphSetRoundTripsDocumentedCoordinatesAndFlags PASSED

GfontRoundTripTest > malformedRecordsAndUnsupportedEntriesAreRejected PASSED

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
Ж | 8 | 0 | 0 | 0 | 318.594 | 27.733 | 318.594
Б | 3 | 0 | 1 | 1 | 16.767 | 11.074 | 16.767
в | 3 | 0 | 2 | 0 | 11.364 | 11.291 | 13.153
Ш | 3 | 0 | 0 | 0 | 8.463 | 8.441 | 8.998
ф | 5 | 0 | 2 | 0 | 9.650 | 9.429 | 11.038
PASS smoke: 5/5 nonempty glyphs, zero closed polylines, explicit no-fill strokes, within budget
Outputs: /home/runner/work/photo-to-glyph/photo-to-glyph/cli/build/smoke/<codepoint>.svg, intermediate PNGs, metrics.csv

> Task :app:packageRelease
> Task :app:createReleaseApkListingFileRedirect
> Task :app:assembleRelease
> Task :app:assemble
> Task :app:build
> Task :build
gradle/actions: Writing build results to /home/runner/work/_temp/.gradle-actions/build-results/verification-1792225401425.json

BUILD SUCCESSFUL in 3m 31s
142 actionable tasks: 142 executed

```
