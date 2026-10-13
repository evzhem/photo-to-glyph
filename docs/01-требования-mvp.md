# Уточнённый пользовательский сценарий PhotoGlyph

Дата: 2026-10-13. Требования уточнены с пользователем после технического спайка M0. Это фиксация продукта, **не утверждение, что функции уже реализованы**, и не разрешение начинать M1.

## Главный результат

Пользователю нужны **файлы собственных наборов почерка**, а не редактор страниц для печати.

Целевой сценарий:

1. Сфотографировать рукописный текст в обычной тетради или импортировать его фото.
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

Сохранение внутри приложения и внешний экспорт — разные вещи. Внутренняя библиотека не является гарантией сохранности после удаления приложения; пользователь должен иметь возможность сохранить внешний файл. Реальное Android-хранилище/экспорт ещё не реализованы.

## Что не входит в основной пользовательский сценарий

Композиция текста на A4, редактор страниц, печать страницы, анимация написания и прямое управление плоттером не нужны для запрошенного результата «получить свои файлы». Предыдущие M6/M7 необходимо пересмотреть до их реализации; внутренняя SVG-диагностика и тест no-fill остаются нужны для качества ядра. Возможность исправить штрихи не отменяется.

## Влияние на план и текущий статус

- M0 уже проверяет получение осевых штрихов на синтетических **заранее вырезанных** буквах. Он **не** распознаёт текст с фотографии тетради и не содержит Android-приложения.
- До реализации ввода/разметки нужно пересогласовать M2–M3: свободный текст требует иной сегментации и распознавания символов, чем назначение буквы по позиции в сетке.
- Не выбирать распознаватель, модель, обучающие данные или новые зависимости наугад. Сохраняются запреты на нативную обработку/несогласованные зависимости, лицензионные правила и запрет заливок.
- Оценка 31–43 человеко-дня относится к **первоначальному** плану. Она не является подтверждённой оценкой уточнённого режима произвольного почерка. Новая оценка не выдумывается.
- Минимальная версия Android (предложение API 26), канал публикации и неизвестные параметры `.gfont` пока не подтверждены.
- M1 не начат; переход к следующему этапу остаётся за отдельным подтверждением пользователя.

## Повторная проверка существующего спайка

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
