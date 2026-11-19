package com.csiss.inventory.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class SummaryStat(
    val label: String,
    val value: String,
    val valueColor: Color = Color.White
)

@Composable
fun SummaryStatsCard(
    stats: List<SummaryStat>
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 100.dp)
            .background(
                brush = Brush.linearGradient(
                    listOf(
                        Color(0xFF312E81),
                        Color(0xFF4C1D95),
                        Color(0xFF6D28D9)
                    )
                ),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(
                horizontal = 8.dp,
                vertical = 20.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        stats.forEachIndexed { index, stat ->
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = stat.value,
                    color = stat.valueColor,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = stat.label,
                    color = Color(0xFFC4B5FD),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (index < stats.lastIndex) {
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(40.dp)
                        .background(Color(0xFF7C3AED))
                )
            }
        }
    }
}
