package us.huseli.umpc.compose.utils

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
fun SmallOutlinedButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    leadingIcon: ImageVector? = null,
    text: String,
) {
    OutlinedButton(
        modifier = modifier.height(25.dp),
        onClick = onClick,
        shape = ShapeDefaults.ExtraSmall,
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
        content = {
            leadingIcon?.let {
                Icon(it, null, modifier = Modifier.size(30.dp).padding(end = 5.dp))
            }
            Text(text, style = MaterialTheme.typography.bodySmall)
        },
    )
}
