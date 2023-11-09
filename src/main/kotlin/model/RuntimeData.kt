package model

import kotlinx.serialization.Serializable

@Serializable
data class RuntimeData(
    val id:String,
    val name:String,
    val sourcefileName:String,
    val extension:String,
    //言語識別用 デフォルトは言語名と拡張子のlowercase
    val alias:List<String> = listOf(name.lowercase(),extension.lowercase()),
    val metaData: MetaData,
    val commands: Commands
)

@Serializable
data class MetaData(
    //言語バージョン
    val version:String,
    //処理系 (JVMなど)
    val processor:String,
)
@Serializable
data class Commands(
    val prepare:String? = null,
    val compile:String?,
    val execute:String
)
