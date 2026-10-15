package photoglyph.core.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import photoglyph.core.geom.Polyline
import photoglyph.core.geom.Vec2

/** Keeps optional JSON persistence concerns out of the geometry/vectorization API. */
internal object PolylineSerializer : KSerializer<Polyline> {
    override val descriptor: SerialDescriptor = PolylinePayload.serializer().descriptor

    override fun serialize(
        encoder: Encoder,
        value: Polyline,
    ) {
        val payload = PolylinePayload(value.points.map { Vec2Payload(it.x, it.y) })
        PolylinePayload.serializer().serialize(encoder, payload)
    }

    override fun deserialize(decoder: Decoder): Polyline {
        val payload = PolylinePayload.serializer().deserialize(decoder)
        return Polyline(payload.points.map { Vec2(it.x, it.y) })
    }
}

@Serializable
private data class PolylinePayload(
    val points: List<Vec2Payload>,
)

@Serializable
private data class Vec2Payload(
    val x: Double,
    val y: Double,
)
