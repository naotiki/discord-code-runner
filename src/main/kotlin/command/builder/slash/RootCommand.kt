package command.builder.slash

import dev.kord.core.Kord

interface RootCommand{
    suspend fun register(kord: Kord)
}
