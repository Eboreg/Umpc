package us.huseli.umpc.mpd.response

import android.util.Log
import us.huseli.umpc.data.MPDError
import java.io.ByteArrayInputStream

class MPDBinaryResponse : BaseMPDResponse() {
    private var _length = 0
    private var _data = ByteArray(0)
    val stream: ByteArrayInputStream
        get() = ByteArrayInputStream(_data)

    override val logLevel: Int
        get() = if (mpdError?.type == MPDError.Type.NO_EXIST) Log.WARN else super.logLevel

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
