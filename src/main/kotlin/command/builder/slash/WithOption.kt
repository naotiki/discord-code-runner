package command.builder.slash

import dev.kord.rest.builder.interaction.OptionsBuilder

abstract class WithOption(name: String, description: String) : BaseCommand(name, description) {
    protected val opts = mutableListOf<BaseCommand.Arg<*, *>>()
    protected fun <T, O : OptionsBuilder> opt(
        opt: Opt<T, O>,
        name: String,
        description: String,
        optionBuilder: O.() -> Unit = {}
    ): Arg<T, O> {
        return Arg(opt, CommandOption(name, description, optionBuilder)).also {
            opts.add(it)
        }
    }
}
