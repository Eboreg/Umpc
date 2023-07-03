package us.huseli.umpc.mpd

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight

@Suppress("unused")
object MPDSearch {
    fun escape(string: String) = string
        .replace("\"", "\\\"")
        .replace("'", "\\'")
        .replace("\\\\\"", "\\\\\\\"")

    fun equals(tag: String, value: String) = "($tag == \"${escape(value)}\")"
    fun notEquals(tag: String, value: String) = "($tag != \"${escape(value)}\")"
    fun contains(tag: String, value: String) = "($tag contains \"${escape(value)}\")"
    fun and(vararg terms: String) = "(${terms.joinToString(" AND ")})"
    fun not(term: String) = "(!$term)"
}

inline fun filter(block: MPDSearch.() -> String) = with(MPDSearch) { "\"${escape(block())}\"" }
inline fun search(block: MPDSearch.() -> String) = "search ${filter(block)}"
inline fun find(block: MPDSearch.() -> String) = "find ${filter(block)}"
inline fun findAdd(position: String? = null, block: MPDSearch.() -> String) =
    "findadd ${filter(block)}" + (position?.let { " position $position" } ?: "")

fun String.highlight(term: String?): AnnotatedString {
    val builder = AnnotatedString.Builder(this)

    if (term != null) {
        val matches = Regex(term, RegexOption.IGNORE_CASE).findAll(this)
        val style = SpanStyle(fontWeight = FontWeight.Bold)

        matches.forEach { builder.addStyle(style, it.range.first, it.range.last + 1) }
    }
    return builder.toAnnotatedString()
}
