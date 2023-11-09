import dev.kord.common.Color
import dev.kord.core.behavior.edit
import dev.kord.core.behavior.reply
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.rest.builder.message.create.embed
import dev.kord.rest.builder.message.modify.embed
import io.ktor.client.call.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.serialization.*
import io.ktor.utils.io.*
import model.RespondSession
import model.RunPhase
import model.RunnerEvent

class RunTextCommand : TextCommand {
    override val prefix: String = pre("run")
    override suspend fun MessageCreateEvent.execute(body: String) {
        val ownMessage = message.reply { content = "お待ちください..." }

        val result = "```(?<lang>[a-z]+)\\n(?<src>[\\s\\S]*)```".toRegex().find(body)
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
        val (sessionId, runtimeData) = client.post("http://0.0.0.0:8080/run") {
            parameter("langAlias", lang)
            setBody(ByteReadChannel(src))
        }.body<RespondSession>()
        var compileLog: String? = null
        var execLog: String? = null
        client.webSocket(host = "0.0.0.0", port = 8080, path = "/run/$sessionId") {
            for (frame in incoming) {
                val event = converter?.deserialize<RunnerEvent>(frame) ?: throw IllegalStateException()
                when (event) {
                    is RunnerEvent.Log -> {
                        when (event.phase) {
                            RunPhase.Compile -> compileLog = compileLog?.plus(event.data) ?: event.data
                            RunPhase.Execute -> execLog = execLog?.plus(event.data) ?: event.data
                            else -> {}
                        }
                    }

                    is RunnerEvent.ErrorLog -> {
                        when (event.phase) {
                            RunPhase.Compile -> compileLog = compileLog?.plus(event.data) ?: event.data
                            RunPhase.Execute -> execLog = execLog?.plus(event.data) ?: event.data
                            else -> {}
                        }
                    }

                    else -> println(event)
                }

                ownMessage.edit {
                    content = null
                    embed {
                        title = runtimeData.name
                        color = when (event) {
                            is RunnerEvent.Abort -> Color(0xff0000)
                            is RunnerEvent.Finish -> if (event.phase == RunPhase.Execute) Color(0x00ff00) else Color(
                                0x0000ff
                            )
                            else -> Color(0x0000ff)
                        }
                        description = runtimeData.metaData.run { "$version ($processor)" }
                        compileLog?.let {
                            field {
                                name = "コンパイルログ"
                                value = """
                                    |```
                                    |${it.escapeCodeblocks()}
                                    |```
                                """.trimMargin()
                            }
                        }
                        execLog?.let {
                            field {
                                name = "実行ログ"
                                value = """
                                    |```
                                    |${it.escapeCodeblocks()}
                                    |```
                                """.trimMargin()
                            }
                        }
                        footer {
                            text = sessionId
                        }
                    }
                }
            }
        }
    }

}
