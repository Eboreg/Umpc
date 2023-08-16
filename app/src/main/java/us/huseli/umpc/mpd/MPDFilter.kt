package us.huseli.umpc.mpd

fun escape(string: String) = string
    .replace("\"", "\\\"")
    .replace("'", "\\'")
    .replace("\\\\\"", "\\\\\\\"")

abstract class BaseMPDFilterContext {
    abstract fun equals(tag: String, value: String): BaseMPDFilter
}

object MPDFilterContext : BaseMPDFilterContext() {
    override fun equals(tag: String, value: String) = MPDFilter("($tag == \"${escape(value)}\")")

    fun notEquals(tag: String, value: String) = MPDFilter("($tag != \"${escape(value)}\")")

    fun contains(tag: String, value: String) = MPDFilter("($tag contains \"${escape(value)}\")")

    fun regex(tag: String, value: String) = MPDFilter("($tag =~ \"${escape(value)}\")")
}

object MPDFilterContextPre021 : BaseMPDFilterContext() {
    override fun equals(tag: String, value: String) = MPDFilterPre021("$tag \"${escape(value)}\"")
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

    fun findadd(position: Int): String = "findadd \"${escape(term)}\" position $position"

    fun findaddRelative(position: Int): String {
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

inline fun mpdFindAdd(position: Int? = null, block: MPDFilterContext.() -> MPDFilter) = with(MPDFilterContext) {
    if (position != null) block().findadd(position)
    else block().findadd()
}

inline fun mpdFindAddRelative(position: Int, block: MPDFilterContext.() -> MPDFilter) =
    with(MPDFilterContext) { block().findaddRelative(position) }

inline fun mpdFindAddPre021(block: MPDFilterContextPre021.() -> MPDFilterPre021) =
    with(MPDFilterContextPre021) { block().findadd() }
