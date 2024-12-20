package us.huseli.umpc.viewmodels

import androidx.compose.ui.text.input.TextFieldValue
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import us.huseli.umpc.data.MPDSong
import us.huseli.umpc.mpd.response.MPDBatchTextResponse
import us.huseli.umpc.repository.AlbumArtRepository
import us.huseli.umpc.repository.MPDRepository
import us.huseli.umpc.repository.MessageRepository
import us.huseli.umpc.viewmodels.abstr.SongSelectViewModel
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    repo: MPDRepository,
    messageRepo: MessageRepository,
    albumArtRepo: AlbumArtRepository,
) : SongSelectViewModel(repo, messageRepo, albumArtRepo) {
    private val _songSearchTerm = MutableStateFlow(TextFieldValue())
    private val _songSearchResults = MutableStateFlow<List<MPDSong>>(emptyList())
    private val _isSearching = MutableStateFlow(false)

    val songSearchResults = _songSearchResults.asStateFlow()
    val songSearchTerm = _songSearchTerm.asStateFlow()
    val isSearching = _isSearching.asStateFlow()

    inline fun addAllToPlaylist(playlistName: String, crossinline onFinish: (MPDBatchTextResponse) -> Unit) =
        repo.addSongsToPlaylist(songSearchResults.value, playlistName, onFinish)

    fun clearSearchTerm() {
        _songSearchTerm.value = _songSearchTerm.value.copy(text = "")
    }

    inline fun enqueueAll(crossinline onFinish: (MPDBatchTextResponse) -> Unit) =
        repo.enqueueSongs(songSearchResults.value.map { it.filename }, onFinish)

    fun setSongSearchTerm(value: TextFieldValue) {
        _songSearchTerm.value = value
        if (value.text.length >= 3) {
            _isSearching.value = true
            search(value.text) {
                _songSearchResults.value = it
                _isSearching.value = false
            }
        }
    }

    private inline fun search(term: String, crossinline onFinish: (List<MPDSong>) -> Unit) {
        if (term.isNotEmpty()) {
            repo.search(term) { response ->
                onFinish(
                    response.extractSongs().filter {
                        it.album.name.contains(term, true) ||
                                it.artist.contains(term, true) ||
                                it.album.artist.contains(term, true) ||
                                it.title.contains(term, true) ||
                                it.filename.contains(term, true)
                    }
                )
            }
        }
    }
}
