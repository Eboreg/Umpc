package us.huseli.umpc.viewmodels

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.text.input.TextFieldValue
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import us.huseli.umpc.data.MPDSong
import us.huseli.umpc.mpd.MPDRepository
import us.huseli.umpc.mpd.response.MPDBatchMapResponse
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(repo: MPDRepository) : SongSelectViewModel(repo) {
    private val _songSearchTerm = MutableStateFlow(TextFieldValue())
    private val _songSearchResults = MutableStateFlow<List<MPDSong>>(emptyList())
    private val _isSearching = MutableStateFlow(false)

    val songSearchResults = _songSearchResults.asStateFlow()
    val songSearchTerm = _songSearchTerm.asStateFlow()
    val isSearching = _isSearching.asStateFlow()
    val listState = LazyListState()

    fun addAllToPlaylist(playlistName: String, onFinish: (MPDBatchMapResponse) -> Unit) =
        repo.engines.playlist.addSongsToStoredPlaylist(_songSearchResults.value, playlistName, onFinish)

    fun enqueueAll() {
        _songSearchResults.value.forEach { song ->
            repo.engines.control.enqueueSongLast(song)
        }
    }

    fun setSongSearchTerm(value: TextFieldValue) {
        _songSearchTerm.value = value
        if (value.text.length >= 3) {
            _isSearching.value = true
            repo.search(value.text) {
                _songSearchResults.value = it
                _isSearching.value = false
            }
        }
    }
}
