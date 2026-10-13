package photoglyph.core.export

/** Optional printable input, not glyph export: labels/marks can be filled for paper printing. */
data class GridSheet(
    val characters: List<String>,
    val columns: Int = 6,
    val widthMm: Double = 210.0,
    val heightMm: Double = 297.0,
    val marginMm: Double = 12.0,
    val gridTopMm: Double = 28.0,
    val cellHeightMm: Double = 23.0,
) {
    init {
        require(characters.isNotEmpty() && columns > 0)
        require(listOf(widthMm, heightMm, marginMm, gridTopMm, cellHeightMm).all { it.isFinite() && it > 0.0 })
        require(widthMm > marginMm * 2 && gridTopMm + rows * cellHeightMm <= heightMm - marginMm)
        require(characters.all { it.codePointCount(0, it.length) == 1 && it.codePoints().allMatch { code -> code >= SPACE_CODE && code != DELETE_CODE } })
    }
    val rows: Int get() = (characters.size + columns - 1) / columns
}

/** Pure independent sheet generator. SVG mm units and raster Y down are explicit. */
fun gridSheetSvg(sheet: GridSheet): String = buildString {
    val right = sheet.widthMm - sheet.marginMm
    val bottom = sheet.gridTopMm + sheet.rows * sheet.cellHeightMm
    val cellWidth = (sheet.widthMm - sheet.marginMm * 2) / sheet.columns
    append("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"${sheet.widthMm}mm\" height=\"${sheet.heightMm}mm\" viewBox=\"0 0 ${sheet.widthMm} ${sheet.heightMm}\">")
    append("<g fill=\"none\" stroke=\"black\" stroke-width=\"$GRID_STROKE_MM\">")
    for (column in 0..sheet.columns) {
        val x = sheet.marginMm + column * cellWidth
        append("<path d=\"M $x ${sheet.gridTopMm} L $x $bottom\"/>")
    }
    for (row in 0..sheet.rows) {
        val y = sheet.gridTopMm + row * sheet.cellHeightMm
        append("<path d=\"M ${sheet.marginMm} $y L $right $y\"/>")
    }
    append("</g><g fill=\"black\">")
    for ((x, y) in listOf(sheet.marginMm to sheet.marginMm, right to sheet.marginMm, right to (sheet.heightMm - sheet.marginMm), sheet.marginMm to (sheet.heightMm - sheet.marginMm))) {
        append("<rect x=\"${x - MARK_SIZE_MM / 2}\" y=\"${y - MARK_SIZE_MM / 2}\" width=\"$MARK_SIZE_MM\" height=\"$MARK_SIZE_MM\"/>")
    }
    append("</g><g font-family=\"sans-serif\" font-size=\"$LABEL_SIZE_MM\" fill=\"black\">")
    for ((index, char) in sheet.characters.withIndex()) {
        val x = sheet.marginMm + index % sheet.columns * cellWidth + LABEL_INSET_MM
        val y = sheet.gridTopMm + index / sheet.columns * sheet.cellHeightMm + LABEL_BASELINE_MM
        append("<text x=\"$x\" y=\"$y\">${escapeLabel(char)}</text>")
    }
    append("</g></svg>")
}

/** Data characters, not UI text. Grid use remains optional for notebook photographs. */
fun cyrillicGridCharacters(): List<String> = (CYRILLIC_UPPER + CYRILLIC_LOWER).map { it.toString() }

private fun escapeLabel(value: String): String = value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;")

private const val CYRILLIC_UPPER = "АБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯ"
private const val CYRILLIC_LOWER = "абвгдеёжзийклмнопрстуфхцчшщъыьэюя"
private const val GRID_STROKE_MM = 0.25
private const val MARK_SIZE_MM = 3.0
private const val LABEL_SIZE_MM = 3.2
private const val LABEL_INSET_MM = 1.5
private const val LABEL_BASELINE_MM = 4.0
private const val SPACE_CODE = 32
private const val DELETE_CODE = 127
