package com.example.objectdetector.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.objectdetector.R
import com.example.objectdetector.model.ApiType

@Composable
fun ApiSelector(
    currApiIdx: Int?,
    apiList: List<ApiType>,
    onApiSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.medium)),
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = stringResource(R.string.api_selector),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.medium)),
            modifier = Modifier.fillMaxWidth()
        ) {
            apiList.forEachIndexed { index, option ->
                val isSelected = index == currApiIdx

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .clip(MaterialTheme.shapes.large)
                        .background(
                            color = when {
                                isSelected -> MaterialTheme.colorScheme.primaryContainer
                                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                            }
                        )
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = when {
                                isSelected -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            },
                            shape = MaterialTheme.shapes.large
                        )
                        .clickable {
                                onApiSelected(index)
                        }
                        .padding(16.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.small))
                    ) {
                        Icon(
                            painter = painterResource(option.icon),
                            contentDescription = stringResource(option.label),
                            tint = when {
                                isSelected -> MaterialTheme.colorScheme.secondary
                                else -> MaterialTheme.colorScheme.tertiary
                            },
                            modifier = Modifier.size(dimensionResource(R.dimen.medium_item))
                        )

                        Text(
                            text = stringResource(option.description),
                            style = MaterialTheme.typography.bodySmall.copy(
                                lineHeight = 20.sp
                            ),
                            color = when {
                                isSelected -> MaterialTheme.colorScheme.secondary
                                else -> MaterialTheme.colorScheme.tertiary
                            },
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}