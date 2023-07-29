package us.huseli.umpc.mpd.client

import kotlinx.coroutines.CoroutineScope
import us.huseli.umpc.repository.SettingsRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MPDClient @Inject constructor(
    ioScope: CoroutineScope,
    settingsRepository: SettingsRepository
) : MPDBaseClient(ioScope, settingsRepository)
