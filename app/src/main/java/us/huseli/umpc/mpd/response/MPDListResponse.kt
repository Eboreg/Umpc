package us.huseli.umpc.mpd.response

class MPDListResponse(private val key: String) : MPDBaseTextResponse() {
    private val _values = mutableListOf<String>()

    val values: List<String>
        get() = _values

    override fun putLine(line: String) {
        parseResponseLine(line)?.let { (key, value) ->
            if (key == this.key) _values.add(value)
        }
    }
}
