package us.huseli.umpc

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import kotlin.math.roundToInt
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

fun <T : Any> Collection<T>.skipEveryX(x: Int) = this.filterIndexed { index, _ -> (index + 1) % x != 0 }

fun <T : Any> Collection<T>.includeEveryX(x: Int) = this.filterIndexed { index, _ -> index % x == 0 }

fun <T : Any> Collection<T>.prune(maxLength: Int) =
    if (maxLength < this.size / 2) this.includeEveryX((this.size.toFloat() / maxLength).roundToInt())
    else this.skipEveryX((this.size.toFloat() / (this.size - maxLength)).roundToInt())

fun <T : Any> Flow<List<T>>.leadingChars(transform: (T) -> String) = this.map { items ->
    items.mapNotNull {
        transform(it)
            .replace(Regex("[^\\w&&[^0-9]]"), "#")
            .getOrNull(0)
            ?.uppercaseChar()
    }.distinct().sorted()
}
