package us.huseli.umpc.mpd.request

import us.huseli.umpc.mpd.response.MPDBatchTextResponse

class MPDBatchRequest(
    private var commands: Set<String>,
    onFinish: ((MPDBatchTextResponse) -> Unit)? = null,
) : BaseMPDTextRequest<MPDBatchTextResponse>(generateCommand(commands), onFinish) {
    constructor(commands: Iterable<String>, onFinish: ((MPDBatchTextResponse) -> Unit)? = null) :
            this(commands.toSet(), onFinish)

    fun addCommands(newCommands: Iterable<String>) {
        commands += newCommands
        command = generateCommand(commands)
    }

    override fun getEmptyResponse() = MPDBatchTextResponse()

    override fun equals(other: Any?) = other is MPDBatchRequest && other.commands == commands

    override fun hashCode(): Int = commands.hashCode()

    override fun toString(): String {
        return if (commands.size <= 5) "${javaClass.simpleName}[$commands]"
        else "${javaClass.simpleName}[${commands.toList().subList(0, 5)}, +${commands.size - 5} rows]"
    }

    companion object {
        private fun generateCommand(commands: Iterable<String>) =
            (listOf("command_list_ok_begin") + commands + listOf("command_list_end")).joinToString("\n")
    }
}
