package us.huseli.umpc.data

data class MPDOutput(
    val id: Int,
    val name: String,
    val plugin: String,
    val isEnabled: Boolean,
)

fun Map<String, String>.toMPDOutput() = try {
    MPDOutput(
        id = this["outputid"]!!.toInt(),
        name = this["outputname"]!!,
        plugin = this["plugin"]!!,
        isEnabled = this["outputenabled"]!! == "1"
    )
} catch (e: NullPointerException) {
    null
}
