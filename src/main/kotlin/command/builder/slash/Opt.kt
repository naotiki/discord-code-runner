package command.builder.slash

import dev.kord.core.entity.Entity
import dev.kord.core.entity.channel.ResolvedChannel
import dev.kord.core.entity.interaction.InteractionCommand
import dev.kord.rest.builder.interaction.*

sealed class Opt<T,O: OptionsBuilder>(
    val get: InteractionCommand.(name: kotlin.String) -> T,
    val define: BaseInputChatBuilder.(CommandOption<O>)->Unit
) {
    data object Integer : Opt<Long?, IntegerOptionBuilder>({
        integers[it]
    },{
        integer(it.name,it.description,it.optionBuilder)
    })

    data object String : Opt<kotlin.String?, StringChoiceBuilder>({
        strings[it]
    },{
        string(it.name,it.description,it.optionBuilder)
    })

    data object Number : Opt<Double?, NumberOptionBuilder>({
        numbers[it]
    },{
        number(it.name,it.description,it.optionBuilder)
    })

    data object Boolean : Opt<kotlin.Boolean?, BooleanBuilder>({
        booleans[it]
    },{
        boolean(it.name,it.description,it.optionBuilder)
    })

    data object User : Opt<dev.kord.core.entity.User?, UserBuilder>({
        users[it]
    },{
        user(it.name,it.description,it.optionBuilder)
    })

    //data object Member : OptType<dev.kord.core.entity.Member?,?>({ members[it] })


    data object Channel : Opt<ResolvedChannel?, ChannelBuilder>({
        channels[it]
    },{
        channel(it.name,it.description,it.optionBuilder)
    })

    data object Role : Opt<dev.kord.core.entity.Role?, RoleBuilder>({
        roles[it]
    },{
        role(it.name,it.description,it.optionBuilder)
    })

    data object Mentionables : Opt<Entity?, MentionableBuilder>({
        mentionables[it]
    },{
        mentionable(it.name,it.description,it.optionBuilder)
    })

    data object Attachment : Opt<dev.kord.core.entity.Attachment?, AttachmentBuilder>({
        attachments[it]
    },{
        attachment(it.name,it.description,it.optionBuilder)
    })

    data class Require<T,B: OptionsBuilder>(val opt: Opt<T?, B>): Opt<T, B>({
        opt.get(this,it)!!
    },{
        opt.define(this,it.copy {
            it.optionBuilder(this)
            required = true
        })
    })

}
