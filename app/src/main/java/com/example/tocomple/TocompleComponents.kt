package com.example.tocomple

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.util.Locale
import kotlinx.coroutines.delay

@Composable
fun PurchaseSummaryCard(
    selectedType: CompletoType,
    calculation: CompletoCalculation?
) {
    Card(
        modifier = Modifier.animateContentSize(
            animationSpec = spring(
                dampingRatio = 0.9f,
                stiffness = 500f
            )
        ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Lista de compra",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Text(
                    text = selectedType.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                )
            }

            if (calculation == null) {
                PurchaseSummaryLoadingContent(selectedType)
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                SummaryPill(
                    modifier = Modifier.weight(1f),
                    count = calculation.totalCompletos,
                    singular = "completo",
                    plural = "completos"
                )
                SummaryPill(
                    modifier = Modifier.weight(1f),
                    count = calculation.totalCompletos,
                    singular = "pan",
                    plural = "panes"
                )
                SummaryPill(
                    modifier = Modifier.weight(1f),
                    count = calculation.totalCompletos,
                    singular = calculation.completoType.proteinSingular,
                    plural = calculation.completoType.proteinPlural
                )
                }
            }
        }
    }
}

@Composable
private fun PurchaseSummaryLoadingContent(selectedType: CompletoType) {
    val transition = rememberInfiniteTransition(label = "summary_loading")
    val shimmerOffset by transition.animateFloat(
        initialValue = -200f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1300),
            repeatMode = RepeatMode.Restart
        ),
        label = "summary_loading_offset"
    )

    val shimmerBrush = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.10f),
            MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.28f),
            MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.10f)
        ),
        start = androidx.compose.ui.geometry.Offset(shimmerOffset, 0f),
        end = androidx.compose.ui.geometry.Offset(shimmerOffset + 220f, 220f)
    )

    Box(
        modifier = Modifier
            .fillMaxWidth(0.25f)
            .height(14.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(shimmerBrush)
    )
    Text(
        text = "Calculando ${selectedType.name.lowercase(Locale.getDefault())}...",
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        repeat(3) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(shimmerBrush)
            )
        }
    }
}

@Composable
fun DetailGrid(calculation: CompletoCalculation) {
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val detailItems = calculation.ingredients.mapIndexed { index, ingredient ->
        DetailItem(
            title = ingredient.name,
            mainValue = formatCount(
                ingredient.unitEstimate,
                ingredient.unitLabelSingular,
                ingredient.unitLabelPlural
            ),
            supporting = "${formatWeight(ingredient.totalKg)} aprox.",
            accent = ingredientAccent(index)
        )
    } + listOf(
        DetailItem(
            title = "Base",
            mainValue = formatCompleteLabel(calculation.baseCompletos),
            supporting = "${formatPersonLabel(calculation.people)} x ${calculation.completosPerPerson}",
            accent = MaterialTheme.colorScheme.tertiaryContainer
        ),
        DetailItem(
            title = "Extras",
            mainValue = formatCount(calculation.extraCompletos, "extra", "extras"),
            supporting = "Sumados manualmente",
            accent = MaterialTheme.colorScheme.primaryContainer
        )
    )

    val singleColumn = screenWidthDp < 390

    if (singleColumn) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            detailItems.forEachIndexed { index, item ->
                DetailCard(
                    modifier = Modifier.fillMaxWidth(),
                    animationKey = "${calculation.completoType.id}-${calculation.totalCompletos}-$index",
                    itemIndex = index,
                    title = item.title,
                    mainValue = item.mainValue,
                    supporting = item.supporting,
                    accent = item.accent
                )
            }
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            detailItems.chunked(2).forEachIndexed { rowIndex, rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    rowItems.forEachIndexed { columnIndex, item ->
                        DetailCard(
                            modifier = Modifier.weight(1f),
                            animationKey = "${calculation.completoType.id}-${calculation.totalCompletos}-${rowIndex * 2 + columnIndex}",
                            itemIndex = rowIndex * 2 + columnIndex,
                            title = item.title,
                            mainValue = item.mainValue,
                            supporting = item.supporting,
                            accent = item.accent
                        )
                    }
                    repeat(2 - rowItems.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun AssumptionsCard(
    calculation: CompletoCalculation,
    smallAvocados: Boolean
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Supuestos de cálculo",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Estos son los pesos base que usa la calculadora para estimar unidades y cantidades.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = buildAssumptionsText(calculation, smallAvocados),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun AnimatedSelectableChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String
) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.015f else 1f,
        animationSpec = spring(
            dampingRatio = 0.72f,
            stiffness = 700f
        ),
        label = "chip_scale"
    )

    FilterChip(
        selected = selected,
        onClick = onClick,
        modifier = Modifier.graphicsLayer(
            scaleX = scale,
            scaleY = scale
        ),
        label = { Text(label) }
    )
}

@Composable
private fun SummaryPill(
    modifier: Modifier = Modifier,
    count: Int,
    singular: String,
    plural: String
) {
    val animatedCount by animateIntAsState(
        targetValue = count,
        animationSpec = tween(durationMillis = 350),
        label = "summary_count"
    )

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = animatedCount.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = if (animatedCount == 1) singular else plural,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.82f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ingredientAccent(index: Int): Color {
    val accents = listOf(
        MaterialTheme.colorScheme.errorContainer,
        MaterialTheme.colorScheme.secondaryContainer,
        MaterialTheme.colorScheme.tertiaryContainer,
        MaterialTheme.colorScheme.primaryContainer
    )
    return accents[index % accents.size]
}

@Composable
private fun DetailCard(
    modifier: Modifier = Modifier,
    animationKey: String,
    itemIndex: Int,
    title: String,
    mainValue: String,
    supporting: String,
    accent: Color
) {
    var revealed by remember(animationKey) { mutableStateOf(false) }
    LaunchedEffect(animationKey) {
        revealed = false
        delay(40L * itemIndex)
        revealed = true
    }
    val animatedAlpha by animateFloatAsState(
        targetValue = if (revealed) 1f else 0.35f,
        animationSpec = tween(durationMillis = 260),
        label = "detail_alpha"
    )
    val animatedOffsetY by animateFloatAsState(
        targetValue = if (revealed) 0f else 18f,
        animationSpec = tween(durationMillis = 260),
        label = "detail_offset"
    )

    Card(
        modifier = modifier.graphicsLayer(
            alpha = animatedAlpha,
            translationY = animatedOffsetY
        ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(
                    animationSpec = spring(
                        dampingRatio = 0.92f,
                        stiffness = 550f
                    )
                )
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(accent)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = mainValue,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = supporting,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
