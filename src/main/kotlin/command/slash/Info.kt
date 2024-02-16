package command.slash

import client
import command.builder.slash.Opt
import command.builder.slash.SlashCommandWithGroup
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.interaction.ChatInputCommandInteraction
import dev.kord.rest.builder.message.modify.embed
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.coroutines.runBlocking
import model.Configuration
import model.RuntimeData
import util.ApiCache.Companion.cache

class Info : SlashCommandWithGroup("info", "らんな〜情報") {
    override val groups: Array<CommandGroup>
        get() = arrayOf(Host(), Runtime())

    inner class Host : CommandGroup("host", "実行ホストの情報") {
        override val subCommands: Array<SubCommand>
            get() = arrayOf(Limit())

        inner class Limit : SubCommand("limit", "実行制限を取得します") {
            override suspend fun ChatInputCommandInteraction.onExecute() {
                deferPublicResponse().respond {
                    client.get("http://localhost:8080/config").body<Configuration>().run {
                        embed {
                            title = "実行制限"
                            field {
                                name = "メモリ"
                                value = runtime.memory.toString()
                            }
                            field {
                                name = "CPU"
                                value = runtime.nanoCpu.div(1000000000.0).toString() + " 論理CPU"
                            }
                            field {
                                name = "プロセス数"
                                value = runtime.pids.toString()
                            }
                            field {
                                name = "ディスク容量"
                                value = "${runtime.diskQuota} (ランタイムによって多少変化します)"
                            }

                            field {
                                name = "コンパイル時間"
                                value = "${session.compileMillis} ms"
                            }

                            field {
                                name = "実行時間"
                                value = "${session.executeMillis} ms"
                            }

                            field {
                                name = "備考"
                                value = """各ログの最大文字数は1000文字です。
                                    |1000文字を超えた場合、後ろから1000文字のみ出力されます。
                                """.trimMargin()
                            }
                        }
                    }

                }
            }

        }
    }

    inner class Runtime : CommandGroup("runtime", "ランタイムの情報") {
        override val subCommands: Array<SubCommand>
            get() = arrayOf(List(), Detail())

        inner class List : SubCommand("list", "実行可能なランタイムを一覧で出力します。") {
            override suspend fun ChatInputCommandInteraction.onExecute() {
                deferPublicResponse().respond {
                    val runtimeDataList =
                        client.get("http://localhost:8080/runtime").body<kotlin.collections.List<RuntimeData>>()
                    content = buildString {
                        appendLine("### 対応ランタイム一覧")
                        append(runtimeDataList.joinToString("\n") {
                            "* ${it.name} ${it.metaData.version} (${it.metaData.processor})"
                        })
                    }
                }
            }
        }

        inner class Detail : SubCommand("detail", "実行可能なランタイム情報を詳細に出力します。") {
            private val runtime = opt(Opt.Require(Opt.String), "runtime", "取得するランタイム名") {
                runBlocking {
                    client.get("http://localhost:8080/runtime").body<kotlin.collections.List<RuntimeData>>()
                }.forEach {
                    choice(it.name, it.id)
                }
            }

            override suspend fun ChatInputCommandInteraction.onExecute() {
                val runtime by runtime
                deferPublicResponse().respond {
                    val runtimeData = client.get("http://localhost:8080/runtime/${runtime}").body<RuntimeData>()
                    embed {
                        title = runtimeData.name
                        field {
                            name = "バージョン (処理系)"
                            value = "${runtimeData.metaData.version} (${runtimeData.metaData.processor})"
                        }
                        field {
                            name = "エイリアス"
                            value = runtimeData.alias.joinToString("\n") {
                                "* $it"
                            }
                        }
                        field {
                            name = "コマンド"
                            value = """
                                |```sh
                                |# 準備
                                |${runtimeData.commands.prepare ?: "# なし"}
                                |
                                |# コンパイル
                                |${runtimeData.commands.compile ?: "# なし"}
                                |
                                |# 実行
                                |${runtimeData.commands.execute}
                                |```
                            """.trimMargin()
                        }
                    }
                }
            }

        }
    }
}
