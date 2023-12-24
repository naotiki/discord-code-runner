package util

fun String.escapeCodeblocks(): String = replace("```","\u200B`\u200B`\u200B`\u200B")
