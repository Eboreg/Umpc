package us.huseli.umpc.mpd.response

abstract class MPDBaseTextResponse : MPDBaseResponse() {
    abstract fun putLine(line: String)
}