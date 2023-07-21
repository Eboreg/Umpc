package us.huseli.umpc.mpd.command

import us.huseli.umpc.mpd.response.MPDBaseResponse
import us.huseli.umpc.mpd.response.MPDBatchMapResponse
import us.huseli.umpc.mpd.response.MPDMapResponse
import java.net.Socket

class MPDBatchMapCommand(
    val commands: Collection<MPDMapCommand>,
    private val successCriteria: SuccessCriteria = SuccessCriteria.ANY_SUCCEEDED,
    onFinish: ((MPDBatchMapResponse) -> Unit)? = null
) : MPDBaseCommand<MPDBatchMapResponse>(onFinish) {
    enum class SuccessCriteria { ANY_SUCCEEDED, ALL_SUCCEEDED }

    override suspend fun getResponse(socket: Socket): MPDBatchMapResponse {
        val commandResponses = mutableListOf<MPDMapResponse>()

        commands.forEach { command ->
            commandResponses.add(command.getResponse(socket))
        }
        return MPDBatchMapResponse(commandResponses = commandResponses).finish(
            status = when {
                successCriteria == SuccessCriteria.ALL_SUCCEEDED && commandResponses.any { !it.isSuccess } -> MPDBaseResponse.Status.ERROR_OTHER
                successCriteria == SuccessCriteria.ANY_SUCCEEDED && commandResponses.none { it.isSuccess } -> MPDBaseResponse.Status.ERROR_OTHER
                else -> MPDBaseResponse.Status.OK
            }
        )
    }

    override fun equals(other: Any?) =
        other is MPDBatchMapCommand && other.commands == commands

    override fun hashCode(): Int = commands.hashCode()

    override fun toString() = "${javaClass.simpleName}[$commands]"
}
