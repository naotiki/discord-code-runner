import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.reply
import dev.kord.core.event.message.MessageCreateEvent


private val textCommandList= listOf<TextCommand>(RunTextCommand())
suspend fun MessageCreateEvent.textCommands(messageStr:String){
    textCommandList.singleOrNull {
        messageStr.startsWith(it.prefix)
    }?.run {
        if (false&&message.author?.id != Snowflake(684655306764058644)){
            message.reply {
                content="""まだNaotikiしか使えません
                    |ごめんね
                """.trimMargin()
            }
            return@run
        }
        execute(messageStr.removePrefix(prefix))
    }
}


const val textCommandPrefix = "!"
interface TextCommand{
    fun pre(str:String) = textCommandPrefix+str
    val prefix:String
    abstract suspend fun MessageCreateEvent.execute(body:String)
}

