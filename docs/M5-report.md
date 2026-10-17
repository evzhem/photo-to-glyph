# M5 — локальное хранилище и нормализация glyphs (частичный результат)

Дата этого M5-среза: **2026-10-16**. Его статус — app-private storage, read-only список и core-нормализация явно размеченных raster glyphs; проверки прошли на ветке `arena/6d0fb259-photo-to-glyph`, commit `2aedf07e9270e1200f767d97d4b87c774fad5504`, CI run [38883213683](https://github.com/evzhem/photo-to-glyph/actions/runs/38883213683). На эту дату не было OCR, UI создания набора или `.gfont` export. **Обновление 2026-10-17:** ручной builder теперь сохраняет созданные из страницы partial sets, показывает независимый uppercase/lowercase прогресс для кириллицы и латиницы и подаёт наборы в `.gfont` library export; это не добавляет OCR или KDraw validation. См. [`M3-manual-builder-report.md`](M3-manual-builder-report.md) и будущий отчёт case-picker slice.

## 1. Файлы и назначение

- `app/src/main/kotlin/photoglyph/app/LocalGlyphSetStore.kt` — app-level file store для независимых JSON snapshots `GlyphSet`: SHA-256-имена файлов, атомарная замена, загрузка/список/удаление по ID, сообщения об ошибке повреждённых данных.
- `app/src/test/kotlin/photoglyph/app/LocalGlyphSetStoreTest.kt` — четыре JUnit-теста, исполняющие настоящее хранилище с временными каталогами и синтетическими наборами.
- `app/src/main/kotlin/photoglyph/app/GlyphSetLibraryScreen.kt` — Compose-экран чтения сохранённых наборов, состояния загрузки/ошибки/пустого списка и возврат к камере. Он не создаёт и не изменяет наборы.
- `app/src/main/kotlin/photoglyph/app/MainActivity.kt` — создаёт store в app-private `filesDir/glyph-sets` и переключает camera-first экран и библиотеку.
- `app/src/main/kotlin/photoglyph/app/PreparationScreen.kt` — кнопка «Мои наборы»; существующий сценарий запуска камеры сохранён.
- `app/src/main/res/values/strings.xml`, `app/src/main/res/values-ru/strings.xml` — английские и русские подписи.
- `app/build.gradle.kts` — app JUnit dependency и вывод имён passed/failed/skipped-тестов в CI.
- `core/src/main/kotlin/photoglyph/core/model/GlyphSetNormalizer.kt` — production-конвертер явно размеченных raster paths из Y-down в общий Y-up `GlyphSet`, с общим baseline и scale от переданных reference metrics.
- `core/src/test/kotlin/photoglyph/core/GlyphSetNormalizerChecks.kt`, `GlyphSetNormalizerTest.kt` — production-based проверки нормализатора и его валидации.
- `README.md`, `ROADMAP.md`, `CHANGELOG.md`, `AGENTS.md`, `docs/01-требования-mvp.md`, `docs/M3-report.md` — актуальный статус реализации и ограничения.

Хранилище сериализует существующую модель `GlyphSet` через production codec `GlyphSetJson`; наборы остаются независимыми и неполными. Интеграция экрана пока только показывает ранее сохранённые данные: в пользовательском UI ещё нет пути их создать.

Нормализатор вызывает алгоритм только после ручной разметки символа, `originX` и baseline для каждого glyph, а также передачи x-height/cap-height reference. Он задаёт baseline модели равным `0`, переводит Y-down в Y-up и масштабирует координаты по явно заданному target cap-height. Он не ищет буквы, не распознаёт подписи и не оценивает baselines, sidebearings, slant или reference heights автоматически.

## 2. Проверки и полный вывод Gradle/Android CI

Первичная проверка storage-среза — GitHub Actions run [38849746285](https://github.com/evzhem/photo-to-glyph/actions/runs/38849746285) на commit `d65829e` — завершилась успешно; в Debug и Release прошли четыре `LocalGlyphSetStoreTest`.

Актуальная полная проверка после добавления нормализатора — GitHub Actions run [38883213683](https://github.com/evzhem/photo-to-glyph/actions/runs/38883213683) на commit `2aedf07` — завершилась успешно: Gradle build, core/CLI tests, app Debug/Release tests, lint, formatting и synthetic CLI smoke; `BUILD SUCCESSFUL`, **142 actionable tasks**. В core прошли обе новые проверки нормализатора, тесты модели JSON, подготовки страниц, picture contours и 21 legacy production check. Это CI build, не запуск на Android устройстве.

Ниже сначала сохранён полный captured Gradle output первоначальной storage-проверки из check-run «Verification output» (ID `116809267527`); после него добавлен полный output актуального run из check-run `116915644023`.

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
> Task :cli:runKtlintCheckOverTestSourceSet
> Task :cli:ktlintTestSourceSetCheck
> Task :cli:checkKotlinGradlePluginConfigurationErrors SKIPPED
> Task :cli:processResources
> Task :cli:processTestResources NO-SOURCE
> Task :core:compileKotlin
> Task :core:detekt
> Task :core:loadKtlintReporters
> Task :core:runKtlintCheckOverKotlinScripts
> Task :core:ktlintKotlinScriptCheck
> Task :core:compileJava NO-SOURCE
> Task :core:classes UP-TO-DATE
> Task :core:jar
> Task :core:runKtlintCheckOverMainSourceSet
> Task :app:compileReleaseKotlin
> Task :app:compileDebugKotlin
> Task :app:compileReleaseJavaWithJavac NO-SOURCE
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

> Task :app:bundleReleaseClassesToRuntimeJar
> Task :app:bundleReleaseClassesToCompileJar
> Task :app:compileReleaseUnitTestKotlin
> Task :app:compileReleaseUnitTestJavaWithJavac NO-SOURCE
> Task :app:processReleaseJavaRes
> Task :app:processReleaseUnitTestJavaRes
> Task :app:lintAnalyzeDebug

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

> Task :app:lintReportDebug
Wrote HTML report to file:///home/runner/work/photo-to-glyph/photo-to-glyph/app/build/reports/lint-results-debug.html

> Task :app:lintDebug
> Task :app:lint
> Task :app:check
> Task :core:ktlintMainSourceSetCheck
> Task :core:runKtlintCheckOverTestSourceSet
> Task :core:ktlintTestSourceSetCheck
> Task :cli:compileTestKotlin
> Task :cli:compileTestJava NO-SOURCE
> Task :cli:testClasses UP-TO-DATE
> Task :core:compileTestKotlin

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
> Task :core:compileTestJava NO-SOURCE
> Task :core:processTestResources NO-SOURCE
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
> Task :tools:detekt NO-SOURCE
> Task :tools:loadKtlintReporters

> Task :tools:verifyFixtures
PASS fixture: tools/fixtures/synthetic/m0/1046.png
PASS fixture: tools/fixtures/synthetic/m0/1041.png
PASS fixture: tools/fixtures/synthetic/m0/1074.png
PASS fixture: tools/fixtures/synthetic/m0/1064.png
PASS fixture: tools/fixtures/synthetic/m0/1092.png
PASS fixture: tools/fixtures/synthetic/m0/manifest.json
PASS fixture: tools/fixtures/synthetic/golden/bar-20-input.png
PASS fixture: tools/fixtures/synthetic/golden/bar-20-expected.png

> Task :app:mergeDebugNativeDebugMetadata NO-SOURCE
> Task :tools:runKtlintCheckOverKotlinScripts
> Task :app:mergeDebugShaders
> Task :app:compileDebugShaders NO-SOURCE
> Task :app:generateDebugAssets UP-TO-DATE
> Task :tools:ktlintKotlinScriptCheck
> Task :app:mergeDebugAssets
> Task :tools:check
> Task :check
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
> Task :app:mergeExtDexRelease
> Task :app:lintVitalReportRelease SKIPPED
> Task :app:lintVitalRelease SKIPPED
> Task :app:mergeReleaseArtProfile
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
> Task :app:compileReleaseArtProfile
> Task :app:writeReleaseSigningConfigVersions
> Task :cli:startScripts
> Task :app:packageRelease
> Task :app:createReleaseApkListingFileRedirect
> Task :app:assembleRelease
> Task :app:assemble
> Task :app:build
> Task :cli:distTar
> Task :cli:distZip
> Task :cli:assemble
> Task :cli:build
> Task :core:assemble
> Task :core:build
> Task :build
> Task :tools:assemble UP-TO-DATE
> Task :tools:build

> Task :cli:smoke
INPUT: original synthetic 300x300 PNGs, 6 px strokes; not real handwriting
TIMING: PNG decode + core + SVG serialization/validation; excludes JVM startup/output writes; 2 warmups + 5 measured runs
char | strokes | closed | graph cycles | split chains | cold ms | median ms | max ms
Ж | 8 | 0 | 0 | 0 | 291.781 | 25.930 | 291.781
Б | 3 | 0 | 1 | 1 | 23.564 | 13.437 | 23.564
в | 3 | 0 | 2 | 0 | 12.958 | 13.129 | 14.882
Ш | 3 | 0 | 0 | 0 | 8.978 | 14.137 | 21.402
ф | 5 | 0 | 2 | 0 | 14.150 | 13.790 | 15.448
PASS smoke: 5/5 nonempty glyphs, zero closed polylines, explicit no-fill strokes, within budget
Outputs: /home/runner/work/photo-to-glyph/photo-to-glyph/cli/build/smoke/<codepoint>.svg, intermediate PNGs, metrics.csv
gradle/actions: Writing build results to /home/runner/work/_temp/.gradle-actions/build-results/verification-1792124191023.json

BUILD SUCCESSFUL in 4m 10s
142 actionable tasks: 142 executed
```

### Актуальный полный вывод CI run 38883213683

Captured from the successful GitHub Actions `Verification output` check-run `116915644023` on commit `2aedf07`.

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

## 3. Связь тестов с production-кодом

- `storesIndependentPartialSetsAcrossStoreInstances` вызывает `LocalGlyphSetStore.save`, `load` и `list`, а те — `GlyphSetJson.encode`/`decode`; проверяет сохранение двух неполных независимых наборов.
- `replacesAndDeletesOnlyTheRequestedSet` вызывает `save`, `load`, `list`, `delete`; проверяет замену/удаление адресного ID без изменения второго набора.
- `hashesIdsBeforeUsingThemAsFileNames` вызывает `save`, `load` и production `fileForId`; проверяет SHA-256-путь для ID с `../`.
- `malformedStoredJsonIsReportedInsteadOfSilentlyDropped` вызывает `list` и `readSet`; проверяет `IOException` при повреждённом JSON.
- `GlyphSetNormalizerTest.descendersUseTheSharedBaselineAndReferenceScale` вызывает production `normalizeGlyphSet`, который применяет raster-to-model Y-up transform и строит `GlyphSet`; проверяет три разные поданные baseline, единый baseline `0`, scale, x-height/cap-height, advance и descenders.
- `GlyphSetNormalizerTest.invalidLabelsAndMetricsAreRejected` вызывает `normalizeGlyphSet`, `RasterFontMetrics` и `GlyphSetNormalizationOptions`; проверяет отклонение повторных labels и недопустимой target cap-height.

UI-экран компилируется и проходит lint/ktlint в указанном CI; отдельного Compose UI/instrumentation-теста для его поведения пока нет.

## 4. Приёмка

| Проверено автоматически | Требует проверки человеком | Не сделано |
| --- | --- | --- |
| App-private JSON-хранилище сохраняет/читает/список/заменяет/удаляет разные partial sets; четыре production-based JUnit-теста прошли в Debug и Release. `normalizeGlyphSet` преобразует явно размеченные raster paths в общую Y-up систему с baseline `0`; два production-based core tests проверяют разные baseline, заданные reference heights, descenders, labels и ошибки метрик. Полная сборка, core/app/CLI tests, lint, formatting и synthetic CLI smoke в CI run `38883213683` зелёные. | Запуск на Android: camera-first старт, переход «Мои наборы» ↔ камера, пустой/непустой список, сохранение между перезапусками приложения и реальная камера. `adb`/`emulator` и Android SDK здесь не доступны; физическое устройство не проверено. | Пользовательское создание/именование/изменение наборов и символов; OCR/лейблы/диакритики; автоматическое определение baseline и reference metrics; sidebearing/slant normalization; pipeline→store save; editor; внешний экспорт; `.gfont` и проверка в KDraw. |

## 5. Предположения и неизвестное

- Для частного локального хранилища выбран каталог Android `filesDir/glyph-sets`; аккаунта и облачной синхронизации нет. Это не внешний backup: удаление приложения может удалить внутренние данные.
- Текущий JSON — внутренний snapshot существующего `GlyphSet` codec. Совместимость с будущим внешним экспортным форматом не предполагается; `.gfont` не создаётся.
- Для нормализации вызывающий код должен уже знать label, raster `originX` и baseline каждого glyph, x-height/cap-height reference metrics и target cap-height. Текущая функция проверяет и преобразует эти значения, но не распознаёт/не оценивает их. Её результат — core foundation, не end-to-end handwriting capture.
- Экран библиотеки read-only, так как OCR и UI создания/редактирования ещё не существуют. Пустой список — ожидаемый текущий сценарий.
- Реальная camera/UI/перезапуск-приёмка, реальные фотографии и KDraw не проверялись.

## 6. Запись CHANGELOG

Запись в `CHANGELOG.md` описывает оба выполненных foundation-среза: app-private локальное JSON-хранилище/read-only список и тестируемую core-нормализацию явно размеченных raster glyphs. Создание/редактирование, автоматическое распознавание/измерение, device QA и `.gfont` экспорт остаются незавершёнными.
