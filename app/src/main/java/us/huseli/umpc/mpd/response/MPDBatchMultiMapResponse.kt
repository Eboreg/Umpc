package us.huseli.umpc.mpd.response

class MPDBatchMultiMapResponse : MPDBaseTextResponse() {
    private val _subResponses = mutableListOf<MPDMultiMapResponse>()
    private var _currentSubResponse = MPDMultiMapResponse()

    val subResponses: List<MPDMultiMapResponse>
        get() = _subResponses

    override fun putLine(line: String) {
        if (line == "list_OK") {
            _subResponses.add(_currentSubResponse)
            _currentSubResponse = MPDMultiMapResponse()
        } else {
            _currentSubResponse.putLine(line)
        }
    }
}