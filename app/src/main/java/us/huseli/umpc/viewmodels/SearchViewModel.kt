package us.huseli.umpc.viewmodels

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.text.input.TextFieldValue
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import us.huseli.umpc.data.MPDSong
import us.huseli.umpc.data.MPDVersion
import us.huseli.umpc.mpd.command.MPDMapCommand
import us.huseli.umpc.mpd.mpdSearch
import us.huseli.umpc.mpd.mpdSearchPre021
import us.huseli.umpc.mpd.response.MPDBatchMapResponse
import us.huseli.umpc.repository.MPDRepository
import us.huseli.umpc.viewmodels.abstr.SongSelectViewModel
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
        repo.client.enqueueBatch(
            commands = _songSearchResults.value.map {
                MPDMapCommand("playlistadd", listOf(playlistName, it.filename))
            },
            onFinish = onFinish,
        )

    fun clearSearchTerm() {
        _songSearchTerm.value = _songSearchTerm.value.copy(text = "")
    }

    fun enqueueAll(onFinish: (MPDBatchMapResponse) -> Unit) =
        repo.client.enqueueBatch(
            commands = _songSearchResults.value.map { MPDMapCommand("add", it.filename) },
            onFinish = onFinish,
        )

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

    private fun search(term: String, onFinish: (List<MPDSong>) -> Unit) {
        /**
         * MPD cannot combine search terms with logical OR for some reason, so
         * we cannot select a list of tags to search, but must use "any". As
         * this may give a lot of search results we don't want, additional
         * filtering must be applied.
         */
        if (term.isNotEmpty()) {
            // TODO: Maybe disable search all together in <0.21, or implement
            // it in some completely different way
            val command =
                if (repo.protocolVersion.value < MPDVersion("0.21")) mpdSearchPre021 { equals("any", term) }
                else mpdSearch { contains("any", term) }
            repo.client.enqueueMultiMap(command) { response ->
                onFinish(
                    response.extractSongs().filter {
                        it.album.name.contains(term, true) ||
                        it.artist.contains(term, true) ||
                        it.album.artist.contains(term, true) ||
                        it.title.contains(term, true)
                    }
                )
            }
        }
    }
}
