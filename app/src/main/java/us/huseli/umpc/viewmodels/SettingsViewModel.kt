package us.huseli.umpc.viewmodels

import android.content.Context
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import us.huseli.umpc.data.MPDServerCredentials
import us.huseli.umpc.mpd.OnMPDChangeListener
import us.huseli.umpc.mpd.response.MPDTextResponse
import us.huseli.umpc.repository.MPDRepository
import us.huseli.umpc.repository.MessageRepository
import us.huseli.umpc.repository.SettingsRepository
import us.huseli.umpc.viewmodels.abstr.BaseViewModel
import java.io.File
import java.io.FileFilter
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    repo: MPDRepository,
    messageRepo: MessageRepository,
    private val settingsRepo: SettingsRepository,
    @ApplicationContext context: Context,
) : BaseViewModel(repo, messageRepo), OnMPDChangeListener {
    private val _selectedServerIdx = MutableStateFlow(settingsRepo.currentServerIdx.value)
    private val albumArtDirectory = File(context.cacheDir, "albumArt").apply { mkdirs() }
    private val thumbnailDirectory = File(albumArtDirectory, "thumbnails").apply { mkdirs() }

    val outputs = repo.outputs
    val servers = settingsRepo.servers
    val selectedServerIdx = _selectedServerIdx.asStateFlow()
    val fetchSpotifyAlbumArt = settingsRepo.fetchSpotifyAlbumArt

    init {
        repo.loadOutputs()

        viewModelScope.launch {
            repo.connectedServer.collect {
                repo.loadOutputs()
            }
        }
    }

    fun addServer(server: MPDServerCredentials) {
        settingsRepo.addServer(server)
        _selectedServerIdx.value = settingsRepo.servers.value.lastIndex
    }

    fun clearAlbumArtCache(onFinish: (() -> Unit)? = null) = viewModelScope.launch {
        albumArtDirectory.listFiles(FileFilter { it.isFile })?.forEach { it.delete() }
        thumbnailDirectory.listFiles(FileFilter { it.isFile })?.forEach { it.delete() }
        onFinish?.invoke()
    }

    fun deleteServer(index: Int) {
        settingsRepo.deleteServer(index)
        _selectedServerIdx.value = settingsRepo.currentServerIdx.value
    }

    fun save() {
        _selectedServerIdx.value?.let { settingsRepo.setServerIdx(it) }
        settingsRepo.save()
    }

    fun selectServer(index: Int) {
        _selectedServerIdx.value = index
    }

    fun setFetchSpotifyAlbumArt(value: Boolean) = settingsRepo.setFetchSpotifyAlbumArt(value)

    fun setOutputEnabled(id: Int, isEnabled: Boolean) = repo.setOutputEnabled(id, isEnabled)

    inline fun updateDatabase(crossinline onFinish: (MPDTextResponse) -> Unit, crossinline onUpdateFinish: () -> Unit) =
        repo.updateDatabase(onFinish = { onFinish(it) }, onUpdateFinish = { onUpdateFinish() })

    fun updateServer(index: Int, server: MPDServerCredentials) = settingsRepo.updateServer(index, server)

    override fun onMPDChanged(subsystems: List<String>) {
        if (subsystems.contains("output")) repo.loadOutputs()
    }
}
