package com.example.objectdetector.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.objectdetector.R


enum class StepStatus {
    NOT_STARTED,
    IN_PROGRESS,
    COMPLETED
}

fun currStatus(stepIdx: Int , currStepIdx: Int) : StepStatus {
   return when {
        stepIdx < currStepIdx -> StepStatus.COMPLETED
        stepIdx == currStepIdx -> StepStatus.IN_PROGRESS
        else -> StepStatus.NOT_STARTED
    }
}

@Composable
fun StepsIndicator(
    steps: List<Int>,
    currStepIdx: Int,
    modifier: Modifier = Modifier
) {
    Column (
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.medium)),
        modifier = modifier
    ) {
        StepsRow(
            modifier = Modifier
                .padding(horizontal = dimensionResource(R.dimen.small)
               )
        ) {
            for (i in steps.indices) {
                StepNumber(
                        stepNumber = i + 1,
                        stepStatus = currStatus(i, currStepIdx),
                        modifier = Modifier
                )

                if (i < steps.size - 1)
                    StepDivider(
                        stepStatus = currStatus(i, currStepIdx),
                        modifier = Modifier.weight(1f)
                    )
            }
        }
        StepsRow (
            modifier = Modifier
        ) {
            steps.forEachIndexed { index, label ->
                StepWrapper (
                    stepAlignment = when {
                        index == 0 -> Alignment.CenterStart
                        index == steps.size - 1 -> Alignment.CenterEnd
                        else -> Alignment.Center
                    },
                    modifier = Modifier.weight(1f)) {
                 StepLabel(
                     stepLabel = stringResource(label),
                     stepStatus = currStatus(index, currStepIdx),
                     labelAlignment = when {
                         index == 0 -> TextAlign.Start
                         index == steps.size - 1 -> TextAlign.End
                         else -> TextAlign.Center
                     },
                     modifier = Modifier
                 )
                }
            }
        }

    }
}

@Composable
fun StepLabel(
    stepLabel: String,
    stepStatus: StepStatus,
    labelAlignment: TextAlign,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints (
        contentAlignment = Alignment.Center,
        modifier = modifier
    ) {
        val isNarrow = maxWidth < 200.dp
        Text(
            text = stepLabel,
            style = MaterialTheme.typography.labelMedium.apply {
                when(stepStatus) {
                    StepStatus.IN_PROGRESS -> copy(fontWeight = FontWeight.Bold)
                    else -> this
                }
            },
            color = when(stepStatus) {
                StepStatus.IN_PROGRESS -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            },
            textAlign = labelAlignment,
            modifier = Modifier
                .then(
                    if (isNarrow)
                        Modifier
                            .width(90.dp)
                    else
                        Modifier
                )
        )
    }

}



@Composable
fun StepNumber(stepNumber: Int, stepStatus: StepStatus, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(
                when(stepStatus) {
                    StepStatus.IN_PROGRESS -> dimensionResource(R.dimen.small_item) + 4.dp
                    else -> dimensionResource(R.dimen.small_item)
                }
            )
            .border(
                width = when(stepStatus) {
                    StepStatus.NOT_STARTED -> 1.dp
                    else -> 1.5.dp
                },
                color = when(stepStatus) {
                    StepStatus.NOT_STARTED -> MaterialTheme.colorScheme.onSurfaceVariant
                    else -> MaterialTheme.colorScheme.primary
                },
                shape = CircleShape
            )
            .background(
                color = when(stepStatus) {
                    StepStatus.COMPLETED -> MaterialTheme.colorScheme.primary
                    else -> Color.Transparent
                },
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stepNumber.toString(),
            style = MaterialTheme.typography.bodyMedium,
            color = when(stepStatus) {
                StepStatus.NOT_STARTED -> MaterialTheme.colorScheme.onSurfaceVariant
                StepStatus.COMPLETED -> MaterialTheme.colorScheme.onPrimary
                else -> MaterialTheme.colorScheme.primary
            }
        )
    }
}

@Composable
fun StepWrapper(
    modifier : Modifier = Modifier,
    stepAlignment: Alignment = Alignment.CenterStart,
    content: @Composable () -> Unit
) {
    Box (
        contentAlignment = stepAlignment,
        modifier = modifier
    ){
        content()
    }
}

@Composable
fun StepsRow(modifier: Modifier = Modifier, content: @Composable () -> Unit ){
    Row (
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
    ){
        content()
    }
}

@Composable
fun StepDivider(stepStatus: StepStatus, modifier: Modifier = Modifier) {
    HorizontalDivider(
        thickness = when(stepStatus) {
            StepStatus.COMPLETED -> 1.5.dp
            else -> 1.dp
        },
        color = when(stepStatus) {
            StepStatus.COMPLETED -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        },
        modifier = modifier
    )
}