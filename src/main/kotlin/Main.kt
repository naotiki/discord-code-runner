import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.reply
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.on
import dev.kord.gateway.Intent
import dev.kord.gateway.PrivilegedIntent
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.websocket.*
import io.ktor.serialization.kotlinx.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import java.util.*

@OptIn(ExperimentalSerializationApi::class)
val client= HttpClient(CIO){
    install(ContentNegotiation) {
        json()
    }
    install(WebSockets){
        contentConverter = KotlinxWebsocketSerializationConverter(Cbor)
    }
}
suspend fun main(args: Array<String>) {
    val props = withContext(Dispatchers.IO) {
        Properties().apply {
            load(File("local.properties").inputStream())
        }
    }

    val kord = Kord(props["discord.token"] as String)

    kord.on<MessageCreateEvent> {
        // 他のボットを無視し、私たち自身も無視します。ここでは人間のみにサービスを提供します。
        if (message.author?.isBot != false) return@on
        val msgContent = message.content
        textCommands(msgContent)
    }
    kord.login {
        // we need to specify this to receive the content of messages
        @OptIn(PrivilegedIntent::class)
        intents += Intent.MessageContent
    }
}
