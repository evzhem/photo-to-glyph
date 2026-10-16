# Уточнённый пользовательский сценарий PhotoGlyph

Дата: 2026-10-13. Требования уточнены с пользователем после технического спайка M0. Это фиксация продукта, **не утверждение, что функции уже реализованы**, и не разрешение начинать M1.

## Главный результат

Пользователю нужны **файлы собственных наборов почерка**, а не редактор страниц для печати.

Целевой сценарий:

1. При запуске Android-приложение сразу открывает камеру после разрешения; пользователь фотографирует рукописную страницу. Если камера недоступна или разрешение не дано, можно выбрать фото вручную.
2. Найти отдельные буквы, определить соответствующие символы и получить их осевые штрихи.
3. Проверить результат, исправить неверно определённые символы или штрихи при необходимости.
4. Сохранить набор под своим названием.
5. Открыть «Мои алфавиты», выбрать набор и экспортировать его отдельным `.gfont` для KDraw.

Произвольный рукописный текст — запрошенный основной сценарий. Он не должен требовать обязательного заполнения специальной сетки. Генератор сетки из первоначального плана может служить дополнительным надёжным способом ввода; полное удаление этого ранее обязательного компонента не согласовывалось.

## Неполные наборы разрешены

- В приложении можно иметь несколько независимых наборов.
- Каждый набор содержит **только найденные и подтверждённые буквы**. Полный алфавит на одном фото не требуется.
- Отсутствующий символ не блокирует сохранение/экспорт неполного набора.
- Приложение не выдумывает недостающие буквы, не рисует их чужим почерком и не дополняет набор скрытыми заменами.
- Индикатор покрытия может сообщать, какие символы есть; это информация, а не требование заполнить весь алфавит.
- Как выбирать один вариант из нескольких повторений одинаковой буквы на фото, предстоит согласовать при проектировании разметки. Автоматическое восстановление движения пера и безошибочное распознавание слитного почерка не обещаются.

## Подстановка принадлежит KDraw

Пользователь сообщил, что программа плоттера умеет брать отсутствующие буквы из другого набора, и явно выбрал **KDraw / программу плоттера** владельцем этого поведения.

Поэтому PhotoGlyph:

- хранит и экспортирует каждый набор отдельно;
- **не** выбирает основной/резервные наборы для автоматического объединения;
- **не** собирает смешанный `.gfont` из других наборов;
- **не** добавляет выдуманные служебные ссылки на другие архивы в формат `.gfont`;
- **не** управляет подстановкой или железом плоттера.

Это подтверждённое пользовательское требование, но поведение конкретной версии KDraw ещё не проверено нами. В M9 нужно отдельно проверить установку нескольких наших неполных наборов и подстановку на тексте, покрываемом их объединением. Названия переключателей/порядок резервных наборов в интерфейсе KDraw неизвестны и не угадываются.

## Хранение — локально, без аккаунта

Пользователь выбрал хранение **на телефоне**:

- библиотека именованных наборов сохраняется между запусками приложения;
- каждый набор можно экспортировать отдельным файлом в выбранное место на телефоне или передать файлом;
- аккаунт, облачная библиотека и синхронизация между устройствами не входят в запрошенный MVP.

Сохранение внутри приложения и внешний экспорт — разные вещи. Внутренняя библиотека не является гарантией сохранности после удаления приложения; пользователь должен иметь возможность сохранить внешний файл. После этой фиксации добавлены app-private JSON snapshots, unit-тесты и read-only список сохранённых наборов (см. `docs/M5-report.md`). UI для создания/редактирования и внешний экспорт ещё не реализованы.

## Что не входит в основной пользовательский сценарий

Композиция текста на A4, редактор страниц, печать страницы, анимация написания и прямое управление плоттером не нужны для запрошенного результата «получить свои файлы». Предыдущие M6/M7 необходимо пересмотреть до их реализации; внутренняя SVG-диагностика и тест no-fill остаются нужны для качества ядра. Возможность исправить штрихи не отменяется.

## Влияние на план и текущий статус

- M0 уже проверяет получение осевых штрихов на синтетических **заранее вырезанных** буквах. Он **не** распознаёт текст с фотографии тетради и не содержит Android-приложения.
- До реализации ввода/разметки нужно пересогласовать M2–M3: свободный текст требует иной сегментации и распознавания символов, чем назначение буквы по позиции в сетке.
- Не выбирать распознаватель, модель, обучающие данные или новые зависимости наугад. Сохраняются запреты на нативную обработку/несогласованные зависимости, лицензионные правила и запрет заливок.
- Оценка 31–43 человеко-дня относится к **первоначальному** плану. Она не является подтверждённой оценкой уточнённого режима произвольного почерка. Новая оценка не выдумывается.
- Минимальная версия Android (предложение API 26), канал публикации и неизвестные параметры `.gfont` пока не подтверждены.
- На момент первоначальной фиксации M1 ещё не начался; с тех пор он завершён. Пользователь позже явно разрешил последовательно продолжать оставшиеся этапы без промежуточного подтверждения (см. актуальные `AGENTS.md` и `ROADMAP.md`).

## Дополнения после M1 — 2026-10-15

Исторические пункты выше описывают момент уточнения после M0. M1 прошёл CI. Android-приложение при запуске запрашивает доступ и сразу открывает полноэкранную камеру; при отказе доступен импорт фото. M2 содержит подготовку фото (обрезка/поворот, выравнивание и ч/б), но не готовое распознавание страницы.

Пользователь подтвердил основной сценарий: сфотографировать страницу с рукописными буквами, определить буквы и выдать алфавит/набор почерка из присутствующих символов. Набор может быть неполным; подстановку отсутствующих букв выполняет KDraw. Конкретный layout страницы, способ сегментации/распознавания и критерии точности предстоит спроектировать; распознавание не объявляется уже реализованным.

Пользователь также уточнил отдельную функцию: по фото изображения выдавать картинку с чёрными контурами. Это не конвейер букв и не осевые штрихи. Формат результата, фон и дополнительные действия не подтверждены; прежние предложения о SVG/PNG, копировании, скачивании и отправке не считать утверждёнными. В рабочей ветке добавлены отдельный Sobel/NMS core-алгоритм, Compose preview и синтетические production-function tests. Полный GitHub Actions Gradle/Android CI прошёл в run `38799609923`, включая новые InkComponents и JSON model tests; проверка реальных фото/устройства ещё не выполнена. Белый raster backing — инженерный выбор реализации, не пользовательское требование. Подробности: `05-рисунок-по-контуру.md`.

## Обновление основы M3 — 2026-10-16

Core теперь возвращает детерминированные 8-связные компоненты чернил и pixel-exact crops; `GlyphSet`/`Glyph`/`Stroke` поддерживают неполные наборы с JSON round-trip проверкой. Тесты подтверждают только эти операции pure core: они не распознают символы, не группируют курсивные буквы или отдельные диакритические отметки и не реализуют экран исправления/локальную Android-библиотеку. Полный Gradle/Android CI run `38799609923` прошёл. Распознавание страницы пока не реализовано.

## Дополнение — Android-хранилище и core-нормализация M5, 2026-10-16

Добавлен app-private file store для отдельных `GlyphSet` JSON snapshots и read-only Compose-экран списка с переходом из camera-first UI. Четыре JUnit-теста обращаются к production store/codec и прошли в Debug/Release в GitHub Actions run `38849746285`.

Production `normalizeGlyphSet` теперь преобразует **вручную размеченные** raster glyph paths: вызываетющий код передаёт label, originX/baseline каждого glyph, x-height/cap-height reference и target cap-height. Функция отображает координаты Y-down → Y-up, общий baseline `0` и общий scale; две core-проверки вызывают production API и прошли в CI run `38883213683`. Это не OCR и не автоматическая разметка или оценка метрик.

UI пока не создаёт и не редактирует наборы, не получает распознанные буквы из pipeline и не экспортирует `.gfont`; real Android/device QA ещё не выполнен. Подробный частичный отчёт и полный CI output: `docs/M5-report.md`.

## Статус проверки M2 — 2026-10-16

`./tools/m2-standalone-check.sh` прошёл: 17 page-preparation core checks, 5 preparation API checks и supporting suites. Full Gradle/Android CI run `38883213683` прошёл и компилирует app. Ни permission/capture, ни CameraX fallback, ни реальная подготовка фотографии не запускались на Android-устройстве/эмуляторе: здесь нет `adb`, `emulator` или Android SDK. M2 остаётся частичным; подробный отчёт с полным stdout/CI output: `docs/M2-report.md`. Следующий этап распознавания пока не начинается по порядку `ROADMAP.md`.

## Повторная проверка существующего спайка (историческая проверка M0)

Изменены только документы; новых production-функций или тестов не добавлено. Повторно проверен существующий M0, а не реализация новых требований. Сопоставление каждого теста с реальной функцией ядра приведено в строках `PASS … -> …` ниже и в разделе 3 `M0-report.md`. Gradle по-прежнему не настроен.

Фактическая команда (exit code 0):

```sh
set -o pipefail && ./tools/m0-check.sh 2>&1 | tee .m0-work/requirements-check.txt
```

Полный объединённый stdout/stderr, без сокращений:

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
Ж | 8 | 0 | 0 | 0 | 204.532 | 15.394 | 204.532
Б | 3 | 0 | 1 | 1 | 20.090 | 11.192 | 58.811
в | 3 | 0 | 2 | 0 | 17.785 | 13.847 | 19.339
Ш | 3 | 0 | 0 | 0 | 21.967 | 8.915 | 21.967
ф | 5 | 0 | 2 | 0 | 8.426 | 9.674 | 10.884
PASS smoke: 5/5 nonempty glyphs, zero closed polylines, explicit no-fill strokes, within budget
Outputs: /home/user/photo-to-glyph/.m0-work/<codepoint>.svg, intermediate PNGs, metrics.csv

=== Repository whitespace check ===
PASS git diff --check + git diff --cached --check
M0 CHECK PASSED (synthetic evidence only; Inkscape/device not verified)
```

Точная копия: `m0/requirements-verification.txt`. Исходные метрики и SVG в отчёте M0 остаются свидетельствами первоначального прогона; повторные замеры не являются новым golden или проверкой Android/KDraw.
