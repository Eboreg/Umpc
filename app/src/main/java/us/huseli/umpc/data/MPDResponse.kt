package us.huseli.umpc.data

@Suppress("ArrayInDataClass")
data class MPDResponse(
    val status: Status,
    val error: String? = null,
    val binaryResponse: ByteArray = byteArrayOf(),
    val responseMap: Map<String, String> = emptyMap(),
    val responseList: List<Pair<String, String>> = emptyList(),
) {
    enum class Status { OK, EMPTY, ERROR }

    val isSuccess: Boolean = status == Status.OK

    override fun toString() =
        "${javaClass.simpleName}[status=$status, error=$error, responseMap=$responseMap, responseList=$responseList]"

    private fun split(): List<Map<String, String>> {
        val responseMaps = mutableListOf<Map<String, String>>()
        var startKey = ""
        var currentMap = mutableMapOf<String, String>()

        responseList.forEachIndexed { index, (key, value) ->
            if (index == 0) startKey = key
            if ((key == startKey && index > 0) || index == responseList.size - 1) {
                responseMaps.add(currentMap)
            }
            if (key == startKey) currentMap = mutableMapOf()
            currentMap[key] = value
        }

        return responseMaps
    }

    private fun splitGrouped(): List<Map<String, List<String>>> {
        val responseMaps = mutableListOf<Map<String, List<String>>>()
        var startKey = ""
        var currentMap = mutableMapOf<String, List<String>>()

        responseList.forEachIndexed { index, (key, value) ->
            if (index == 0) startKey = key
            if ((key == startKey && index > 0) || index == responseList.size - 1) {
                responseMaps.add(currentMap)
            }
            if (key == startKey) currentMap = mutableMapOf()
            currentMap[key] = currentMap[key]?.let { it + value } ?: listOf(value)
        }

        return responseMaps
    }

    fun extractSongs(): List<MPDSong> = if (isSuccess) split().mapNotNull { it.toMPDSong() } else emptyList()

    fun extractOutputs(): List<MPDOutput> = if (isSuccess) split().mapNotNull { it.toMPDOutput() } else emptyList()

    fun extractChanged(): List<String> =
        if (isSuccess) responseList.filter { it.first == "changed" }.map { it.second } else emptyList()

    fun extractAlbums(): List<MPDAlbum> =
        if (isSuccess) splitGrouped().flatMap { it.toMPDAlbums() } else emptyList()
}
