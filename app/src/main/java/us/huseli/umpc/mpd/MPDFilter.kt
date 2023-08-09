package us.huseli.umpc.mpd

object MPDFilterContext {
    fun escape(string: String) = string
        .replace("\"", "\\\"")
        .replace("'", "\\'")
        .replace("\\\\\"", "\\\\\\\"")

    fun equals(tag: String, value: String) = MPDFilter("($tag == \"${escape(value)}\")")
    fun notEquals(tag: String, value: String) = MPDFilter("($tag != \"${escape(value)}\")")
    fun contains(tag: String, value: String) = MPDFilter("($tag contains \"${escape(value)}\")")
    fun regex(tag: String, value: String) = MPDFilter("($tag =~ \"${escape(value)}\")")
}

open class MPDFilter(private val term: String) {
    override fun toString() = term

    // open fun and(other: MPDFilter) = MPDAndFilter(this, other)

    infix fun and(other: MPDFilter) = MPDFilter("($this AND $other)")

    fun not() = MPDFilter("(!$this)")

    fun find() = "find \"${MPDFilterContext.escape(term)}\""

    fun search() = "search \"${MPDFilterContext.escape(term)}\""

    fun findadd() = "findadd \"${MPDFilterContext.escape(term)}\""

    fun findadd(position: Int? = null): String {
        if (position == null) return findadd()
        val pos = if (position >= 0) "+${position}" else position.toString()
        return "findadd \"${MPDFilterContext.escape(term)}\" position $pos"
    }

    fun list(type: String, group: List<String> = emptyList()) =
        "list $type \"${MPDFilterContext.escape(term)}\" " + group.map { "group $it" }.joinToString(" ")
}

inline fun mpdFilter(block: MPDFilterContext.() -> MPDFilter) = with(MPDFilterContext) { block() }
inline fun mpdSearch(block: MPDFilterContext.() -> MPDFilter) = with(MPDFilterContext) { block().search() }
inline fun mpdFind(block: MPDFilterContext.() -> MPDFilter) = with(MPDFilterContext) { block().find() }
inline fun mpdList(type: String, group: List<String>, block: MPDFilterContext.() -> MPDFilter) =
    with(MPDFilterContext) { block().list(type, group) }
inline fun mpdList(type: String, group: String, block: MPDFilterContext.() -> MPDFilter) =
    mpdList(type, listOf(group), block)
inline fun mpdList(type: String, block: MPDFilterContext.() -> MPDFilter) = mpdList(type, emptyList(), block)
