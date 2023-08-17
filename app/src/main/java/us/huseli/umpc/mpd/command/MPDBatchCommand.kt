package us.huseli.umpc.mpd.command

import us.huseli.umpc.mpd.response.MPDTextResponse

class MPDBatchCommand(
    val commands: Collection<String>,
    onFinish: ((MPDTextResponse) -> Unit)? = null,
) : MPDCommand(
    (listOf("command_list_ok_begin") + commands + listOf("command_list_end")).joinToString("\n"),
    onFinish,
) {
    override fun equals(other: Any?) = other is MPDBatchCommand && other.commands == commands

    override fun hashCode(): Int = commands.hashCode()

    override fun toString() = "${javaClass.simpleName}[$commands]"
}
