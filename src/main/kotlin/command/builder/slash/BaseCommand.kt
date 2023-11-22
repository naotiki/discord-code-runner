package command.builder.slash

import dev.kord.core.entity.interaction.ChatInputCommandInteraction
import dev.kord.rest.builder.interaction.BaseInputChatBuilder
import dev.kord.rest.builder.interaction.OptionsBuilder
import kotlin.reflect.KProperty

abstract class BaseCommand(
    val name: String,
    val description: String,
){
    var interaction: ChatInputCommandInteraction? = null

    suspend fun exec(chatInputCommandInteraction: ChatInputCommandInteraction){
        interaction = chatInputCommandInteraction
        execute(chatInputCommandInteraction)
        interaction = null
    }
    protected abstract suspend fun execute(chatInputCommandInteraction: ChatInputCommandInteraction)

    inner class Arg<T, O : OptionsBuilder>(private val opt: Opt<T, O>, private val optionData: CommandOption<O>) {


        operator fun getValue(nothing: Nothing?, property: KProperty<*>): T = get(interaction!!)


        fun get(interaction: ChatInputCommandInteraction): T {
            return interaction.command.let { opt.get(it, optionData.name) }
        }

        fun define(builder: BaseInputChatBuilder) {
            opt.define(builder, optionData)
        }

    }
}
