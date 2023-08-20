package us.huseli.umpc.viewmodels.abstr

import us.huseli.umpc.data.AlbumArtKey
import us.huseli.umpc.data.MPDAlbumArt
import us.huseli.umpc.data.MPDAlbumWithSongs
import us.huseli.umpc.repository.AlbumArtRepository
import us.huseli.umpc.repository.MPDRepository
import us.huseli.umpc.repository.MessageRepository

abstract class AlbumArtViewModel(
    repo: MPDRepository,
    messageRepo: MessageRepository,
    private val albumArtRepo: AlbumArtRepository,
) : BaseViewModel(repo, messageRepo) {
    val currentSongAlbumArt = albumArtRepo.currentSongAlbumArt

    fun getAlbumArt(key: AlbumArtKey, onFinish: (MPDAlbumArt) -> Unit) = albumArtRepo.getAlbumArt(key, onFinish)

    fun getAlbumArt(album: MPDAlbumWithSongs, onFinish: (MPDAlbumArt) -> Unit) =
        album.albumArtKey?.let { getAlbumArt(it, onFinish) }
}
