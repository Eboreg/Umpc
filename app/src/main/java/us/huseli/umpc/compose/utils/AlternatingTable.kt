package us.huseli.umpc.compose.utils

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalAbsoluteTonalElevation
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

class AlternatingTableRowScope(val rowScope: RowScope, val cellWeights: List<Float> = emptyList()) {
    var currentIndex = 0

    @Composable
    inline fun Cell(crossinline content: @Composable () -> Unit) {
        val weight = cellWeights.getOrNull(currentIndex)

        val modifier = with(rowScope) {
            if (weight != null) Modifier.weight(weight)
            else Modifier
        }

        Box(modifier = modifier) {
            content()
        }
        currentIndex++
    }
}

class AlternatingTableScope(
    val rowModifier: Modifier = Modifier,
    val cellWeights: List<Float> = emptyList(),
    val evenRowElevation: Dp = 5.dp,
    val oddRowElevation: Dp = 0.dp,
) {
    var currentIndex = 0

    @Composable
    inline fun Row(crossinline content: @Composable AlternatingTableRowScope.() -> Unit) {
        val tonalElevation =
            LocalAbsoluteTonalElevation.current + if (currentIndex % 2 == 0) evenRowElevation else oddRowElevation

        CompositionLocalProvider(LocalAbsoluteTonalElevation provides tonalElevation) {
            Surface {
                Row(modifier = rowModifier) {
                    with(AlternatingTableRowScope(this, cellWeights = cellWeights)) { content() }
                }
            }
        }
        currentIndex++
    }
}

@Composable
fun AlternatingTable(
    modifier: Modifier = Modifier,
    rowModifier: Modifier = Modifier.padding(10.dp),
    cellWeights: List<Float> = emptyList(),
    evenRowElevation: Dp = 5.dp,
    oddRowElevation: Dp = 0.dp,
    content: @Composable AlternatingTableScope.() -> Unit,
) {
    Column(modifier = modifier) {
        with(AlternatingTableScope(rowModifier, cellWeights, evenRowElevation, oddRowElevation)) { content() }
    }
}
