package command.builder.slash

import dev.kord.core.Kord
import dev.kord.core.entity.interaction.ChatInputCommandInteraction
import dev.kord.rest.builder.interaction.*

abstract class SlashCommandWithGroup(
    name: String,
    description: String,
    val builder: GlobalChatInputCreateBuilder.() -> Unit = {}
) : BaseCommand(name, description), RootCommand {
    abstract val groups: Array<CommandGroup>

    override suspend fun execute(chatInputCommandInteraction: ChatInputCommandInteraction) {
        val a=(chatInputCommandInteraction.command as dev.kord.core.entity.interaction.GroupCommand)
        groups.single { it.name==a.groupName }.exec(chatInputCommandInteraction)
    }
    abstract inner class CommandGroup(
        name: String,
        description: String,
        private val builder: GroupCommandBuilder.() -> Unit = {}
    ) : BaseCommand(name, description) {
        abstract val subCommands:Array<SubCommand>
        override suspend fun execute(chatInputCommandInteraction: ChatInputCommandInteraction) {
            val a=(chatInputCommandInteraction.command as dev.kord.core.entity.interaction.GroupCommand)
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
            fun register(builder: GroupCommandBuilder) {
                builder.subCommand(name, description) {
                    opts.forEach {
                        it.define(this)
                    }
                    builder()
                }
            }
        }
        fun register(builder: RootInputChatBuilder) {
            builder.group(name, description) {
                subCommands.forEach {
                    it.register(this)
                }
                builder()
            }
        }
    }

    override suspend fun register(kord: Kord) {
        kord.createGlobalChatInputCommand(name, description) {
            groups.forEach {
                it.register(this)
            }
            builder()
        }
    }
}
