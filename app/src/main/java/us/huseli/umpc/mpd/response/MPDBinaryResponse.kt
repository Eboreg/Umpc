package us.huseli.umpc.mpd.response

import java.io.ByteArrayInputStream

class MPDBinaryResponse : MPDBaseResponse() {
    private var _length = 0
    private var _data = ByteArray(0)
    val stream: ByteArrayInputStream
        get() = ByteArrayInputStream(_data)

    fun putBinaryChunk(chunk: ByteArray, destPos: Int, chunkLength: Int) {
        System.arraycopy(chunk, 0, _data, destPos, chunkLength)
    }

    fun setLength(length: Int) {
        if (_length == 0) {
            _length = length
            _data = ByteArray(length)
        }
    }
}
