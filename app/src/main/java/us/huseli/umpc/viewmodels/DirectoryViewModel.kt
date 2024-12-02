package us.huseli.umpc.viewmodels

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import us.huseli.umpc.data.MPDDirectory
import us.huseli.umpc.data.MPDSong
import us.huseli.umpc.repository.MPDRepository
import us.huseli.umpc.repository.MessageRepository
import us.huseli.umpc.viewmodels.abstr.BaseViewModel
import javax.inject.Inject

@HiltViewModel
class DirectoryViewModel @Inject constructor(
    repo: MPDRepository,
    messageRepo: MessageRepository,
) : BaseViewModel(repo, messageRepo) {
    private val _directories = MutableStateFlow<Map<String, MPDDirectory>>(emptyMap())
    private val _openDirectories = MutableStateFlow(setOf(""))

    val currentSongFilename = repo.currentSong.map { it?.filename }.distinctUntilChanged()
    val openDirectories = _openDirectories.asStateFlow()

    fun getDirectoryContents(path: String) = MutableStateFlow(MPDDirectory(path, contentsLoaded = false)).apply {
        val directory = _directories.value[path]

        if (directory != null) value = directory
        else repo.getDirectory(path) {
            value = it
            _directories.value += path to it
        }
    }.asStateFlow()

    fun playDirectory(directory: MPDDirectory, startSong: MPDSong) {
        repo.playSongs(directory.songs, directory.songs.indexOf(startSong))
    }

    fun toggleDirectoryOpen(directory: String) {
        _openDirectories.value = _openDirectories.value.toMutableSet().apply {
            if (contains(directory)) {
                if (directory != "") remove(directory)
            } else add(directory)
        }
    }
}