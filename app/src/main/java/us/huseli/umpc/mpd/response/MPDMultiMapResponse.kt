package us.huseli.umpc.mpd.response

import us.huseli.umpc.data.MPDAlbum
import us.huseli.umpc.data.MPDOutput
import us.huseli.umpc.data.MPDPlaylist
import us.huseli.umpc.data.MPDSong
import us.huseli.umpc.data.toMPDAlbums
import us.huseli.umpc.data.toMPDOutput
import us.huseli.umpc.data.toMPDPlaylist
import us.huseli.umpc.data.toMPDSong

class MPDMultiMapResponse : MPDBaseTextResponse() {
    private val _responseMaps = mutableListOf<Map<String, List<String>>>()
    private var _currentMap = mutableMapOf<String, List<String>>()
    private var _startKey: String? = null

    private val responseMaps: List<Map<String, List<String>>>
        get() = _responseMaps

    override fun putLine(line: String) {
        if (responseRegex.matches(line)) {
            parseResponseLine(line)?.let { (key, value) ->
                if (_startKey == null) _startKey = key
                else if (key == _startKey) {
                    _responseMaps.add(_currentMap)
                    _currentMap = mutableMapOf()
                }
                _currentMap[key] = _currentMap[key]?.let { it + value } ?: listOf(value)
            }
        }
    }

    override fun <RT : MPDBaseResponse> finish(status: Status, exception: Throwable?, error: String?): RT {
        if (_startKey != null) _responseMaps.add(_currentMap)
        return super.finish(status, exception, error)
    }

    fun extractAlbums(): List<MPDAlbum> = _responseMaps.flatMap { it.toMPDAlbums() }
    fun extractOutputs(): List<MPDOutput> =
        _responseMaps.mapNotNull { map -> map.mapValues { it.value.first() }.toMPDOutput() }

    fun extractPlaylists(): List<MPDPlaylist> =
        _responseMaps.mapNotNull { map -> map.mapValues { it.value.first() }.toMPDPlaylist() }

    fun extractSongs(): List<MPDSong> =
        _responseMaps.mapNotNull { map -> map.mapValues { it.value.first() }.toMPDSong() }

    override fun toString() =
        "${javaClass.simpleName}[status=$status, error=$error, responseMaps=$responseMaps]"
}
