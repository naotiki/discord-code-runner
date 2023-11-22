package command.slash

import command.builder.slash.Opt
import command.builder.slash.Opt.Attachment
import command.builder.slash.Opt.Require
import command.builder.slash.SlashCommand
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.interaction.ChatInputCommandInteraction

class Run : SlashCommand("run", "るん") {
    private val src = opt(Require(Attachment), "src", "実行するソースコード")
    private val runtime = opt(Opt.String, "runtime", "実行環境を指定") {
        required = false
    }

    override suspend fun ChatInputCommandInteraction.onExecute() {
        val src by src
        val runtime by runtime
        deferPublicResponse().respond {
            content="""
                $src
                $runtime
            """.trimIndent()
        }
    }
}
