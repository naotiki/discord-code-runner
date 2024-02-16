package data

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = DataSizeSerializer::class)
@JvmInline
value class DataSize(val bytes: Long) {

    constructor(size: Long, unit: ByteUnit) : this(size * unit.toLong())


    enum class ByteUnit {
        B,
        KB,
        MB,
        GB;

        // 1024^ordinal
        internal fun toLong(): Long {
            return 1L shl (10 * ordinal)
        }
    }

    override fun toString(): String {
        var byte = bytes
        var unit = ByteUnit.B
        for (entry in ByteUnit.entries) {
            if (bytes % entry.toLong() != 0L || bytes == 0L) {
                break
            }
            byte = bytes / entry.toLong()
            unit = entry
        }
        return "$byte${unit}"
    }

    companion object {
        fun fromStringOrNull(str: String): DataSize? {
            return "([0-9]+)(\\w+)".toRegex().find(str)?.run {
                val (size, unit) = destructured
                return@run ByteUnit.entries.singleOrNull { unit == it.name }?.let {
                    size.toLongOrNull()?.let { it1 -> DataSize(it1, it) }
                }
            }
        }
        fun fromString(str: String) = fromStringOrNull(str)!!


    }
}

object DataSizeSerializer : KSerializer<DataSize> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("DataSize", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: DataSize) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): DataSize {
        return DataSize.fromString(decoder.decodeString())
    }

}
