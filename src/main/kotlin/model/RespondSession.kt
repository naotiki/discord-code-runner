package model

import kotlinx.serialization.Serializable

@Serializable
data class RespondSession(
    val sessionId: String,
    val runtimeData: RuntimeData
){
}
