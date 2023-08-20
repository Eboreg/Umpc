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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun SmallOutlinedButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    leadingIcon: ImageVector? = null,
    text: String,
    height: Dp = 25.dp,
    textStyle: TextStyle = MaterialTheme.typography.bodySmall,
    enabled: Boolean = true,
) {
    OutlinedButton(
        modifier = modifier.height(height),
        onClick = onClick,
        shape = ShapeDefaults.ExtraSmall,
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
        enabled = enabled,
        content = {
            leadingIcon?.let {
                Icon(imageVector = it, contentDescription = null, modifier = Modifier.size(height).padding(end = 5.dp))
            }
            Text(text = text, style = textStyle)
        },
    )
}
