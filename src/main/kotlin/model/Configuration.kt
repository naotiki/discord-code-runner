package model

import data.DataSize
import data.DataSize.ByteUnit.*
import kotlinx.serialization.Serializable

@Serializable
data class Configuration(
    val runtime: RuntimeConfig = RuntimeConfig(),
    val session:SessionConfig = SessionConfig()
)

@Serializable
data class RuntimeConfig(
    val memory: DataSize = MEMORY,
    val nanoCpu: Long = NANOCPU,
    val pids: Long = PIDS,
    val diskQuota: DataSize = DISK_QUOTA_SIZE
) {
    companion object Default {
        private val MEMORY: DataSize = DataSize(1, GB)
        private const val NANOCPU: Long = 1000000000//1.0 CPU
        private const val PIDS: Long = 256
        private val DISK_QUOTA_SIZE: DataSize = DataSize(256, MB)
    }
}

@Serializable
data class SessionConfig(
    val prepareMillis: Long = 0,
    val compileMillis: Long = COMPILE_TIMEOUT_MILLIS,
    val executeMillis: Long = EXECUTE_TIMEOUT_MILLIS
) {

    companion object Default {
        private const val COMPILE_TIMEOUT_MILLIS: Long = 120 * 1000
        private const val EXECUTE_TIMEOUT_MILLIS: Long = 10 * 1000
    }
}
