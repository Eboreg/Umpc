package us.huseli.umpc.mpd.response

import java.io.File
import java.io.PrintWriter

class MPDFileResponse : MPDBaseResponse() {
    private lateinit var _file: File
    private lateinit var _writer: PrintWriter

    fun start(file: File) {
        _file = file
        _writer = PrintWriter(file.outputStream())
    }

    fun putLine(line: String) {
        _writer.println(line)
    }

    fun finish() {
        _writer.close()
    }
}
