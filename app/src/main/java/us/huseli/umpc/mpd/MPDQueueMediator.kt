package us.huseli.umpc.mpd

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import us.huseli.umpc.data.MPDSong

@OptIn(ExperimentalPagingApi::class)
class MPDQueueMediator : RemoteMediator<Int, MPDSong>() {
    override suspend fun load(loadType: LoadType, state: PagingState<Int, MPDSong>): MediatorResult {
        TODO("Not yet implemented")
    }
}