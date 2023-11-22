package command.builder.slash

import dev.kord.core.Kord
import dev.kord.core.entity.interaction.ChatInputCommandInteraction
import dev.kord.rest.builder.interaction.GlobalChatInputCreateBuilder
import dev.kord.rest.builder.interaction.RootInputChatBuilder
import dev.kord.rest.builder.interaction.SubCommandBuilder
import dev.kord.rest.builder.interaction.subCommand

abstract class SlashCommandWithSub(
    name: String,
    description: String,
    val builder: GlobalChatInputCreateBuilder.() -> Unit = {}
) : BaseCommand(name, description), RootCommand {
    abstract val subCommands: Array<SubCommand>
    override suspend fun execute(chatInputCommandInteraction: ChatInputCommandInteraction) {
        val a=(chatInputCommandInteraction.command as dev.kord.core.entity.interaction.SubCommand)
        subCommands.single { it.name==a.name }.exec(chatInputCommandInteraction)
    }

    abstract inner class SubCommand(
        name: String, description: String,
        private val builder: SubCommandBuilder.() -> Unit = {},
    ) : WithOption(name, description) {
        override suspend fun execute(chatInputCommandInteraction: ChatInputCommandInteraction) {
            chatInputCommandInteraction.onExecute()
        }

        internal abstract suspend fun ChatInputCommandInteraction.onExecute()
        fun register(builder: RootInputChatBuilder) {
            builder.subCommand(name, description) {
                opts.forEach {
                    it.define(this)
                }
                builder()
            }
        }
    }

    override suspend fun register(kord: Kord) {
        kord.createGlobalChatInputCommand(name, description) {
            subCommands.forEach {
                it.register(this)
            }
            builder()
        }
    }
}


