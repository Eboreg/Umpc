package us.huseli.umpc

import android.os.Bundle
import android.os.StrictMode
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import us.huseli.umpc.compose.App
import us.huseli.umpc.ui.theme.UmpcTheme
import us.huseli.umpc.viewmodels.MPDViewModel

@AndroidEntryPoint
class MainActivity : ComponentActivity(), LoggerInterface {
    private val viewModel by viewModels<MPDViewModel>()
    private var isStreaming = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isStreaming.collect { isStreaming = it }
            }
        }

        StrictMode.setVmPolicy(
            StrictMode.VmPolicy.Builder(StrictMode.getVmPolicy())
                .detectLeakedClosableObjects()
                .build()
        )

        setContent {
            UmpcTheme {
                App()
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (isStreaming) return super.onKeyDown(keyCode, event)

        return when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> {
                viewModel.onVolumeUpPressed()
                true
            }
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                viewModel.onVolumeDownPressed()
                true
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }
}
