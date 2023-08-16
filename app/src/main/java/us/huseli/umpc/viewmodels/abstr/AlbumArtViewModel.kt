package us.huseli.umpc.viewmodels.abstr

import android.content.Context
import us.huseli.umpc.ImageRequestType
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
    context: Context,
) : BaseViewModel(repo, messageRepo, context) {
    val currentSongAlbumArt = albumArtRepo.currentSongAlbumArt

    fun getAlbumArt(
        key: AlbumArtKey,
        requestType: ImageRequestType,
        onFinish: (MPDAlbumArt) -> Unit
    ) = albumArtRepo.getAlbumArt(key, requestType, onFinish)

    fun getThumbnail(album: MPDAlbumWithSongs, onFinish: (MPDAlbumArt) -> Unit) =
        album.albumArtKey?.let { getAlbumArt(it, ImageRequestType.THUMBNAIL, onFinish) }
}