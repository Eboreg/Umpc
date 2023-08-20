package us.huseli.umpc

import android.util.Log

interface LoggerInterface {
    fun log(tag: String, message: String, level: Int = Log.INFO) {
        if (BuildConfig.DEBUG) Log.println(level, tag, message)
    }

    fun log(message: String, level: Int = Log.INFO) = log(javaClass.simpleName, message, level)

    fun logError(message: String, exception: Throwable? = null) = Log.e(javaClass.simpleName, message, exception)
}

object Logger : LoggerInterface
