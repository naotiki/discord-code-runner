package command.builder.slash

import dev.kord.core.Kord
import dev.kord.core.entity.interaction.ChatInputCommandInteraction
import dev.kord.rest.builder.interaction.GlobalChatInputCreateBuilder

abstract class SlashCommand(
    name: String, description: String,
    val builder: GlobalChatInputCreateBuilder.() -> Unit = {},
): WithOption(name, description),RootCommand {
    internal abstract suspend fun ChatInputCommandInteraction.onExecute()

    override suspend fun execute(chatInputCommandInteraction: ChatInputCommandInteraction) {
        chatInputCommandInteraction.onExecute()
    }


    override suspend fun register(kord: Kord) {
        kord.createGlobalChatInputCommand(name, description) {
            opts.forEach {
                println(it)
                it.define(this)
            }
            builder()
        }
    }
}

