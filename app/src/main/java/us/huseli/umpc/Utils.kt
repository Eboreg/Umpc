package us.huseli.umpc

import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.ComponentActivity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder

val SENSIBLE_DATE_TIME: DateTimeFormatter = DateTimeFormatterBuilder()
    .parseCaseSensitive()
    .append(DateTimeFormatter.ISO_LOCAL_DATE)
    .appendLiteral(' ')
    .append(DateTimeFormatter.ISO_LOCAL_TIME)
    .toFormatter()

fun File.toBitmap(): Bitmap? = takeIf { it.isFile }?.inputStream().use { BitmapFactory.decodeStream(it) }

fun Int.sqrt() = kotlin.math.sqrt(toDouble())

fun Double.roundUp() = toInt() + (if (this % 1 > 0) 1 else 0)

fun Int.roundUpSqrt() = sqrt().roundUp()

fun IntRange.toYearRangeString() = if (first == last) first.toString() else "${first}-${last}"

fun Instant.formatDateTime(): String = atZone(ZoneId.systemDefault()).format(SENSIBLE_DATE_TIME)

fun Context.getActivity(): ComponentActivity? = when (this) {
    is ComponentActivity -> this
    is ContextWrapper -> baseContext.getActivity()
    else -> null
}

fun String.toInstantOrNull(): Instant? = try {
    Instant.parse(this)
} catch (e: Exception) {
    null
}

fun String.parseYear(): Int? = Regex("^([1-2]\\d{3})").find(this)?.value?.toInt()

inline fun <T : Any> Flow<List<T>>.leadingChars(crossinline transform: (T) -> String) = map { items ->
    items.mapNotNull {
        transform(it)
            .replace(Regex("[^\\w&&[^0-9]]"), "#")
            .getOrNull(0)
            ?.uppercaseChar()
    }.distinct().sorted()
}

private val leadingJunkRegex =
    Regex("^[^\\p{Alnum}]*((the )|(los )|(os )|(de )|(dom )|(den )|(det ))?[^\\p{Alnum}]*", RegexOption.IGNORE_CASE)

fun String.replaceLeadingJunk(): String = replace(leadingJunkRegex, "")

fun String.highlight(term: String?): AnnotatedString {
    val builder = AnnotatedString.Builder(this)

    if (term != null) {
        val matches = Regex(term, RegexOption.IGNORE_CASE).findAll(this)
        val style = SpanStyle(fontWeight = FontWeight.Black, textDecoration = TextDecoration.Underline)

        matches.forEach { builder.addStyle(style, it.range.first, it.range.last + 1) }
    }
    return builder.toAnnotatedString()
}

/**
 * All `args` will be individually quoted and escaped. So if they already
 * contain quotation marks, those will be escaped.
 *
 * Ex. formatMPDCommand("list artist", "album", "the duck who said \"quack\"")
 * becomes the string:
 * list artist "album" "the duck who said \"quack\""
 */
fun formatMPDCommand(command: String, vararg args: Any): String =
    if (args.isEmpty()) command
    else "$command " + args.joinToString(" ") { "\"${it.toString().escapeQuotes()}\"" }

fun formatMPDCommand(command: String, args: Collection<Any> = emptyList()): String =
    if (args.isEmpty()) command else formatMPDCommand(command, *args.toTypedArray())

fun <T : Any> Collection<T>.containsAny(vararg elements: T): Boolean = elements.any { contains(it) }

fun String.escapeQuotes() =
    replace("\"", "\\\"")
        .replace("'", "\\'")
        .replace("\\\\\"", "\\\\\\\"")
