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
    val albumArtRepo: AlbumArtRepository,
) : BaseViewModel(repo, messageRepo) {
    val currentSongAlbumArt = albumArtRepo.currentSongAlbumArt

    inline fun getAlbumArt(key: AlbumArtKey, crossinline onFinish: (MPDAlbumArt) -> Unit) =
        albumArtRepo.getAlbumArt(key) { onFinish(it) }

    inline fun getAlbumArt(album: MPDAlbumWithSongs, crossinline onFinish: (MPDAlbumArt) -> Unit) =
        album.albumArtKey?.let { key -> getAlbumArt(key, onFinish) }
}
