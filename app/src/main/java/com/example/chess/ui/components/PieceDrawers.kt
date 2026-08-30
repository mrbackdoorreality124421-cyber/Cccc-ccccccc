package com.example.chess.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

/**
 * High-precision Staunton Canvas vector drawing algorithms for classic chess pieces.
 * Uses mathematical Bezier paths, multi-stop radial gradients for realistic 3D lighting,
 * and high-contrast contour strokes.
 */
object PieceDrawers {

    fun drawKing(drawScope: DrawScope, center: Offset, size: Float, fillColor: Color, outlineColor: Color) {
        with(drawScope) {
            val strokeWidth = size * 0.035f

            // 1. Base pedestal
            val basePath = Path().apply {
                moveTo(center.x - size * 0.32f, center.y + size * 0.38f)
                lineTo(center.x + size * 0.32f, center.y + size * 0.38f)
                lineTo(center.x + size * 0.26f, center.y + size * 0.22f)
                lineTo(center.x - size * 0.26f, center.y + size * 0.22f)
                close()
            }

            // 2. Tapered Body & Collar
            val bodyPath = Path().apply {
                moveTo(center.x - size * 0.24f, center.y + size * 0.22f)
                lineTo(center.x + size * 0.24f, center.y + size * 0.22f)
                quadraticBezierTo(
                    center.x + size * 0.16f, center.y,
                    center.x + size * 0.20f, center.y - size * 0.12f
                )
                lineTo(center.x - size * 0.20f, center.y - size * 0.12f)
                quadraticBezierTo(
                    center.x - size * 0.16f, center.y,
                    center.x - size * 0.24f, center.y + size * 0.22f
                )
                close()
            }

            // 3. Royal Crown
            val crownPath = Path().apply {
                moveTo(center.x - size * 0.20f, center.y - size * 0.12f)
                quadraticBezierTo(
                    center.x - size * 0.26f, center.y - size * 0.28f,
                    center.x, center.y - size * 0.32f
                )
                quadraticBezierTo(
                    center.x + size * 0.26f, center.y - size * 0.28f,
                    center.x + size * 0.20f, center.y - size * 0.12f
                )
                close()
            }

            // 4. Finial Cross
            val crossPath = Path().apply {
                // Vertical
                moveTo(center.x, center.y - size * 0.32f)
                lineTo(center.x, center.y - size * 0.44f)
                // Horizontal
                moveTo(center.x - size * 0.08f, center.y - size * 0.38f)
                lineTo(center.x + size * 0.08f, center.y - size * 0.38f)
            }

            val gradient = Brush.radialGradient(
                colors = listOf(
                    fillColor,
                    fillColor.copy(alpha = 0.88f),
                    fillColor.copy(alpha = 0.72f)
                ),
                center = center.copy(x = center.x - size * 0.06f, y = center.y - size * 0.12f),
                radius = size * 0.55f
            )

            // Fill shapes
            drawPath(basePath, brush = gradient)
            drawPath(bodyPath, brush = gradient)
            drawPath(crownPath, brush = gradient)

            // Draw crisp contours
            drawPath(basePath, color = outlineColor, style = Stroke(width = strokeWidth))
            drawPath(bodyPath, color = outlineColor, style = Stroke(width = strokeWidth))
            drawPath(crownPath, color = outlineColor, style = Stroke(width = strokeWidth))
            drawPath(crossPath, color = outlineColor, style = Stroke(width = strokeWidth * 1.3f, cap = StrokeCap.Round))

            // Crown accent jewels / orb
            drawCircle(
                color = outlineColor,
                radius = size * 0.035f,
                center = center.copy(y = center.y - size * 0.32f)
            )
        }
    }

    fun drawQueen(drawScope: DrawScope, center: Offset, size: Float, fillColor: Color, outlineColor: Color) {
        with(drawScope) {
            val strokeWidth = size * 0.035f

            // Base
            val basePath = Path().apply {
                moveTo(center.x - size * 0.32f, center.y + size * 0.38f)
                lineTo(center.x + size * 0.32f, center.y + size * 0.38f)
                lineTo(center.x + size * 0.25f, center.y + size * 0.22f)
                lineTo(center.x - size * 0.25f, center.y + size * 0.22f)
                close()
            }

            // Body
            val bodyPath = Path().apply {
                moveTo(center.x - size * 0.23f, center.y + size * 0.22f)
                lineTo(center.x + size * 0.23f, center.y + size * 0.22f)
                quadraticBezierTo(
                    center.x + size * 0.14f, center.y,
                    center.x + size * 0.18f, center.y - size * 0.10f
                )
                lineTo(center.x - size * 0.18f, center.y - size * 0.10f)
                quadraticBezierTo(
                    center.x - size * 0.14f, center.y,
                    center.x - size * 0.23f, center.y + size * 0.22f
                )
                close()
            }

            // Flared Coronet (5 points)
            val coronetPath = Path().apply {
                moveTo(center.x - size * 0.18f, center.y - size * 0.10f)
                lineTo(center.x - size * 0.28f, center.y - size * 0.30f)
                lineTo(center.x - size * 0.14f, center.y - size * 0.20f)
                lineTo(center.x, center.y - size * 0.34f)
                lineTo(center.x + size * 0.14f, center.y - size * 0.20f)
                lineTo(center.x + size * 0.28f, center.y - size * 0.30f)
                lineTo(center.x + size * 0.18f, center.y - size * 0.10f)
                close()
            }

            val gradient = Brush.radialGradient(
                colors = listOf(
                    fillColor,
                    fillColor.copy(alpha = 0.88f),
                    fillColor.copy(alpha = 0.72f)
                ),
                center = center.copy(x = center.x - size * 0.06f, y = center.y - size * 0.10f),
                radius = size * 0.55f
            )

            drawPath(basePath, brush = gradient)
            drawPath(bodyPath, brush = gradient)
            drawPath(coronetPath, brush = gradient)

            drawPath(basePath, color = outlineColor, style = Stroke(width = strokeWidth))
            drawPath(bodyPath, color = outlineColor, style = Stroke(width = strokeWidth))
            drawPath(coronetPath, color = outlineColor, style = Stroke(width = strokeWidth))

            // Coronet pearl orbs
            val orbRadius = size * 0.032f
            drawCircle(color = outlineColor, radius = orbRadius, center = Offset(center.x - size * 0.28f, center.y - size * 0.30f))
            drawCircle(color = outlineColor, radius = orbRadius, center = Offset(center.x, center.y - size * 0.34f))
            drawCircle(color = outlineColor, radius = orbRadius, center = Offset(center.x + size * 0.28f, center.y - size * 0.30f))
        }
    }

    fun drawRook(drawScope: DrawScope, center: Offset, size: Float, fillColor: Color, outlineColor: Color) {
        with(drawScope) {
            val strokeWidth = size * 0.035f

            // Base
            val basePath = Path().apply {
                moveTo(center.x - size * 0.30f, center.y + size * 0.38f)
                lineTo(center.x + size * 0.30f, center.y + size * 0.38f)
                lineTo(center.x + size * 0.24f, center.y + size * 0.24f)
                lineTo(center.x - size * 0.24f, center.y + size * 0.24f)
                close()
            }

            // Tower Shaft
            val shaftPath = Path().apply {
                moveTo(center.x - size * 0.20f, center.y + size * 0.24f)
                lineTo(center.x + size * 0.20f, center.y + size * 0.24f)
                lineTo(center.x + size * 0.17f, center.y - size * 0.15f)
                lineTo(center.x - size * 0.17f, center.y - size * 0.15f)
                close()
            }

            // Battlements / Crenellations
            val battlementPath = Path().apply {
                moveTo(center.x - size * 0.26f, center.y - size * 0.15f)
                lineTo(center.x - size * 0.26f, center.y - size * 0.34f)
                lineTo(center.x - size * 0.14f, center.y - size * 0.34f)
                lineTo(center.x - size * 0.14f, center.y - size * 0.24f)
                lineTo(center.x - size * 0.05f, center.y - size * 0.24f)
                lineTo(center.x - size * 0.05f, center.y - size * 0.34f)
                lineTo(center.x + size * 0.05f, center.y - size * 0.34f)
                lineTo(center.x + size * 0.05f, center.y - size * 0.24f)
                lineTo(center.x + size * 0.14f, center.y - size * 0.24f)
                lineTo(center.x + size * 0.14f, center.y - size * 0.34f)
                lineTo(center.x + size * 0.26f, center.y - size * 0.34f)
                lineTo(center.x + size * 0.26f, center.y - size * 0.15f)
                close()
            }

            val gradient = Brush.radialGradient(
                colors = listOf(
                    fillColor,
                    fillColor.copy(alpha = 0.88f),
                    fillColor.copy(alpha = 0.72f)
                ),
                center = center.copy(x = center.x - size * 0.06f, y = center.y - size * 0.08f),
                radius = size * 0.55f
            )

            drawPath(basePath, brush = gradient)
            drawPath(shaftPath, brush = gradient)
            drawPath(battlementPath, brush = gradient)

            drawPath(basePath, color = outlineColor, style = Stroke(width = strokeWidth))
            drawPath(shaftPath, color = outlineColor, style = Stroke(width = strokeWidth))
            drawPath(battlementPath, color = outlineColor, style = Stroke(width = strokeWidth))
        }
    }

    fun drawBishop(drawScope: DrawScope, center: Offset, size: Float, fillColor: Color, outlineColor: Color) {
        with(drawScope) {
            val strokeWidth = size * 0.035f

            // Base
            val basePath = Path().apply {
                moveTo(center.x - size * 0.28f, center.y + size * 0.38f)
                lineTo(center.x + size * 0.28f, center.y + size * 0.38f)
                lineTo(center.x + size * 0.22f, center.y + size * 0.24f)
                lineTo(center.x - size * 0.22f, center.y + size * 0.24f)
                close()
            }

            // Body
            val bodyPath = Path().apply {
                moveTo(center.x - size * 0.18f, center.y + size * 0.24f)
                lineTo(center.x + size * 0.18f, center.y + size * 0.24f)
                quadraticBezierTo(
                    center.x + size * 0.12f, center.y,
                    center.x + size * 0.16f, center.y - size * 0.08f
                )
                lineTo(center.x - size * 0.16f, center.y - size * 0.08f)
                quadraticBezierTo(
                    center.x - size * 0.12f, center.y,
                    center.x - size * 0.18f, center.y + size * 0.24f
                )
                close()
            }

            // Mitre Head
            val mitrePath = Path().apply {
                moveTo(center.x - size * 0.16f, center.y - size * 0.08f)
                quadraticBezierTo(
                    center.x - size * 0.22f, center.y - size * 0.22f,
                    center.x, center.y - size * 0.36f
                )
                quadraticBezierTo(
                    center.x + size * 0.22f, center.y - size * 0.22f,
                    center.x + size * 0.16f, center.y - size * 0.08f
                )
                close()
            }

            val gradient = Brush.radialGradient(
                colors = listOf(
                    fillColor,
                    fillColor.copy(alpha = 0.88f),
                    fillColor.copy(alpha = 0.72f)
                ),
                center = center.copy(x = center.x - size * 0.05f, y = center.y - size * 0.10f),
                radius = size * 0.55f
            )

            drawPath(basePath, brush = gradient)
            drawPath(bodyPath, brush = gradient)
            drawPath(mitrePath, brush = gradient)

            drawPath(basePath, color = outlineColor, style = Stroke(width = strokeWidth))
            drawPath(bodyPath, color = outlineColor, style = Stroke(width = strokeWidth))
            drawPath(mitrePath, color = outlineColor, style = Stroke(width = strokeWidth))

            // Mitre Cut / Slit
            drawLine(
                color = outlineColor,
                start = Offset(center.x + size * 0.02f, center.y - size * 0.26f),
                end = Offset(center.x + size * 0.14f, center.y - size * 0.18f),
                strokeWidth = strokeWidth * 1.1f,
                cap = StrokeCap.Round
            )

            // Top Cross Orb
            drawCircle(
                color = outlineColor,
                radius = size * 0.038f,
                center = center.copy(y = center.y - size * 0.38f)
            )
        }
    }

    fun drawKnight(drawScope: DrawScope, center: Offset, size: Float, fillColor: Color, outlineColor: Color) {
        with(drawScope) {
            val strokeWidth = size * 0.035f

            // Base
            val basePath = Path().apply {
                moveTo(center.x - size * 0.30f, center.y + size * 0.38f)
                lineTo(center.x + size * 0.30f, center.y + size * 0.38f)
                lineTo(center.x + size * 0.24f, center.y + size * 0.25f)
                lineTo(center.x - size * 0.24f, center.y + size * 0.25f)
                close()
            }

            // Horse Head & Arched Mane
            val horsePath = Path().apply {
                moveTo(center.x - size * 0.20f, center.y + size * 0.25f)
                // Breast curve
                quadraticBezierTo(
                    center.x - size * 0.28f, center.y + size * 0.05f,
                    center.x - size * 0.26f, center.y - size * 0.10f
                )
                // Muzzle
                lineTo(center.x - size * 0.30f, center.y - size * 0.18f)
                lineTo(center.x - size * 0.16f, center.y - size * 0.26f)
                // Ear & Crest
                lineTo(center.x - size * 0.08f, center.y - size * 0.38f)
                lineTo(center.x - size * 0.02f, center.y - size * 0.30f)
                // Mane curve back to base
                quadraticBezierTo(
                    center.x + size * 0.26f, center.y - size * 0.15f,
                    center.x + size * 0.22f, center.y + size * 0.25f
                )
                close()
            }

            val gradient = Brush.radialGradient(
                colors = listOf(
                    fillColor,
                    fillColor.copy(alpha = 0.88f),
                    fillColor.copy(alpha = 0.72f)
                ),
                center = center.copy(x = center.x - size * 0.08f, y = center.y - size * 0.05f),
                radius = size * 0.55f
            )

            drawPath(basePath, brush = gradient)
            drawPath(horsePath, brush = gradient)

            drawPath(basePath, color = outlineColor, style = Stroke(width = strokeWidth))
            drawPath(horsePath, color = outlineColor, style = Stroke(width = strokeWidth))

            // Knight Eye
            drawCircle(
                color = outlineColor,
                radius = size * 0.032f,
                center = Offset(center.x - size * 0.14f, center.y - size * 0.18f)
            )

            // Mane detail grooves
            drawLine(
                color = outlineColor,
                start = Offset(center.x + size * 0.04f, center.y - size * 0.20f),
                end = Offset(center.x + size * 0.16f, center.y - size * 0.10f),
                strokeWidth = strokeWidth * 0.8f,
                cap = StrokeCap.Round
            )
            drawLine(
                color = outlineColor,
                start = Offset(center.x + size * 0.08f, center.y - size * 0.06f),
                end = Offset(center.x + size * 0.18f, center.y + size * 0.04f),
                strokeWidth = strokeWidth * 0.8f,
                cap = StrokeCap.Round
            )
        }
    }

    fun drawPawn(drawScope: DrawScope, center: Offset, size: Float, fillColor: Color, outlineColor: Color) {
        with(drawScope) {
            val strokeWidth = size * 0.035f

            // Base
            val basePath = Path().apply {
                moveTo(center.x - size * 0.24f, center.y + size * 0.38f)
                lineTo(center.x + size * 0.24f, center.y + size * 0.38f)
                lineTo(center.x + size * 0.18f, center.y + size * 0.24f)
                lineTo(center.x - size * 0.18f, center.y + size * 0.24f)
                close()
            }

            // Body
            val bodyPath = Path().apply {
                moveTo(center.x - size * 0.15f, center.y + size * 0.24f)
                lineTo(center.x + size * 0.15f, center.y + size * 0.24f)
                quadraticBezierTo(
                    center.x + size * 0.08f, center.y + size * 0.02f,
                    center.x + size * 0.12f, center.y - size * 0.06f
                )
                lineTo(center.x - size * 0.12f, center.y - size * 0.06f)
                quadraticBezierTo(
                    center.x - size * 0.08f, center.y + size * 0.02f,
                    center.x - size * 0.15f, center.y + size * 0.24f
                )
                close()
            }

            val gradient = Brush.radialGradient(
                colors = listOf(
                    fillColor,
                    fillColor.copy(alpha = 0.88f),
                    fillColor.copy(alpha = 0.72f)
                ),
                center = center.copy(x = center.x - size * 0.04f, y = center.y - size * 0.10f),
                radius = size * 0.50f
            )

            drawPath(basePath, brush = gradient)
            drawPath(bodyPath, brush = gradient)

            // Head Sphere
            drawCircle(
                brush = gradient,
                radius = size * 0.18f,
                center = center.copy(y = center.y - size * 0.18f)
            )

            drawPath(basePath, color = outlineColor, style = Stroke(width = strokeWidth))
            drawPath(bodyPath, color = outlineColor, style = Stroke(width = strokeWidth))
            drawCircle(
                color = outlineColor,
                radius = size * 0.18f,
                center = center.copy(y = center.y - size * 0.18f),
                style = Stroke(width = strokeWidth)
            )
        }
    }
}
