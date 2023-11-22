package command.builder.slash

import dev.kord.rest.builder.interaction.OptionsBuilder

data class CommandOption<O: OptionsBuilder>(val name: String, val description: String, val optionBuilder: O.() -> Unit)
