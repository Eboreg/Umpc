package us.huseli.umpc.mpd.command

import us.huseli.umpc.mpd.response.MPDBatchTextResponse

class MPDBatchCommand(
    private var commands: Set<String>,
    onFinish: ((MPDBatchTextResponse) -> Unit)? = null,
) : BaseMPDTextCommand<MPDBatchTextResponse>(generateCommand(commands), onFinish) {
    constructor(commands: List<String>, onFinish: ((MPDBatchTextResponse) -> Unit)? = null) :
        this(commands.toSet(), onFinish)

    fun addCommands(newCommands: List<String>) {
        commands += newCommands
        command = generateCommand(commands)
    }

    override fun getEmptyResponse() = MPDBatchTextResponse()

    override fun equals(other: Any?) = other is MPDBatchCommand && other.commands == commands

    override fun hashCode(): Int = commands.hashCode()

    override fun toString(): String {
        return if (commands.size <= 5) "${javaClass.simpleName}[$commands]"
        else "${javaClass.simpleName}[${commands.toList().subList(0, 5)}, +${commands.size - 5} rows]"
    }

    companion object {
        private fun generateCommand(commands: Collection<String>) =
            (listOf("command_list_ok_begin") + commands + listOf("command_list_end")).joinToString("\n")
    }
}
