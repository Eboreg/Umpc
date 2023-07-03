package us.huseli.umpc.viewmodels

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import us.huseli.umpc.data.MPDSong
import us.huseli.umpc.mpd.MPDRepository
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(repo: MPDRepository) : BaseViewModel(repo) {
    private val _songSearchTerm = MutableStateFlow("")
    private val _songSearchResults = MutableStateFlow<List<MPDSong>>(emptyList())

    val songSearchResults = _songSearchResults.asStateFlow()
    val songSearchTerm = _songSearchTerm.asStateFlow()

    fun setSongSearchTerm(value: String) {
        _songSearchTerm.value = value
        if (value.length >= 3) {
            repo.search(value) { _songSearchResults.value = it }
        }
    }
}
