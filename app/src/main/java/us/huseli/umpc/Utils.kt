package us.huseli.umpc

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import java.time.Instant
import kotlin.time.Duration.Companion.seconds

fun Double.formatDuration(): String = this.seconds.toComponents { hours, minutes, seconds, _ ->
    if (hours > 0) String.format("%d:%02d:%02d", hours, minutes, seconds)
    else String.format("%d:%02d", minutes, seconds)
}

fun File.toBitmap(): Bitmap? = this.takeIf { it.isFile }?.inputStream().use { BitmapFactory.decodeStream(it) }

fun Int.sqrt(): Double = kotlin.math.sqrt(this.toDouble())

fun Double.roundUp(): Int = this.toInt() + (if (this % 1 > 0) 1 else 0)

fun Int.roundUpSqrt(): Int = this.sqrt().roundUp()

fun IntRange.toYearRangeString(): String =
    if (this.first == this.last) this.first.toString() else "${this.first}-${this.last}"
