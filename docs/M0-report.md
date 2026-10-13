# M0 — спайк «фото → штрих»

Дата: **2026-10-13**. Вход: **только собственная синтетика**, не фотографии реального почерка. Этапы M1–M10 не начаты.

**Решение: B — осевые линии получаются, но нужны дополнительная предобработка и очистка скелета/узлов.** На чистых входах нет двойного обвода или заливок; вместе с тем пересечение «Ж» распалось на восемь цепочек вместо трёх исходных рисованных линий, а диагонали ступенчатые. Это основание продолжать исследование скелетонизации, а не объявлять production-качество. Переход к M1 — только после подтверждения пользователя. Полная человеческая приёмка M0 пока не закрыта: SVG в Inkscape не проверялся.

## 1) Что сделано: файлы и содержимое

### Документы первым коммитом

Первый проектный коммит: `58e3e4e` — `M0: establish project specifications and milestone roadmap`.

- `AGENTS.md` — запреты, архитектура, неизменяемые контракты, алгоритмы, параметры, тесты, Definition of Done и протокол одного этапа. В M0 дополнены явно отмеченные соглашения пиксельного графа.
- `ROADMAP.md` — M0–M10 с критериями и оценками, итог 31–43 человеко-дня; M0 ожидает человеческой приёмки, прочие этапы не начаты.
- `README.md` — продукт, назначение, честный текущий статус и команды воспроизводимого запуска.
- `docs/04-формат-gfont.md` — установленные бинарные факты и отдельно неизвестные единицы/служебные записи/спутник.
- `docs/02-устройство-хорус-3-pro.md` — граница интеграции через KDraw и `gcodefonts`, порядок аппаратной проверки в M9.
- `LICENSE` — MIT; `CHANGELOG.md` — запись M0; `.editorconfig` — форматирование; `.gitignore` — Gradle/Android/IDE, локальные инструменты и результаты, запрет секретов, сторонних наборов и фото пользователей.

### Настоящее Kotlin-ядро (без Gradle на этапе M0)

Все перечисленные файлы находятся в `core/src/main/kotlin/photoglyph/core/`:

| Файл | Реальная функция / назначение |
| --- | --- |
| `PipelineParams.kt` | Порог 25×25 / offset 10, компоненты <12 px, pruning <2 px / 2 прохода, RDP ε=0.6 px; SVG и benchmark-параметры с именованными дефолтами |
| `img/Raster.kt` | Неизменяемые `ArgbImage`, `GrayImage`, `BinaryGrid`, защитное копирование массивов, соглашение о фоне за границей |
| `img/Binarization.kt` | `toGrayscale` (BT.601 + alpha на белом), `adaptiveThreshold` через интегральное изображение, `removeSmallComponents` по 8-связности |
| `thin/ZhangSuen.kt` | `zhangSuen`: две подытерации, одновременное удаление пикселей, до фиксированной точки; `SkeletonPixels` |
| `geom/Geometry.kt` | Контракты `Vec2`, `Polyline`; итеративный `simplifyRdp` |
| `graph/SkeletonToPolylines.kt` | `skeletonToPolylines`: граф, короткие терминальные ветки, однократный обход рёбер, сохранение петель через разбиение циклических цепочек, метрики, RDP |
| `export/SvgExport.kt` | `strokesToSvg` без заливок, явные stroke-атрибуты; реальный `requireStrokeOnlySvg`, используемый также CLI, запрещает fill/style-обходы и внешние XML entities |
| `Vectorize.kt` | Чистая `vectorize`, возвращающая промежуточные результаты для диагностики и тестов |

- `core/src/test/kotlin/photoglyph/core/M0Tests.kt` — 21 реальный тест, отдельная компиляция **против production JAR**, без копий обработки из ядра.
- `cli/src/main/kotlin/photoglyph/cli/Main.kt` — `smoke` и `vectorize`, декодирование/запись PNG через JVM ImageIO и измерение времени вне ядра.
- `tools/m0-check.sh` — реальная самостоятельная компиляция production и тестов, тестовый прогон, smoke и whitespace-проверки. Это **не** фиктивный `gradlew`.
- `tools/bootstrap-m0.py` — необязательная установка закреплённых по checksum локальных инструментов в игнорируемый `.cache/`; не зависимость продукта.
- `tools/generate_m0_fixtures.py` — собственный генератор PNG, только стандартная библиотека Python, воспроизводимый `--check`.
- `tools/fixtures/synthetic/README.md` — происхождение и объяснение независимого golden.
- `tools/fixtures/synthetic/m0/{1046,1041,1074,1064,1092}.png` — пять собственных геометрических букв 300×300, белый фон, штрих 6 px; `manifest.json` — параметры, исходные количества рисованных линий и SHA-256.
- `tools/fixtures/synthetic/golden/bar-20-{input,expected}.png` — вручную заданный 20×20 вход (12×3 прямоугольник) и ожидаемая девятипиксельная ось; ожидаемый PNG **не** вычисляется реализацией Zhang–Suen.
- `docs/M0-report.md` — этот шестичастный отчёт.
- `docs/m0/{1046,1041,1074,1064,1092}.svg`, `metrics.csv`, `verification.txt` — небольшие доказательства из реально выполненного финального прогона. Это **не golden-ответы тестов**.

JAR в `build/m0/`, локальные инструменты в `.cache/` и промежуточные PNG в `.m0-work/` не коммитятся. Нет OpenCV/NDK/JNI, кода OpenHand, чужих наборов `.gfont` или фото пользователей. `app/`, Gradle-wrapper и CI не созданы: это не начатый M1.

### Метрики по буквам

Среда: Linux x86_64, Temurin JVM 21.0.8, Kotlin/JVM 2.1.21, целевой байткод JVM 17. Первая колонка времени — холодный первый проход этого CLI-процесса; затем 2 прогревочных + 5 измеренных проходов на каждую букву, медиана пяти. Максимум включает холодный проход и пять измеренных проходов, но не прогревочные. Таймер включает PNG decode, ядро, построение и XML-валидацию SVG; не включает запуск JVM и запись результатов на диск. Это измерение в sandbox, **не Android-бенчмарк**.

| Буква | Штрихи (цепочки) | Замкнутые выходные полилинии | Циклический ранг графа | Разделённые замкнутые цепочки | Холодный, мс | Медиана, мс | Максимум, мс |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| Ж | 8 | 0 | 0 | 0 | 192.013 | 23.477 | 192.013 |
| Б | 3 | 0 | 1 | 1 | 26.793 | 13.521 | 26.793 |
| в | 3 | 0 | 2 | 0 | 15.665 | 13.148 | 21.542 |
| Ш | 3 | 0 | 0 | 0 | 18.403 | 11.285 | 18.403 |
| ф | 5 | 0 | 2 | 0 | 11.573 | 16.243 | 17.768 |

CSV: [m0/metrics.csv](m0/metrics.csv).

**Ноль замкнутых полилиний ≠ отсутствие естественных петель в букве.** У «Б», «в», «ф» в графе сохраняются циклы. Замкнутая цепочка «Б» делится на две открытые с общими концами; каждое ребро остаётся ровно один раз до RDP. У «в»/«ф» циклы уже состоят из открытых цепочек между узлами. Циклический ранг `E − V + C` считает независимые циклы графа после pruning, а не двойные контуры трассировки. SVG не содержит `Z`, заливок или двух границ толстого штриха. Отдельные точки трактуются как точки пера, не как контуры.

Ссылки на исходники и фактический экспорт:

| Буква | Синтетический источник | Осевые штрихи |
| --- | --- | --- |
| Ж | [PNG](../tools/fixtures/synthetic/m0/1046.png) | [SVG](m0/1046.svg) |
| Б | [PNG](../tools/fixtures/synthetic/m0/1041.png) | [SVG](m0/1041.svg) |
| в | [PNG](../tools/fixtures/synthetic/m0/1074.png) | [SVG](m0/1074.svg) |
| Ш | [PNG](../tools/fixtures/synthetic/m0/1064.png) | [SVG](m0/1064.svg) |
| ф | [PNG](../tools/fixtures/synthetic/m0/1092.png) | [SVG](m0/1092.svg) |

## 2) Вывод команд сборки и тестов целиком

Gradle не настроен: разрешённое исключение **до M1**. Команда `./gradlew build :core:test :cli:smoke` не запускалась, её успешный вывод не заявляется. Фактически выполнено:

```sh
set -o pipefail && ./tools/m0-check.sh 2>&1 | tee .m0-work/verification-final.txt
```

Exit code: **0**. Полный объединённый stdout/stderr (без сокращений):

```text
=== M0 standalone verification (no Gradle; M1 not started) ===
openjdk version "21.0.8" 2025-07-15 LTS
OpenJDK Runtime Environment Temurin-21.0.8+9 (build 21.0.8+9-LTS)
OpenJDK 64-Bit Server VM Temurin-21.0.8+9 (build 21.0.8+9-LTS, mixed mode)
info: kotlinc-jvm 2.1.21 (JRE 21.0.8+9-LTS)

=== Verify original synthetic fixtures ===
PASS fixture: tools/fixtures/synthetic/m0/1046.png
PASS fixture: tools/fixtures/synthetic/m0/1041.png
PASS fixture: tools/fixtures/synthetic/m0/1074.png
PASS fixture: tools/fixtures/synthetic/m0/1064.png
PASS fixture: tools/fixtures/synthetic/m0/1092.png
PASS fixture: tools/fixtures/synthetic/m0/manifest.json
PASS fixture: tools/fixtures/synthetic/golden/bar-20-input.png
PASS fixture: tools/fixtures/synthetic/golden/bar-20-expected.png

=== Compile production core + CLI (JVM target 17) ===
PASS production compilation

=== Compile tests against production JAR ===
PASS test compilation

=== Real core tests ===
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

=== CLI smoke: five original synthetic glyphs ===
INPUT: original synthetic 300x300 PNGs, 6 px strokes; not real handwriting
TIMING: PNG decode + core + SVG serialization/validation; excludes JVM startup/output writes; 2 warmups + 5 measured runs
char | strokes | closed | graph cycles | split chains | cold ms | median ms | max ms
Ж | 8 | 0 | 0 | 0 | 192.013 | 23.477 | 192.013
Б | 3 | 0 | 1 | 1 | 26.793 | 13.521 | 26.793
в | 3 | 0 | 2 | 0 | 15.665 | 13.148 | 21.542
Ш | 3 | 0 | 0 | 0 | 18.403 | 11.285 | 18.403
ф | 5 | 0 | 2 | 0 | 11.573 | 16.243 | 17.768
PASS smoke: 5/5 nonempty glyphs, zero closed polylines, explicit no-fill strokes, within budget
Outputs: /home/user/photo-to-glyph/.m0-work/<codepoint>.svg, intermediate PNGs, metrics.csv

=== Repository whitespace check ===
PASS git diff --check + git diff --cached --check
M0 CHECK PASSED (synthetic evidence only; Inkscape/device not verified)
```

Точная копия: [m0/verification.txt](m0/verification.txt). В скрипте компилятор работает с `-Werror -jvm-target 17`; production и тесты — два отдельных JAR. При ошибке тестов, smoke или whitespace скрипт завершается неуспешно, а `pipefail` сохраняет этот статус.

## 3) Каждый тест → реальная функция ядра

Все функции ниже — из `core/src/main/kotlin`; `vectorize` реально вызывает `toGrayscale → adaptiveThreshold → removeSmallComponents → zhangSuen → skeletonToPolylines → simplifyRdp`. В тестах нет своей реализации этих алгоритмов. ImageIO/конструкторы входов в test-коде лишь читают контролируемые PNG и задают ожидаемые координаты.

| Тест | Реальные функции |
| --- | --- |
| `GrayscaleTest.bt601` | `toGrayscale` |
| `GrayscaleTest.alphaOnWhite` | `toGrayscale` |
| `AdaptiveThresholdTest.localMeanAndOffset` | `adaptiveThreshold` |
| `AdaptiveThresholdTest.clippedBorderWindow` | `adaptiveThreshold` |
| `DespeckleTest.strict12pxBoundary` | `removeSmallComponents` |
| `DespeckleTest.eightConnectivity` | `removeSmallComponents` |
| `ZhangSuenTest.golden20px` | `zhangSuen` |
| `ZhangSuenTest.fixedPointAndBorder` | `zhangSuen` |
| `ZhangSuenTest.preserveLoop` | `zhangSuen, skeletonToPolylines` |
| `SkeletonToPolylinesTest.zheAtLeast3Open` | `vectorize, skeletonToPolylines` |
| `SkeletonToPolylinesTest.cornerAndDiagonal` | `skeletonToPolylines` |
| `SkeletonToPolylinesTest.cycleEdgeCoverage` | `skeletonToPolylines` |
| `SkeletonToPolylinesTest.pruneStrictlyBelow2px` | `skeletonToPolylines` |
| `SkeletonToPolylinesTest.dotAndEmpty` | `skeletonToPolylines, strokesToSvg, requireStrokeOnlySvg` |
| `RdpTest.handSpecifiedSimplification` | `simplifyRdp` |
| `RdpTest.zeroEpsilonAndShortPaths` | `simplifyRdp` |
| `SvgNoFillTest.fiveActualExports` | `vectorize, strokesToSvg, requireStrokeOnlySvg` |
| `SvgNoFillTest.rejectCorruptedExport` | `strokesToSvg, requireStrokeOnlySvg` |
| `SvgNoFillTest.rejectClosureAndUseDecimalPoint` | `strokesToSvg, requireStrokeOnlySvg` |
| `PipelineTest.deterministicAndImmutable` | `vectorize` |
| `PipelineTest.blankImage` | `vectorize, strokesToSvg, requireStrokeOnlySvg` |

В отрицательном SVG-тесте семь повреждений сделаны **после реального `strokesToSvg`**: fill корня/пути, отсутствие stroke, style-override, залитый circle, `Z`, DOCTYPE с внешней entity. Тест требует отказа реального `requireStrokeOnlySvg`; для обычного экспорта он требует успешной проверки. Проверка циклической цепочки сравнивает все восемь заранее заданных рёбер, отсутствие повторов/потерь и открытые выходные пути. Проверка pruning различает <2 и ровно 2 px.

`BaselineTest`, `GlyphSetRoundTripTest`, `GfontRoundTripTest` не добавлялись: соответствующих production-функций ещё нет. Обязательный source-scanning тест на `android.*` — задача M1; сейчас это правило соблюдается кодом, но CI не заявляется.

## 4) Таблица критериев приёмки M0

| Критерий | Статус | Доказательство / граница |
| --- | --- | --- |
| При отсутствии фотографий использовать пять собственных PNG 300×300 / 6 px | **проверено автоматически** | Генератор `--check`, manifest и SHA-256; синтетика отмечена явно |
| Вход → ч/б → истончение → полилинии → SVG для Ж/Б/в/Ш/ф | **проверено автоматически** | Реальное ядро, smoke 5/5, PNG-golden 20×20 |
| Число штрихов, замкнутых выходных контуров и время на каждую букву | **проверено автоматически** | Реальный `metrics.csv`, таблица выше, ноль замкнутых полилиний |
| Экспорт только штрихами `fill="none"` + stroke | **проверено автоматически** | `SvgNoFillTest`, реальный CLI-валидатор, отрицательные повреждения |
| SVG открывается **в Inkscape** как штрихи | **требует проверки человеком** | XML проверен, но Inkscape не запускался; конкретные SVG приложены |
| Честность оценки качества и решение A/B/C | **требует проверки человеком** | Отчёт и решение **B** написаны; основания перечислены ниже, окончательная приёмка — пользователь |
| Проверка фотографии реального почерка (не обязательна для разрешённого synthetic M0) | **не сделано** | Реальных фотографий нет; не подменяется синтетическим успехом |

### Как проверить SVG в Inkscape

Открыть каждый `docs/m0/<codepoint>.svg`. Выбрать отдельный path в «Объект → Заливка и обводка»: заливка отсутствует, обводка есть (1 px). В редакторе узлов должны быть осевые открытые пути, не пара внешнего/внутреннего контуров. Сравнить с соответствующим исходным PNG, особенно центр «Ж» и петли «Б»/«в»/«ф». При разбиении петли общие концы двух путей ожидаемы. Отметить визуальное качество отдельно от структурного no-fill.

### Что действительно ломается / не восстанавливается

- **«Ж»**: исходные 3 рисованные линии → 8 цепочек. Zhang–Suen даёт разнесённые точки ветвления вокруг пересечения; образуется центральный мост и короткая цепочка между узлами. Pruning удаляет только короткие *терминальные* ветки: эти межузловые рёбра он корректно не маскирует. Нужна обработка кластера узлов, а не ложная «идеальная» буква.
- **Диагонали и дуги**: дискретная ось после ε=0.6 всё ещё имеет ступеньки. Сглаживания M4 нет; увеличение ε без проверки геометрии не выдаётся за исправление.
- **«Б»**: петля сохранена двумя открытыми путями, плюс верхняя/вертикальная цепочка; дополнительный подъём пера — явная цена open-chain представления.
- **«в», «ф», «Ш»**: узнаваемый осевой рисунок, но количество цепочек (3/5/3) не равно исходным жестам письма (1/2/2). Порядок обхода детерминированный пиксельный, не порядок письма.
- Не реализованы перспектива, deskew, сетка, сегментация листа, подавление теней/плохого освещения, нормализация, сглаживание, TSP и ручной редактор. Это дальнейшие этапы, не скрытые функции M0.
- Синтетика однородна и геометрична. По ней нельзя подтвердить распознавание осмысленного штриха на реальной фотографии, долю ≥80% правильных production-штрихов или скорость телефона. Порог M4 в 2 с не нарушен в этом sandbox, но аппаратный Android-замер впереди.

**Выбор B:** сохранить centerline-подход, планировать дальнейшую предобработку и очистку узлов/геометрии в соответствующих этапах. A («качества достаточно») преждевременна даже на «Ж»; C (PNG-only) этими данными не обоснована и потребовала бы согласования изменения продукта. M0 сам не меняет MVP и не начинает M1.

## 5) Допущения и неизвестное

- Синтетические пути — геометрические приближения кириллических букв; это не чей-либо почерк. Исходных фото нет, фото у пользователя для M0 не запрашивались.
- M0 принимает **уже вырезанную букву** и считает её изображение выровненным. Не обещает обработку произвольного листа.
- Растровая геометрия имеет Y вниз и вершины в центрах пикселей `(x+0.5, y+0.5)`. SVG использует те же координаты. `Glyph`/`GlyphSet` пока не реализованы; их будущая Y-вверх модель и em-box-контракты не меняются.
- Удаление избыточных диагоналей при существующем ортогональном маршруте — отмеченное в коде соглашение M0. Сохраняет 8-связность, устраняет угловые треугольники, но не доказывает исходную топологию жестов письма.
- Разбиение замкнутой цепочки не удаляет петлю и не скрывает её в метриках. Точки растра сохраняются на этапе обхода; последующий RDP намеренно аппроксимирует их с заданным ε. Направление/порядок пера не восстановлены.
- Kotlin 2.1.21 / локальный OpenJDK используются как инструменты; загруженный compiler/runtime остаётся в игнорируемом кэше. npm/pip-зависимостей в продукте нет. MIT-код ядра зависит только от stdlib/JVM; JDK toolchain имеет свою GPLv2 + Classpath Exception лицензию, не является GPL-библиотекой проекта.
- `.gfont` в M0 **не реализован**. Единицы, кегль, служебные записи, спутник `.gfont.txt` и дополнительные ограничения остаются неизвестны; никаких «предположенных» файлов плоттера не создано. Round-trip и реальная проверка — M9.
- Перед соответствующими этапами нужны ответы пользователя: **API 26? APK в GitHub Releases или Google Play?** После исходного отчёта пользователь уточнил основной сценарий: обычная тетрадь без обязательной сетки, неполные отдельные наборы, локальное хранение и подстановка в KDraw (`01-требования-mvp.md`). Сегментация/распознавание этого режима ещё не выбраны и не реализованы. Вопросы масштаба/записей `.gfont` задаются с разрешённым реальным файлом перед M9.
- Ограничение платформы: работа и PR выполняются только в фиксированной ветке `arena/7c133358-photo-to-glyph`. Отдельная новая ветка M0 не создавалась; `main` не меняется. Это явно оговорённое отклонение от запроса отдельной ветки на каждый этап. Коммитов в текущем цикле — три: документы, спайк/тесты/отчёт, затем уточнение пользовательского сценария без изменения кода. Лимит 15 не превышен. Последующее уточнение неполных наборов, локального хранения и подстановки в KDraw зафиксировано в `01-требования-mvp.md`; оно не означает начала M1.

## 6) Запись в CHANGELOG.md

`[Unreleased] → M0 — 2026-10-13`:

- Establish the MIT license, engineering specification, full M0–M10 roadmap (31–43 person-days), README and repository hygiene rules in the first project commit.
- Document the known `.gfont` binary layout and KDraw / Хорус 3 Pro file-installation boundary, with explicit unresolved format and hardware questions.
- Add a pure Kotlin/JVM spike: BT.601 grayscale, clipped-window adaptive threshold, eight-connected despeckling, simultaneous-pass Zhang–Suen thinning, pixel-graph traversal, strict short-branch pruning, edge-preserving cycle splitting, RDP and explicit no-fill SVG export/validation.
- Add five original synthetic 300 × 300 / 6 px PNG fixtures («Ж», «Б», «в», «Ш», «ф»), a reproducible standard-library generator, provenance/hashes and an independently hand-specified 20 × 20 thinning golden.
- Add 21 real-core tests, compiled against the production JAR, and CLI smoke with per-glyph stroke/closed-path/cycle counts and cold/median/max timings. Reject deliberately corrupted filled/styled SVG and external entities.
- Add standalone pre-Gradle verification and an optional checksum-pinned, ignored local compiler/runtime bootstrap. No native processing libraries, copied project code, third-party font sets or product GPL dependencies.
- Record measured SVG/CSV evidence and complete command output in `docs/M0-report.md` and `docs/m0/`. Decision **B**: centerlines are viable on synthetic inputs, but junction cleanup/preprocessing is required; «Ж» fragments into eight chains and diagonals remain jagged. Real handwriting and Inkscape acceptance are not claimed.
- Stop after M0 for user review. M1/Gradle/Android/CI, normalization, editor, `.gfont` and hardware validation have not started.

---

**Остановка после M0.** Следующий этап — только после «да» пользователя; M0 не объявлен прошедшим Inkscape или реальные фото.
