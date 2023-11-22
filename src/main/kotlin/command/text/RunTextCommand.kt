package command.text

import command.builder.text.TextCommand
import client
import dev.kord.common.Color
import dev.kord.core.behavior.edit
import dev.kord.core.behavior.reply
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.rest.builder.message.modify.embed
import escapeCodeblocks
import io.ktor.client.call.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.serialization.*
import kotlinx.serialization.ExperimentalSerializationApi
import model.RespondSession
import model.RunPhase
import model.RunPhase.*
import model.RunnerError
import model.RunnerEvent

class RunTextCommand : TextCommand("run") {
    @OptIn(ExperimentalStdlibApi::class, ExperimentalSerializationApi::class)
    override suspend fun MessageCreateEvent.execute(body: String) {
        val ownMessage = message.reply { content = "お待ちください..." }
//val result = "```(?<lang>(?!input).*)\\n(?<src>[\\s\\S]*)```".toRegex().find(body)
        val result = "```(?<lang>(?!input).*)\\n(?<src>(?:(?!```)[\\s\\S])*)```".toRegex().find(body)
        val input = "```input\\n(?<body>[\\s\\S]*)```".toRegex().find(body)
        val inputBody = input?.groups?.get("body")?.value
        val lang = result?.groups?.get("lang")?.value
        val src = result?.groups?.get("src")?.value
        if (result == null || lang == null || src == null) {
            ownMessage.edit {
                content = null
                embed {
                    color = Color(0xff0000)
                    title = "コマンド解析エラー"
                    description = """正しくコマンドを読み取れませんでした。
                                |$prefix の形式を確認してください。
                            """.trimMargin()
                }
            }
            return
        }

        val (sessionId, runtimeData) = client.post("http://localhost:8080/run") {
            parameter("langAlias", lang)

            setBody(MultiPartFormDataContent(
                formData {
                    append("src", src)
                    inputBody?.let {
                        append("input", it)
                    }
                }
            ))
        }.body<RespondSession>()

        client.webSocket(host = "localhost", port = 8080, path = "/run/$sessionId") {
            requireNotNull(converter)
            var errorLog: String? = null
            var embedColor = Color(0x0f0f0f)
            var phase: RunPhase? = Prepare
            val logList = mutableListOf<RunnerEvent.LogBase>()
            for (frame in incoming) {
                val event =
                    kotlin.runCatching { converter?.deserialize<RunnerEvent>(frame) }.onFailure { it.printStackTrace() }
                        .getOrNull()
                when (event) {
                    is RunnerEvent.LogBase -> {
                        logList.add(event)
                        logList.sortBy { it.id }
                    }

                    is RunnerEvent.Abort -> {
                        embedColor = Color(0xaf0000)
                        val errorText = when (event.error) {
                            is RunnerError.CmdError -> """
                                コマンドエラー
                                ${event.error.reason}
                                """.trimIndent()

                            is RunnerError.Timeout -> """
                                タイムアウトエラー
                                コマンドの実行時間を超過しました
                                """.trimIndent()
                        }.also {
                            """
                            フェーズ ${event.phase.name}
                            $it
                            """.trimIndent()
                        }
                        errorLog = errorLog?.plus(errorText) ?: errorText
                    }

                    is RunnerEvent.Start -> {
                        phase = event.phase
                        embedColor = Color(0x0000af)
                    }

                    is RunnerEvent.Finish -> {
                        if (event.phase == Execute) embedColor = Color(0x00af00)
                    }

                    else -> println(event)
                }
                println(event)

                kotlin.runCatching {
                    ownMessage.edit {
                        content = null
                        embed {
                            title = runtimeData.name
                            color = embedColor
                            description = runtimeData.metaData.run { "$version ($processor)" }
                            field {
                                name = "フェーズ"
                                value = when (phase) {
                                    Prepare -> "準備中"
                                    Compile -> "コンパイル中"
                                    Execute -> "実行中"
                                    null -> "終了"
                                }
                            }
                            logList.filter { it.phase == Compile }.ifEmpty { null }?.let {
                                field {
                                    name = "コンパイルログ"
                                    value = """
                                    |```
                                    |${it.joinToString("") { it.data }.escapeCodeblocks()}
                                    |```
                                """.trimMargin().takeIf { it.codePointCount(0,it.lastIndex)<=1024 }?:"文字数オーバー"
                                }
                            }
                            logList.filter { it.phase == Execute }.ifEmpty { null }?.let {
                                field {
                                    name = "実行ログ"
                                    value = """
                                    |```
                                    |${it.joinToString("") { it.data }.escapeCodeblocks()}
                                    |```
                                """.trimMargin().takeIf { it.codePointCount(0,it.lastIndex)<=1024 }?:"文字数オーバー"
                                }
                            }
                            errorLog?.let {
                                field {
                                    name = "エラー"
                                    value="""
                                    |```
                                    |${it.escapeCodeblocks()}
                                    |```
                                """.trimMargin().takeIf { it.codePointCount(0,it.lastIndex)<=1024 }?:"文字数オーバー"
                                }
                            }
                            footer {
                                text = sessionId
                            }
                        }
                    }
                }.onFailure {
                    it.printStackTrace()
                }
            }
        }
    }

}
