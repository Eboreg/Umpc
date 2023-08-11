package us.huseli.umpc.mpd

fun escape(string: String) = string
    .replace("\"", "\\\"")
    .replace("'", "\\'")
    .replace("\\\\\"", "\\\\\\\"")

object MPDFilterContext {
    fun equals(tag: String, value: String) = MPDFilter("($tag == \"${escape(value)}\")")

    fun notEquals(tag: String, value: String) = MPDFilter("($tag != \"${escape(value)}\")")

    fun contains(tag: String, value: String) = MPDFilter("($tag contains \"${escape(value)}\")")

    fun regex(tag: String, value: String) = MPDFilter("($tag =~ \"${escape(value)}\")")
}

object MPDFilterContextPre021 {
    fun equals(tag: String, value: String) = MPDFilterPre021("$tag \"${escape(value)}\"")
}

abstract class BaseMPDFilter(protected val term: String) {
    override fun toString() = term

    abstract fun find(): String

    abstract fun search(): String

    abstract fun findadd(): String

    abstract fun list(type: String): String
}

class MPDFilter(term: String) : BaseMPDFilter(term) {
    override fun toString() = term
    infix fun and(other: MPDFilter) = MPDFilter("($this AND $other)")

    fun not() = MPDFilter("(!$this)")

    override fun find() = "find \"${escape(term)}\""

    override fun search() = "search \"${escape(term)}\""

    override fun findadd() = "findadd \"${escape(term)}\""

    fun findadd(position: Int?): String {
        if (position == null) return findadd()
        val pos = if (position >= 0) "+${position}" else position.toString()
        return "findadd \"${escape(term)}\" position $pos"
    }

    override fun list(type: String) = "list $type \"${escape(term)}\""

    fun list(type: String, group: List<String>) =
        "list $type \"${escape(term)}\" " + group.joinToString(" ") { "group $it" }
}

class MPDFilterPre021(term: String) : BaseMPDFilter(term) {
    infix fun and(other: MPDFilterPre021) = MPDFilterPre021("$this $other")

    override fun find() = "find $term"

    override fun search() = "search $term"

    override fun findadd() = "findadd $term"

    override fun list(type: String) = "list $type $term"
}

inline fun mpdFilter(block: MPDFilterContext.() -> MPDFilter) = with(MPDFilterContext) { block() }

inline fun mpdFilterPre021(block: MPDFilterContextPre021.() -> MPDFilterPre021) =
    with(MPDFilterContextPre021) { block() }

inline fun mpdSearch(block: MPDFilterContext.() -> MPDFilter) = with(MPDFilterContext) { block().search() }

inline fun mpdSearchPre021(block: MPDFilterContextPre021.() -> MPDFilterPre021) =
    with(MPDFilterContextPre021) { block().search() }

inline fun mpdFind(block: MPDFilterContext.() -> MPDFilter) = with(MPDFilterContext) { block().find() }

inline fun mpdFindAdd(position: Int?, block: MPDFilterContext.() -> MPDFilter) =
    with(MPDFilterContext) { block().findadd(position) }

inline fun mpdFindPre021(block: MPDFilterContextPre021.() -> MPDFilterPre021) =
    with(MPDFilterContextPre021) { block().find() }

inline fun mpdList(type: String, group: List<String>, block: MPDFilterContext.() -> MPDFilter) =
    with(MPDFilterContext) { block().list(type, group) }

inline fun mpdList(type: String, group: String, block: MPDFilterContext.() -> MPDFilter) =
    mpdList(type, listOf(group), block)

inline fun mpdList(type: String, block: MPDFilterContext.() -> MPDFilter) = mpdList(type, emptyList(), block)

inline fun mpdListPre021(type: String, block: MPDFilterContextPre021.() -> MPDFilterPre021) =
    with(MPDFilterContextPre021) { block().list(type) }
