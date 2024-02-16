import command.builder.slash.*
import command.builder.text.textCommands
import command.slash.Info
import dev.kord.core.Kord
import dev.kord.core.entity.interaction.ChatInputCommandInteraction
import dev.kord.core.event.interaction.ChatInputCommandInteractionCreateEvent
import dev.kord.core.event.interaction.GlobalChatInputCommandInteractionCreateEvent
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.on
import dev.kord.gateway.Intent
import dev.kord.gateway.PrivilegedIntent
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.websocket.*
import io.ktor.serialization.kotlinx.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import java.io.File
import java.net.ConnectException
import java.util.*

@OptIn(ExperimentalSerializationApi::class)
val client = HttpClient(CIO) {
    install(ContentNegotiation) {
        json()
    }
    install(WebSockets) {
        contentConverter = KotlinxWebsocketSerializationConverter(Cbor)
    }
}

val slashCommands by lazy {
    arrayOf<RootCommand>(
        Info()
    )
}

suspend fun main(args: Array<String>) {

    val props = withContext(Dispatchers.IO) {
        Properties().apply {
            load(File("env.properties").inputStream())
        }
    }
    val env = props["discord.env"] as String
    println("Starting... on $env environment")
    println(props["discord.${env}.token"] as String)
    val kord = Kord(props["discord.${env}.token"] as String)
    println("Register slash-commands")
    slashCommands.forEach {
        println("\tRegister ${(it as? BaseCommand)?.name}")
        it.register(kord)
    }


    kord.on<MessageCreateEvent> {
        // 他のボットを無視し、私たち自身も無視します。ここでは人間のみにサービスを提供します。
        if (message.author?.isBot != false) return@on
        val msgContent = message.content
        textCommands(msgContent)
    }
    kord.on<ChatInputCommandInteractionCreateEvent> {
        println(interaction.command)
        when (interaction.command) {
            is dev.kord.core.entity.interaction.GroupCommand -> slashCommands.filterIsInstance<SlashCommandWithGroup>()
            is dev.kord.core.entity.interaction.RootCommand -> slashCommands.filterIsInstance<SlashCommand>()
            is dev.kord.core.entity.interaction.SubCommand -> slashCommands.filterIsInstance<SlashCommandWithSub>()
        }.single { it.name == interaction.command.rootName }.exec(interaction)
    }
    println("Login")
    kord.login {
        // we need to specify this to receive the content of messages
        @OptIn(PrivilegedIntent::class)
        intents += Intent.MessageContent
    }
}
