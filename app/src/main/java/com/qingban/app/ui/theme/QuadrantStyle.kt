package com.qingban.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.qingban.app.domain.Quadrant

data class QuadrantColors(
    val accent: Color,
    val container: Color,
    val onContainer: Color,
)

@Composable
fun quadrantColors(quadrant: Quadrant): QuadrantColors {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    return when (quadrant) {
        Quadrant.DO_NOW -> QuadrantColors(
            accent = if (dark) Color(0xFFE09A84) else Q1Color,
            container = if (dark) Q1SoftDark else Q1Soft,
            onContainer = if (dark) Color(0xFFF3C7BA) else Color(0xFF54291E),
        )

        Quadrant.PLAN -> QuadrantColors(
            accent = if (dark) Color(0xFF8FC4B0) else Q2Color,
            container = if (dark) Q2SoftDark else Q2Soft,
            onContainer = if (dark) Color(0xFFC1E4D6) else Color(0xFF1D4236),
        )

        Quadrant.QUICK -> QuadrantColors(
            accent = if (dark) Color(0xFFD8B679) else Q3Color,
            container = if (dark) Q3SoftDark else Q3Soft,
            onContainer = if (dark) Color(0xFFEFD7AA) else Color(0xFF4A371C),
        )

        Quadrant.LATER -> QuadrantColors(
            accent = if (dark) Color(0xFFB2B9AE) else Q4Color,
            container = if (dark) Q4SoftDark else Q4Soft,
            onContainer = if (dark) Color(0xFFD5D9D1) else Color(0xFF3D423C),
        )
    }
}
