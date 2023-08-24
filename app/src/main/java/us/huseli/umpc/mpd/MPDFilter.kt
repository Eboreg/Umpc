package us.huseli.umpc.mpd

import us.huseli.umpc.data.MPDServerCapability
import us.huseli.umpc.data.MPDVersion
import us.huseli.umpc.escapeQuotes

@JvmInline
value class EscapedValue(private val original: String) {
    override fun toString() = original.escapeQuotes()
}

data class MPDFilterAtom(
    private val key: String,
    private val value: EscapedValue,
    private val comparator: Comparator,
    private var negative: Boolean = false,
) {
    enum class Comparator { EQUALS, NOT_EQUALS, CONTAINS, REGEX }

    constructor(key: String, value: String, comparator: Comparator) : this(key, EscapedValue(value), comparator)

    fun invert() {
        negative = !negative
    }

    fun toString(protocolVersion: MPDVersion?): String {
        return if (protocolVersion?.hasCapability(MPDServerCapability.NEW_FILTER_SYNTAX) != true) "$key \"$value\""
        else {
            val raw = when (comparator) {
                Comparator.EQUALS -> "($key == \"$value\")"
                Comparator.NOT_EQUALS -> "($key != \"$value\")"
                Comparator.CONTAINS -> "($key contains \"$value\")"
                Comparator.REGEX -> "($key =~ \"$value\")"
            }
            if (negative) "(!$raw)" else raw
        }
    }
}

open class MPDFilter {
    private var atoms = setOf<MPDFilterAtom>()

    fun find(protocolVersion: MPDVersion?) = "find ${render(protocolVersion)}"

    fun findadd(protocolVersion: MPDVersion?) = "findadd ${render(protocolVersion)}"

    fun findadd(protocolVersion: MPDVersion?, position: Int): String {
        return if (protocolVersion?.hasCapability(MPDServerCapability.SEARCHADD_POSITION) == true)
            "findadd ${render(protocolVersion)} position $position"
        else "findadd ${render(protocolVersion)}"
    }

    fun findaddRelative(protocolVersion: MPDVersion?, position: Int): String {
        return if (protocolVersion?.hasCapability(MPDServerCapability.SEARCHADD_RELATIVE_POSITION) == true)
            "findadd ${render(protocolVersion)} position ${if (position >= 0) "+$position" else position.toString()}"
        else "findadd ${render(protocolVersion)}"
    }

    fun search(protocolVersion: MPDVersion?) = "search ${render(protocolVersion)}"

    fun searchaddpl(protocolVersion: MPDVersion?, playlistName: String) =
        "searchaddpl \"${playlistName.escapeQuotes()}\" ${render(protocolVersion)}"

    infix fun and(other: MPDFilter): MPDFilter = apply { atoms += other.atoms }

    infix fun String.eq(other: String): MPDFilter =
        addAtom(this, other, MPDFilterAtom.Comparator.EQUALS)

    infix fun String.ne(other: String): MPDFilter =
        addAtom(this, other, MPDFilterAtom.Comparator.NOT_EQUALS)

    infix fun String.contains(other: String): MPDFilter =
        addAtom(this, other, MPDFilterAtom.Comparator.CONTAINS)

    infix fun String.regex(other: String): MPDFilter =
        addAtom(this, other, MPDFilterAtom.Comparator.REGEX)

    operator fun not(): MPDFilter = apply {
        // Local copy to avoid concurrent updates:
        val localAtoms = atoms.toSet()
        localAtoms.forEach { it.invert() }
        atoms = localAtoms
    }

    private fun addAtom(key: String, value: String, comparator: MPDFilterAtom.Comparator): MPDFilter {
        atoms += MPDFilterAtom(key, value, comparator)
        return this
    }

    fun render(protocolVersion: MPDVersion?): String {
        return if (!isNewSyntax(protocolVersion))
            atoms.joinToString(" ") { it.toString(protocolVersion) }
        else {
            val raw =
                if (atoms.size == 1) atoms.first().toString(protocolVersion)
                else "(${atoms.joinToString(" AND ") { it.toString(protocolVersion) }})"
            "\"${raw.escapeQuotes()}\""
        }
    }

    fun isNewSyntax(protocolVersion: MPDVersion?) =
        protocolVersion?.hasCapability(MPDServerCapability.NEW_FILTER_SYNTAX) == true
}

inline fun mpdFilter(block: MPDFilter.() -> MPDFilter): MPDFilter = with(MPDFilter()) { block() }

inline fun mpdFind(protocolVersion: MPDVersion?, sort: String? = null, filter: MPDFilter.() -> MPDFilter): String =
    with(MPDFilter()) {
        "find ${filter().render(protocolVersion)}" + if (sort != null && isNewSyntax(protocolVersion)) " sort $sort" else ""
    }

inline fun mpdList(
    type: String,
    protocolVersion: MPDVersion?,
    group: String? = null,
    filter: MPDFilter.() -> MPDFilter,
): String = with(MPDFilter()) {
    "list $type ${filter().render(protocolVersion)}" + if (group != null) " group $group" else ""
}
