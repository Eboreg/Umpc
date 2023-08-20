package us.huseli.umpc.mpd.response

import us.huseli.umpc.data.MPDSong
import us.huseli.umpc.data.toMPDSong
import us.huseli.umpc.data.toMPDSongs

class MPDBatchTextResponse : BaseMPDResponse() {
    val batchResponseMaps: List<Map<String, List<String>>>
        get() = _responseLines.batchResponseToMultimapLists().flatten()

    fun extractNestedMaps(): List<List<Map<String, String>>> = _responseLines.batchResponseToMapLists()

    fun extractNestedSongs(): List<List<MPDSong>> = _responseLines.batchResponseToMapLists().map { it.toMPDSongs() }

    fun extractNestedPositionedSongs(): List<List<MPDSong>> = _responseLines.batchResponseToMapLists().map { maps ->
        maps.mapIndexedNotNull { index, map -> map.toMPDSong(position = index) }
    }

    override fun toString() =
        "${javaClass.simpleName}[status=$status, error=$error, mpdError=$mpdError, response=$_responseLines]"
}

fun List<String>.batchResponseToLists(): List<List<String>> {
    /** Splits response at "list_OK" lines. */
    var currentList = mutableListOf<String>()
    val lists = mutableListOf<List<String>>()

    forEach { line ->
        if (line == "list_OK") {
            lists.add(currentList)
            currentList = mutableListOf()
        } else if (line != "OK") {
            currentList.add(line)
        }
    }
    return lists
}

fun List<String>.batchResponseToMultimapLists(): List<List<Map<String, List<String>>>> =
    batchResponseToLists().map { it.responseToMultimapList() }

fun List<String>.batchResponseToMapLists(): List<List<Map<String, String>>> =
    batchResponseToLists().map { it.responseToMapList() }
