package command.builder.text

import dev.kord.core.event.message.MessageCreateEvent

abstract class TextCommand(val name:String){
    open val prefix:String= textCommandPrefix + name

    fun arg(regex:String){

    }
    abstract suspend fun MessageCreateEvent.execute(body:String)
}


