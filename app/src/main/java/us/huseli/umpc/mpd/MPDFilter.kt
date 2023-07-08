package us.huseli.umpc.mpd

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration

open class MPDFilter(private val term: String) {
    override fun toString() = term
    open fun and(other: MPDFilter) = MPDAndFilter(this, other)
    fun not() = MPDFilter("(!$this)")
    fun find() = "find \"${MPDFilterContext.escape(term)}\""
    fun search() = "search \"${MPDFilterContext.escape(term)}\""
    fun findadd() = "findadd \"${MPDFilterContext.escape(term)}\""
    fun findadd(position: Int): String {
        val pos = if (position >= 0) "+${position}" else position.toString()
        return "findadd \"${MPDFilterContext.escape(term)}\" position $pos"
    }
}

class MPDAndFilter(private vararg val filters: MPDFilter) :
    MPDFilter("(${filters.joinToString(" AND ")})") {
    override fun and(other: MPDFilter) = MPDAndFilter(*this.filters, other)
}

object MPDFilterContext {
    fun escape(string: String) = string
        .replace("\"", "\\\"")
        .replace("'", "\\'")
        .replace("\\\\\"", "\\\\\\\"")

    fun equals(tag: String, value: String) = MPDFilter("($tag == \"${escape(value)}\")")
    fun notEquals(tag: String, value: String) = MPDFilter("($tag != \"${escape(value)}\")")
    fun contains(tag: String, value: String) = MPDFilter("($tag contains \"${escape(value)}\")")
}

inline fun mpdFilter(block: MPDFilterContext.() -> MPDFilter) = with(MPDFilterContext) { block() }
inline fun mpdSearch(block: MPDFilterContext.() -> MPDFilter) = with(MPDFilterContext) { block().search() }
inline fun mpdFind(block: MPDFilterContext.() -> MPDFilter) = with(MPDFilterContext) { block().find() }

fun String.highlight(term: String?): AnnotatedString {
    val builder = AnnotatedString.Builder(this)

    if (term != null) {
        val matches = Regex(term, RegexOption.IGNORE_CASE).findAll(this)
        val style = SpanStyle(fontWeight = FontWeight.Black, textDecoration = TextDecoration.Underline)

        matches.forEach { builder.addStyle(style, it.range.first, it.range.last + 1) }
    }
    return builder.toAnnotatedString()
}
