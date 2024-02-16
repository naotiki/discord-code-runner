package command.text

import client
import command.builder.text.TextCommand
import dev.kord.common.Color
import dev.kord.core.behavior.edit
import dev.kord.core.behavior.reply
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.rest.builder.message.modify.embed
import io.ktor.client.call.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.launch
import model.RespondSession
import model.RunPhase
import model.RunPhase.*
import model.RunnerError
import model.RunnerEvent
import util.escapeCodeblocks

class RunTextCommand : TextCommand("run") {
    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun MessageCreateEvent.execute(body: String) {
//val result = "```(?<lang>(?!input).*)\\n(?<src>[\\s\\S]*)```".toRegex().find(body)
        val result = "```(?<lang>(?!input).*)\\n(?<src>(?:(?!```)[\\s\\S])*)```".toRegex().find(body) ?: return
        val ownMessage = message.reply { content = "お待ちください..." }
        val input = "```input\\n(?<body>[\\s\\S]*)```".toRegex().find(body)
        val inputBody = input?.groups?.get("body")?.value
        val lang = result.groups["lang"]?.value
        val src = result.groups["src"]?.value
        if (lang == null || src == null) {
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
        }.takeUnless {
            it.status == HttpStatusCode.NotFound
        }?.body<RespondSession>() ?: run {
            ownMessage.edit {
                content = null
                embed {
                    color = Color(0xff0000)
                    title = "言語エラー"
                    description = """非対応の言語です
                                |$prefix の形式を確認してください。
                            """.trimMargin()
                }
            }
            return
        }

        client.webSocket(host = "localhost", port = 8080, path = "/run/$sessionId") {

            requireNotNull(converter)
            var errorLog: String? = null
            var embedColor = Color(0x0f0f0f)
            var phase: RunPhase? = Prepare
            val logList = mutableListOf<RunnerEvent.LogBase>()
            var lastUpdated = System.currentTimeMillis()
            var job: Job? = null
            suspend fun editRunEmbed() {
                job?.cancel()
                job = launch {
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

                            synchronized(logList) {
                                logList.filter { it.phase == Compile }.ifEmpty { null }?.let {
                                    field {
                                        name = "コンパイルログ"
                                        value = """
                                        |```
                                        |${it.joinToString("") { it.data }.escapeCodeblocks().takeLast(1000)}
                                        |```
                                    """.trimMargin()
                                    }
                                }
                                logList.filter { it.phase == Execute }.ifEmpty { null }?.let {
                                    field {
                                        name = "実行ログ"
                                        value = """
                                        |```
                                        |${it.joinToString("") { it.data }.escapeCodeblocks().takeLast(1000)}
                                        |```
                                    """.trimMargin()
                                    }
                                }
                            }
                            errorLog?.let {
                                field {
                                    name = "エラー"
                                    value = """
                                        |```
                                        |${it.escapeCodeblocks().takeLast(1000)}
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
            try {
                while (true) {
                    val event = receiveDeserialized<RunnerEvent>()
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
                    if (System.currentTimeMillis() - lastUpdated >= 500 || event !is RunnerEvent.LogBase) {
                        println(System.currentTimeMillis() - lastUpdated)
                        kotlin.runCatching {
                            editRunEmbed()
                        }.onFailure {
                            it.printStackTrace()
                        }

                        lastUpdated = System.currentTimeMillis()
                    }

                }
            } catch (e: ClosedReceiveChannelException) {
                phase = null
                kotlin.runCatching {
                    editRunEmbed()
                }.onFailure {
                    it.printStackTrace()
                }
            } catch (_: Throwable) {

            }
        }

    }
}



