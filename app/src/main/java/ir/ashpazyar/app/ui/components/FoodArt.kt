package ir.ashpazyar.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ir.ashpazyar.app.ui.theme.AshpaTheme
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

private data class ArtPalette(
    val background: List<Color>,
    val plate: Color,
    val food: List<Color>,
    val garnish: Color,
    val steam: Color
)

@Composable
fun FoodArt(
    style: String,
    seed: Int,
    modifier: Modifier = Modifier,
    corner: Dp = 28.dp
) {
    val palette = AshpaTheme.palette
    val art = when (style.uppercase()) {
        "PEACH" -> ArtPalette(
            listOf(Color(0xFFF8DEC7), Color(0xFFEFC09A)),
            Color(0xFFFFFAF2),
            listOf(Color(0xFFC96F4A), Color(0xFFE8A66F), Color(0xFF7D3F28), Color(0xFFF3D28E)),
            Color(0xFF557A50),
            Color(0x66FFFFFF)
        )
        "TERRACOTTA" -> ArtPalette(
            listOf(Color(0xFFD88762), Color(0xFFB65C3C)),
            Color(0xFFF8EBDD),
            listOf(Color(0xFF8F4028), Color(0xFFE2A05E), Color(0xFFF6D6A8), Color(0xFF5B6F3D)),
            Color(0xFFE9D9A5),
            Color(0x55FFF7EB)
        )
        "SAGE" -> ArtPalette(
            listOf(Color(0xFFDCE6D3), Color(0xFFAFC4A6)),
            Color(0xFFFDFBF4),
            listOf(Color(0xFF6E8E5B), Color(0xFFB7C786), Color(0xFFE4D7A7), Color(0xFFC6784E)),
            Color(0xFF4A6C48),
            Color(0x66FFFFFF)
        )
        "CREAM" -> ArtPalette(
            listOf(Color(0xFFF3E6CF), Color(0xFFDCC8A7)),
            Color(0xFFFFFCF5),
            listOf(Color(0xFFC69C5D), Color(0xFF9A6A39), Color(0xFFE8C58A), Color(0xFF7A8A57)),
            Color(0xFF5D7A4B),
            Color(0x66FFFFFF)
        )
        "DARK" -> ArtPalette(
            listOf(Color(0xFF26372B), Color(0xFF14231A)),
            Color(0xFFE9E0C9),
            listOf(Color(0xFFC98D57), Color(0xFF8BA36B), Color(0xFFE0B67A), Color(0xFFA64F35)),
            Color(0xFFC8D8A8),
            Color(0x33F2E9D5)
        )
        else -> ArtPalette(
            listOf(Color(0xFF2C4A37), Color(0xFF17291E)),
            Color(0xFFF3E9D5),
            listOf(Color(0xFFC77A46), Color(0xFF91A76F), Color(0xFFE5C480), Color(0xFFB65337)),
            Color(0xFFD5E1B8),
            Color(0x44F8F0DE)
        )
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(corner))
            .background(art.background.first())
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            drawRect(
                brush = Brush.linearGradient(
                    colors = art.background,
                    start = Offset(0f, 0f),
                    end = Offset(width, height)
                )
            )
            drawCircle(
                color = art.steam.copy(alpha = 0.10f),
                radius = width * 0.52f,
                center = Offset(width * 0.82f, height * 0.16f)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.06f),
                radius = width * 0.30f,
                center = Offset(width * 0.12f, height * 0.88f)
            )
            val plateW = width * 0.68f
            val plateH = plateW * 0.50f
            val plateCenter = Offset(width * 0.50f, height * 0.60f)
            drawOval(
                color = Color.Black.copy(alpha = 0.12f),
                topLeft = Offset(plateCenter.x - plateW / 2f, plateCenter.y - plateH / 2f + height * 0.035f),
                size = Size(plateW, plateH)
            )
            drawOval(
                color = art.plate,
                topLeft = Offset(plateCenter.x - plateW / 2f, plateCenter.y - plateH / 2f),
                size = Size(plateW, plateH)
            )
            drawOval(
                color = art.plate.copy(alpha = 0.55f),
                topLeft = Offset(plateCenter.x - plateW * 0.38f, plateCenter.y - plateH * 0.30f),
                size = Size(plateW * 0.76f, plateH * 0.60f),
                style = Stroke(width = width * 0.006f)
            )

            repeat(9) { index ->
                val jitter = abs(sin((seed + index * 13) * 0.7)).toFloat()
                val angle = (index / 9f) * 6.28318f + jitter * 0.5f
                val radius = plateW * (0.18f + jitter * 0.15f)
                val x = plateCenter.x + cos(angle) * radius
                val y = plateCenter.y + sin(angle) * radius * 0.45f
                val foodRadius = width * (0.035f + jitter * 0.022f)
                drawCircle(
                    color = art.food[index % art.food.size].copy(alpha = 0.95f),
                    radius = foodRadius,
                    center = Offset(x, y)
                )
                if (index % 3 == 0) {
                    drawCircle(
                        color = Color.White.copy(alpha = 0.20f),
                        radius = foodRadius * 0.36f,
                        center = Offset(x + foodRadius * 0.25f, y - foodRadius * 0.25f)
                    )
                }
                if (jitter > 0.55f) {
                    drawCircle(
                        color = art.garnish.copy(alpha = 0.85f),
                        radius = foodRadius * 0.28f,
                        center = Offset(x - foodRadius * 0.9f, y - foodRadius * 0.35f)
                    )
                }
            }

            val leafCenter = Offset(width * 0.71f, height * 0.45f)
            rotate(degrees = 18f + (seed % 18), pivot = leafCenter) {
                val leaf = Path().apply {
                    moveTo(leafCenter.x, leafCenter.y - height * 0.06f)
                    cubicTo(
                        leafCenter.x + width * 0.07f, leafCenter.y - height * 0.04f,
                        leafCenter.x + width * 0.05f, leafCenter.y + height * 0.03f,
                        leafCenter.x, leafCenter.y + height * 0.06f
                    )
                    cubicTo(
                        leafCenter.x - width * 0.05f, leafCenter.y + height * 0.03f,
                        leafCenter.x - width * 0.07f, leafCenter.y - height * 0.04f,
                        leafCenter.x, leafCenter.y - height * 0.06f
                    )
                    close()
                }
                drawPath(leaf, color = art.garnish.copy(alpha = 0.9f))
                drawLine(
                    color = Color.White.copy(alpha = 0.35f),
                    start = Offset(leafCenter.x, leafCenter.y - height * 0.045f),
                    end = Offset(leafCenter.x, leafCenter.y + height * 0.045f),
                    strokeWidth = width * 0.004f,
                    cap = StrokeCap.Round
                )
            }

            repeat(3) { index ->
                val x = width * (0.36f + index * 0.12f)
                val top = height * 0.10f + (seed % 10) * height * 0.004f
                val steam = Path().apply {
                    moveTo(x, top + height * 0.12f)
                    cubicTo(
                        x - width * 0.03f, top + height * 0.07f,
                        x + width * 0.03f, top + height * 0.04f,
                        x, top
                    )
                }
                drawPath(
                    path = steam,
                    color = art.steam,
                    style = Stroke(width = width * 0.008f, cap = StrokeCap.Round)
                )
            }

            repeat(14) { index ->
                val x = width * (0.10f + ((seed + index * 37) % 80) / 100f)
                val y = height * (0.18f + ((seed + index * 53) % 72) / 100f)
                drawCircle(
                    color = Color.White.copy(alpha = 0.10f),
                    radius = width * (0.002f + (index % 3) * 0.002f),
                    center = Offset(x, y)
                )
            }
        }
    }
}
