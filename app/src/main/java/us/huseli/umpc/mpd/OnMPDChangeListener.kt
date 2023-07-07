package us.huseli.umpc.mpd

interface OnMPDChangeListener {
    fun onMPDChanged(subsystems: List<String>)
}
