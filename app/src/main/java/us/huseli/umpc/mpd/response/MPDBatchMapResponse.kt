package us.huseli.umpc.mpd.response

class MPDBatchMapResponse : MPDBaseTextResponse() {
    private var _currentMap = mutableMapOf<String, List<String>>()
    private val _responseMaps = mutableListOf<Map<String, List<String>>>()

    val responseMaps: List<Map<String, List<String>>>
        get() = _responseMaps

    override fun putLine(line: String) {
        if (responseRegex.matches(line)) {
            parseResponseLine(line)?.let { (key, value) ->
                _currentMap[key] = _currentMap[key]?.let { it + value } ?: listOf(value)
            }
        } else if (line == "list_OK") {
            _responseMaps.add(_currentMap)
            _currentMap = mutableMapOf()
        }
    }
}
