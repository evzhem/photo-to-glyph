# M3 — раздельные заглавные и строчные формы (частичный результат)

Дата: **2026-10-17**. Статус: **пользователь может отдельно выбрать и собрать прописные и строчные glyphs, но все crops и подписи по-прежнему ручные**. Проверка на ветке `arena/6d0fb259-photo-to-glyph`, commit `9761a88eb6fd6ce8b1e7a8695c17f1f72f88d8c6`; GitHub Actions run [39052628653](https://github.com/evzhem/photo-to-glyph/actions/runs/39052628653) прошёл. Это CI, не физическая приёмка UI на Android.

## 1. Файлы и назначение

- `app/src/main/kotlin/photoglyph/app/AlphabetLetterForms.kt` — production catalog русских (33 пары, включая Ё/ё) и латинских (26 пар) букв, а также функция отдельного подсчёта заглавных/строчных форм.
- `app/src/main/kotlin/photoglyph/app/AlphabetLetterPicker.kt` — кириллическая/латинская вкладка, отдельные ряды заглавных и строчных чипов, индикаторы прогресса; уже добавленные символы отмечены и повторно выбрать их нельзя.
- `app/src/main/kotlin/photoglyph/app/AlphabetBuilderScreen.kt` — выбранный символ предварительно подставляется в review crop; `А` и `а` сохраняются в один `GlyphSet` как разные ключи. Их можно также вручную заменить в поле метки.
- `app/src/test/kotlin/photoglyph/app/AlphabetLetterFormsTest.kt` — три JUnit-проверки production catalog/progress: Ё/ё, раздельный uppercase/lowercase progress и 26 букв латиницы.
- `core/src/test/kotlin/photoglyph/core/GlyphSetChecks.kt` — production JSON round-trip теперь проверяет, что `А` и `а` остаются разными записями.
- `core/src/test/kotlin/photoglyph/core/GfontCodecChecks.kt`, `GfontRoundTripTest.kt` — production codec тестирует разные decimal Unicode ZIP entries `1040` и `1072` для пары `А`/`а`.
- `app/src/main/res/values/strings.xml`, `values-ru/strings.xml`, `README.md`, `ROADMAP.md`, `CHANGELOG.md`, `AGENTS.md`, `docs/01-требования-mvp.md` — UI-подсказки и статус.

## 2. Полный вывод build / tests / lint / smoke

Workflow command: `./gradlew --console=plain --no-daemon build :core:test :cli:smoke`. Все строки captured output из check-run `117454741364` сохранены; единственный завершающий пробел в строке Gradle `Daemon will be stopped at the end of the build` удалён, чтобы `git diff --check` проходил.

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
> Task :core:compileKotlin
> Task :core:compileJava NO-SOURCE
> Task :core:classes UP-TO-DATE
> Task :core:jar
> Task :cli:runKtlintCheckOverMainSourceSet
> Task :app:compileReleaseKotlin
> Task :app:compileReleaseJavaWithJavac NO-SOURCE
> Task :app:compileDebugKotlin
> Task :app:compileDebugJavaWithJavac NO-SOURCE
> Task :app:bundleDebugClassesToCompileJar
> Task :app:bundleReleaseClassesToRuntimeJar
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

AlphabetLetterFormsTest > cyrillicCapitalAndSmallLettersAreSeparateFormsIncludingYo PASSED

AlphabetLetterFormsTest > coverageCountsUppercaseAndLowercaseIndependently PASSED

AlphabetLetterFormsTest > latinAlphabetProvidesSeparateTwentySixLetterCases PASSED

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

AlphabetLetterFormsTest > cyrillicCapitalAndSmallLettersAreSeparateFormsIncludingYo PASSED

AlphabetLetterFormsTest > coverageCountsUppercaseAndLowercaseIndependently PASSED

AlphabetLetterFormsTest > latinAlphabetProvidesSeparateTwentySixLetterCases PASSED

LocalGlyphSetStoreTest > hashesIdsBeforeUsingThemAsFileNames PASSED

LocalGlyphSetStoreTest > malformedStoredJsonIsReportedInsteadOfSilentlyDropped PASSED

LocalGlyphSetStoreTest > storesIndependentPartialSetsAcrossStoreInstances PASSED

LocalGlyphSetStoreTest > replacesAndDeletesOnlyTheRequestedSet PASSED

> Task :app:test
> Task :cli:ktlintMainSourceSetCheck
> Task :cli:runKtlintCheckOverTestSourceSet
> Task :cli:ktlintTestSourceSetCheck
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

> Task :app:lintAnalyzeDebug

> Task :cli:test

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

BinaryVectorizeTest > preparedOnePixelLineProducesAnOpenCenterlineWithoutASecondThresholdPass PASSED

BinaryVectorizeTest > defaultBinaryPathPreservesSmallDetachedMarks PASSED

CoreSourcePolicyTest > actualProductionSourcesHaveNoAndroidImports PASSED

CoreSourcePolicyTest > ignoresNestedCommentsAndStringExamples PASSED

CoreSourcePolicyTest > rejectsQualifiedWildcardAndAliasedAndroidImports PASSED

GfontRoundTripTest > supplementaryPlaneCharactersAreRejectedByTheDocumentedUint16Field PASSED

GfontRoundTripTest > uppercaseAndLowercaseRemainDistinctGfontEntries PASSED

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
> Task :app:mergeLibDexDebug
> Task :app:mergeExtDexDebug
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
Ж | 8 | 0 | 0 | 0 | 292.151 | 17.399 | 292.151
Б | 3 | 0 | 1 | 1 | 15.029 | 11.889 | 15.029
в | 3 | 0 | 2 | 0 | 11.010 | 10.905 | 12.286
Ш | 3 | 0 | 0 | 0 | 9.600 | 7.907 | 9.600
ф | 5 | 0 | 2 | 0 | 9.829 | 9.562 | 10.432
PASS smoke: 5/5 nonempty glyphs, zero closed polylines, explicit no-fill strokes, within budget
Outputs: /home/runner/work/photo-to-glyph/photo-to-glyph/cli/build/smoke/<codepoint>.svg, intermediate PNGs, metrics.csv

> Task :app:packageRelease
> Task :app:createReleaseApkListingFileRedirect
> Task :app:assembleRelease
> Task :app:assemble
> Task :app:build
> Task :build
gradle/actions: Writing build results to /home/runner/work/_temp/.gradle-actions/build-results/verification-1792257075336.json

BUILD SUCCESSFUL in 2m 45s
142 actionable tasks: 142 executed

```

## 3. Какие production-функции вызвали тесты

- `AlphabetLetterFormsTest.cyrillicCapitalAndSmallLettersAreSeparateFormsIncludingYo`, `coverageCountsUppercaseAndLowercaseIndependently` и `latinAlphabetProvidesSeparateTwentySixLetterCases` вызывают production `AlphabetLetterForms.uppercase` / `lowercase` / `progress`; в app Debug и Release прошли все три новых теста.
- `GlyphSetRoundTripTest` → production `GlyphSetJson.encode`/`decode`: synthetic set хранит `А` и `а` одновременно и round-trip оставляет две записи.
- `GfontRoundTripTest.uppercaseAndLowercaseRemainDistinctGfontEntries` → production `GfontCodec.encode`/`decode`: проверяет decimal ZIP entries `1040`/`1072` и разные codepoint keys.
- Полный CI также прогнал `BinaryVectorizeTest`, codec/store/normalizer tests, CLI API tests, 21 legacy core checks, Debug/Release builds, Android lint/detekt/ktlint и CLI smoke. Compose UI gestures не запускались на эмуляторе/телефоне.

## 4. Приёмка

| Критерий | Проверено автоматически | Требует проверки человеком | Не сделано |
| --- | --- | --- | --- |
| Отдельно собирать прописную и строчную форму одного символа | Catalog/progress tests; JSON round-trip и `.gfont` round-trip сохраняют `А`/`а` как отдельные ключи | Нажать чипы и пройти полный page → crop → review → save → export flow на Android | Автоматически распознать регистр с изображения |
| Выбирать русскую или латинскую раскладку | Списки содержат 33 пары кириллицы (с Ё/ё) и 26 пар латиницы; независимый progress | Проверить читаемость/удобство чипов на реальном размере телефона | OCR и сопоставление страницы со списком букв |
| Повторно не добавить уже сохранённую форму | Сохранённый glyph отображается с галочкой и его чип выключен; core проверяет раздельные codepoints | Проверить эти состояния реальным касанием | Автоматически заполнить отсутствующие формы |
| Сохранить неполный набор | Существующая модель/flow не требуют полного списка; новые чипы лишь показывают progress | Убедиться на устройстве, что пользователь может сохранить выбранную часть | Автодобавление пропусков или case-folding |

## 5. Допущения и неизвестные

- «Отдельно» означает два самостоятельных glyph-entry в одном partial `GlyphSet`/`.gfont`: например, `А` и `а` — разные Unicode codepoints. Символы не смешиваются и не заменяют друг друга.
- Picker покрывает стандартную кириллицу и базовую латиницу; остальные допустимые BMP-символы (цифры/пунктуация и т. п.) можно ввести вручную в review поле. Supplementary-plane символы всё ещё не поддерживаются документированным `uint16` `.gfont` subset.
- Переключатель script не определяет язык по фото. Пользователь выбирает правильную форму, сам вырезает соответствующую букву и подтверждает метку; connected handwriting не сегментируется.
- Нормализация по-прежнему требует вручную выбрать cap-height и x-height references и baseline. UI/удобство нового picker-а не проверялись физически на Android. Масштаб `.gfont`/KDraw остаётся неизвестным.

## 6. Запись CHANGELOG

`M3 — 2026-10-17`: Russian/Latin case picker with independent uppercase/lowercase progress; separate `А`/`а` persistence and `.gfont` record tests; three production catalog app tests. CI run `39052628653` passed; manual crop/label only, not OCR.
