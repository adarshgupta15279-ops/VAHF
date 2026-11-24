package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R

/**
 * Branded dark aurora + glowing lotus meditation backdrop matching the VAHF design screenshot.
 * Uses pure Compose Canvas geometry (no AI-generated images or fake device status bars).
 */
@Composable
fun VahfBrandedDarkBackdrop(
    modifier: Modifier = Modifier,
    showBottomLotus: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF020B14),
                        Color(0xFF041C2E),
                        Color(0xFF031422),
                        Color(0xFF010912)
                    )
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Soft diagonal cyan aurora glow beams
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color(0xFF00D4E8).copy(alpha = 0.09f),
                        Color(0xFF1CB5FF).copy(alpha = 0.07f),
                        Color.Transparent
                    ),
                    start = Offset(0f, h * 0.15f),
                    end = Offset(w, h * 0.55f)
                )
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF00E5FF).copy(alpha = 0.14f),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.85f, h * 0.36f),
                    radius = w * 0.55f
                ),
                radius = w * 0.55f,
                center = Offset(w * 0.85f, h * 0.36f)
            )

            // Subtle upper-left botanical leaves
            drawLeafCluster(
                origin = Offset(-w * 0.02f, h * 0.14f),
                scale = w * 0.09f,
                flipX = false
            )

            if (showBottomLotus) {
                val lotusCenter = Offset(w * 0.5f, h * 0.935f)
                val glowRadius = w * 0.46f

                // Cyan lotus aura glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF00F0FF).copy(alpha = 0.36f),
                            Color(0xFF00B4D8).copy(alpha = 0.14f),
                            Color.Transparent
                        ),
                        center = Offset(lotusCenter.x, h * 0.90f),
                        radius = glowRadius
                    ),
                    radius = glowRadius,
                    center = Offset(lotusCenter.x, h * 0.90f)
                )

                // Layered translucent lotus petals behind the meditating silhouette
                val petalAngles = listOf(-52f, -30f, -12f, 0f, 12f, 30f, 52f)
                petalAngles.forEachIndexed { idx, angle ->
                    val petalHeight = if (idx == 3) w * 0.21f else w * 0.175f
                    val petalWidth = w * 0.085f
                    rotate(degrees = angle, pivot = lotusCenter) {
                        val petalPath = Path().apply {
                            moveTo(lotusCenter.x, lotusCenter.y)
                            cubicTo(
                                lotusCenter.x - petalWidth,
                                lotusCenter.y - petalHeight * 0.45f,
                                lotusCenter.x - petalWidth * 0.55f,
                                lotusCenter.y - petalHeight * 0.85f,
                                lotusCenter.x,
                                lotusCenter.y - petalHeight
                            )
                            cubicTo(
                                lotusCenter.x + petalWidth * 0.55f,
                                lotusCenter.y - petalHeight * 0.85f,
                                lotusCenter.x + petalWidth,
                                lotusCenter.y - petalHeight * 0.45f,
                                lotusCenter.x,
                                lotusCenter.y
                            )
                            close()
                        }
                        drawPath(
                            path = petalPath,
                            color = Color(0xFF2BE7FF).copy(alpha = if (idx == 3) 0.22f else 0.14f)
                        )
                    }
                }

                // Distant soft dark mountains
                val mountainPath = Path().apply {
                    moveTo(0f, h * 0.92f)
                    lineTo(w * 0.18f, h * 0.86f)
                    lineTo(w * 0.35f, h * 0.90f)
                    lineTo(w * 0.65f, h * 0.89f)
                    lineTo(w * 0.84f, h * 0.85f)
                    lineTo(w, h * 0.91f)
                    lineTo(w, h)
                    lineTo(0f, h)
                    close()
                }
                drawPath(path = mountainPath, color = Color(0xFF02111D).copy(alpha = 0.72f))

                // Curved ground hill at bottom
                val hillPath = Path().apply {
                    moveTo(0f, h * 0.95f)
                    quadraticTo(w * 0.5f, h * 0.89f, w, h * 0.95f)
                    lineTo(w, h)
                    lineTo(0f, h)
                    close()
                }
                drawPath(path = hillPath, color = Color(0xFF010810))

                // Meditating silhouette centered in front of the glowing lotus
                drawMeditatingSilhouette(
                    centerBottom = Offset(w * 0.5f, h * 0.945f),
                    scale = w * 0.19f
                )

                // Bottom corner botanical leaves
                drawLeafCluster(
                    origin = Offset(0f, h * 0.96f),
                    scale = w * 0.10f,
                    flipX = false
                )
                drawLeafCluster(
                    origin = Offset(w, h * 0.96f),
                    scale = w * 0.10f,
                    flipX = true
                )
            }
        }

        content()
    }
}

private fun DrawScope.drawLeafCluster(
    origin: Offset,
    scale: Float,
    flipX: Boolean
) {
    val dir = if (flipX) -1f else 1f
    val angles = listOf(-35f, -10f, 18f, 42f)
    angles.forEachIndexed { index, baseAngle ->
        val angle = if (flipX) -baseAngle else baseAngle
        val length = scale * (0.85f + (index % 2) * 0.25f)
        rotate(degrees = angle, pivot = origin) {
            val leaf = Path().apply {
                moveTo(origin.x, origin.y)
                cubicTo(
                    origin.x + dir * length * 0.35f,
                    origin.y - length * 0.38f,
                    origin.x + dir * length * 0.82f,
                    origin.y - length * 0.30f,
                    origin.x + dir * length,
                    origin.y - length * 0.05f
                )
                cubicTo(
                    origin.x + dir * length * 0.72f,
                    origin.y + length * 0.18f,
                    origin.x + dir * length * 0.30f,
                    origin.y + length * 0.12f,
                    origin.x,
                    origin.y
                )
                close()
            }
            drawPath(leaf, color = Color(0xFF041520).copy(alpha = 0.85f))
        }
    }
}

private fun DrawScope.drawMeditatingSilhouette(
    centerBottom: Offset,
    scale: Float
) {
    val cx = centerBottom.x
    val by = centerBottom.y
    val silhouetteColor = Color(0xFF020810)

    // Head
    drawCircle(
        color = silhouetteColor,
        radius = scale * 0.12f,
        center = Offset(cx, by - scale * 0.66f)
    )

    // Torso & crossed legs
    val bodyPath = Path().apply {
        moveTo(cx - scale * 0.06f, by - scale * 0.56f)
        lineTo(cx + scale * 0.06f, by - scale * 0.56f)
        // Shoulders
        cubicTo(
            cx + scale * 0.18f, by - scale * 0.52f,
            cx + scale * 0.22f, by - scale * 0.42f,
            cx + scale * 0.15f, by - scale * 0.28f
        )
        // Waist to right knee
        cubicTo(
            cx + scale * 0.22f, by - scale * 0.16f,
            cx + scale * 0.48f, by - scale * 0.10f,
            cx + scale * 0.44f, by
        )
        lineTo(cx - scale * 0.44f, by)
        // Left knee to waist
        cubicTo(
            cx - scale * 0.48f, by - scale * 0.10f,
            cx - scale * 0.22f, by - scale * 0.16f,
            cx - scale * 0.15f, by - scale * 0.28f
        )
        cubicTo(
            cx - scale * 0.22f, by - scale * 0.42f,
            cx - scale * 0.18f, by - scale * 0.52f,
            cx - scale * 0.06f, by - scale * 0.56f
        )
        close()
    }
    drawPath(bodyPath, color = silhouetteColor)

    // Meditating arms resting on knees
    val leftArm = Path().apply {
        moveTo(cx - scale * 0.17f, by - scale * 0.46f)
        quadraticTo(cx - scale * 0.34f, by - scale * 0.28f, cx - scale * 0.38f, by - scale * 0.09f)
    }
    val rightArm = Path().apply {
        moveTo(cx + scale * 0.17f, by - scale * 0.46f)
        quadraticTo(cx + scale * 0.34f, by - scale * 0.28f, cx + scale * 0.38f, by - scale * 0.09f)
    }
    drawPath(leftArm, color = silhouetteColor, style = Stroke(width = scale * 0.075f, cap = StrokeCap.Round))
    drawPath(rightArm, color = silhouetteColor, style = Stroke(width = scale * 0.075f, cap = StrokeCap.Round))
}

/**
 * Displays the original VAHF logo with transparent outer background seamlessly over any dark backdrop.
 */
@Composable
fun VahfOriginalLogoBadge(
    modifier: Modifier = Modifier,
    size: Dp = 196.dp
) {
    Image(
        painter = painterResource(id = R.drawable.vahf_logo_transparent),
        contentDescription = "VAHF Official Logo - Prevention, Care and Awareness",
        contentScale = ContentScale.Fit,
        modifier = modifier.size(size)
    )
}

/**
 * Clean multicolor Google 'G' mark for the 'Continue with Google' button.
 */
@Composable
fun GoogleGIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(20.dp)) {
        val strokeWidth = size.minDimension * 0.20f
        val inset = strokeWidth / 2f
        val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
        val topLeft = Offset(inset, inset)

        // Red top-left arc
        drawArc(
            color = Color(0xFFEA4335),
            startAngle = -140f,
            sweepAngle = 95f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
        )
        // Yellow bottom-left arc
        drawArc(
            color = Color(0xFFFBBC05),
            startAngle = 140f,
            sweepAngle = 80f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
        )
        // Green bottom arc
        drawArc(
            color = Color(0xFF34A853),
            startAngle = 45f,
            sweepAngle = 95f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
        )
        // Blue right arc + horizontal bar
        drawArc(
            color = Color(0xFF4285F4),
            startAngle = -10f,
            sweepAngle = 55f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
        )
        drawLine(
            color = Color(0xFF4285F4),
            start = Offset(size.width * 0.52f, size.height * 0.50f),
            end = Offset(size.width * 0.94f, size.height * 0.50f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Butt
        )
    }
}
