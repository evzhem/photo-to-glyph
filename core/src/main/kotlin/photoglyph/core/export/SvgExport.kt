package photoglyph.core.export

import java.io.ByteArrayInputStream
import java.util.Locale
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory
import org.xml.sax.SAXParseException
import org.xml.sax.helpers.DefaultHandler
import photoglyph.core.PipelineParams
import photoglyph.core.geom.Polyline

/** M0 raster-coordinate SVG (Y down). No fills, outlines, implicit closure or UI text. */
fun strokesToSvg(
    polylines: List<Polyline>,
    rasterWidth: Int,
    rasterHeight: Int,
    params: PipelineParams = PipelineParams(),
): String {
    require(rasterWidth > 0 && rasterHeight > 0)
    require(polylines.none { it.isClosed }) { "Split cyclic chains before M0 SVG export" }
    fun coordinate(value: Double): String = String.format(Locale.ROOT, "%.${params.svgDecimalPlaces}f", value)
    return buildString {
        appendLine("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
        appendLine(
            "<svg xmlns=\"$SVG_NAMESPACE\" width=\"$rasterWidth\" height=\"$rasterHeight\" " +
                "viewBox=\"0 0 $rasterWidth $rasterHeight\" fill=\"none\">",
        )
        for (path in polylines) {
            append("  <path d=\"")
            for ((index, point) in path.points.withIndex()) {
                append(if (index == 0) "M " else " L ")
                append(coordinate(point.x)).append(' ').append(coordinate(point.y))
            }
            if (path.points.size == 1) {
                // A round-capped zero-length segment renders an isolated pen dot.
                val point = path.points.single()
                append(" L ").append(coordinate(point.x)).append(' ').append(coordinate(point.y))
            }
            appendLine(
                "\" fill=\"none\" stroke=\"black\" stroke-width=\"${params.svgStrokeWidthPx}\" " +
                    "stroke-linecap=\"round\" stroke-linejoin=\"round\"/>",
            )
        }
        appendLine("</svg>")
    }
}

/**
 * Validate our strict stroke-only SVG dialect. Returns the number of stroke paths.
 * Used by the CLI, not only by tests. Styles, other painted elements and external
 * entities are forbidden, so explicit fill="none" cannot be overridden silently.
 */
fun requireStrokeOnlySvg(svg: String): Int {
    val factory = DocumentBuilderFactory.newInstance()
    factory.isNamespaceAware = true
    factory.isXIncludeAware = false
    factory.isExpandEntityReferences = false
    factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)
    factory.setFeature(DISALLOW_DOCTYPE_FEATURE, true)
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "")
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "")
    val builder = factory.newDocumentBuilder()
    builder.setErrorHandler(object : DefaultHandler() {
        override fun error(exception: SAXParseException) { throw exception }
        override fun fatalError(exception: SAXParseException) { throw exception }
    })
    val document = builder.parse(ByteArrayInputStream(svg.toByteArray(Charsets.UTF_8)))
    val root = document.documentElement
    require(root.localName == "svg" && root.namespaceURI == SVG_NAMESPACE)
    val elements = document.getElementsByTagName("*")
    var paths = 0
    for (index in 0 until elements.length) {
        val element = elements.item(index) as org.w3c.dom.Element
        require(element.namespaceURI == SVG_NAMESPACE)
        require(element.localName == "svg" || element.localName == "path") { "Non-stroke SVG element" }
        require(element.getAttribute("fill") == "none") { "Every element must explicitly disable fill" }
        require(!element.hasAttribute("style") && !element.hasAttribute("class")) { "Style overrides forbidden" }
        if (element.localName == "svg") {
            require(element === root) { "Nested SVG forbidden in our export" }
        } else {
            require(element.parentNode === root) { "Stroke paths must be direct root children" }
            val stroke = element.getAttribute("stroke").trim()
            require(stroke.isNotEmpty() && stroke != "none" && !stroke.startsWith("url(")) { "Missing stroke" }
            val strokeWidth = element.getAttribute("stroke-width").toDoubleOrNull()
            require(strokeWidth != null && strokeWidth.isFinite() && strokeWidth > 0.0) { "Invalid stroke width" }
            val data = element.getAttribute("d")
            require(data.startsWith("M ") && 'Z' !in data && 'z' !in data) { "Implicitly closed or missing path" }
            paths++
        }
    }
    return paths
}

private const val SVG_NAMESPACE = "http://www.w3.org/2000/svg"
private const val DISALLOW_DOCTYPE_FEATURE = "http://apache.org/xml/features/disallow-doctype-decl"
