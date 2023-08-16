package us.huseli.umpc.mpd.response

import us.huseli.umpc.data.MPDSong
import us.huseli.umpc.data.toMPDSong

class MPDMapResponse : MPDBaseTextResponse() {
    private val _responseMap = mutableMapOf<String, List<String>>()

    val responseMap: Map<String, List<String>>
        get() = _responseMap

    override fun putLine(line: String) {
        if (responseRegex.matches(line)) {
            parseResponseLine(line)?.let { (key, value) ->
                _responseMap[key] = _responseMap[key]?.let { it + value } ?: listOf(value)
            }
        }
    }

    fun extractChanged(): List<String> = _responseMap["changed"] ?: emptyList()

    fun extractSong(): MPDSong? = responseMap.mapValues { it.value.first() }.toMPDSong()

    fun extractTagTypes(): List<String> = _responseMap["tagtype"] ?: emptyList()

    override fun toString() =
        "${javaClass.simpleName}[status=$status, error=$error, responseMap=$responseMap]"
}
