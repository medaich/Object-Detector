package com.example.objectdetector.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.dimensionResource
import com.example.objectdetector.R

@Composable
fun CustomIconButton (
    onClick : () -> Unit,
    icon: ImageVector,
    iconDescription: String,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = {
            onClick()
        },
        modifier = Modifier
            .padding(dimensionResource(R.dimen.extra_small))
            .then(modifier)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = iconDescription,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .size(dimensionResource(R.dimen.small_item))
                .background(
                    color = MaterialTheme.colorScheme.primary.copy(
                        alpha = .3f
                    )
                )
                .padding(dimensionResource(R.dimen.extra_small))
        )
    }
}