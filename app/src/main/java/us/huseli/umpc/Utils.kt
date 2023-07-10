package us.huseli.umpc

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import kotlin.time.Duration.Companion.seconds

val SENSIBLE_DATE_TIME: DateTimeFormatter = DateTimeFormatterBuilder()
    .parseCaseSensitive()
    .append(DateTimeFormatter.ISO_LOCAL_DATE)
    .appendLiteral(' ')
    .append(DateTimeFormatter.ISO_LOCAL_TIME)
    .toFormatter()

fun Double.formatDuration() = this.seconds.toComponents { hours, minutes, seconds, _ ->
    if (hours > 0) String.format("%d:%02d:%02d", hours, minutes, seconds)
    else String.format("%d:%02d", minutes, seconds)
}

fun File.toBitmap(): Bitmap? = this.takeIf { it.isFile }?.inputStream().use { BitmapFactory.decodeStream(it) }

fun Int.sqrt() = kotlin.math.sqrt(this.toDouble())

fun Double.roundUp() = this.toInt() + (if (this % 1 > 0) 1 else 0)

fun Int.roundUpSqrt() = this.sqrt().roundUp()

fun IntRange.toYearRangeString() =
    if (this.first == this.last) this.first.toString() else "${this.first}-${this.last}"

fun Instant.formatDateTime(): String = atZone(ZoneId.systemDefault()).format(SENSIBLE_DATE_TIME)

fun Instant.formatDate(): String = atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ISO_LOCAL_DATE)

fun Context.getActivity(): ComponentActivity? = when (this) {
    is ComponentActivity -> this
    is ContextWrapper -> baseContext.getActivity()
    else -> null
}

@Suppress("BooleanMethodIsAlwaysInverted")
@Composable
fun isInLandscapeMode() = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

fun String.toInstant(): Instant? = try {
    Instant.parse(this)
} catch (e: Exception) {
    null
}

fun String.parseYear(): Int? = Regex("^([1-2]\\d{3})").find(this)?.value?.toInt()
