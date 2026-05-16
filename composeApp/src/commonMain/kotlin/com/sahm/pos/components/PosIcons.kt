package com.sahm.pos.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

object PosIcons {
    val ArrowBack: ImageVector = outlinedIcon("ArrowBack") {
        moveTo(15f, 18f)
        lineTo(9f, 12f)
        lineTo(15f, 6f)
    }

    val ChevronRight: ImageVector = outlinedIcon("ChevronRight") {
        moveTo(9f, 18f)
        lineTo(15f, 12f)
        lineTo(9f, 6f)
    }

    val Refresh: ImageVector = outlinedIcon("Refresh") {
        moveTo(20f, 11f)
        curveTo(19.5f, 7.1f, 16.2f, 4f, 12.1f, 4f)
        curveTo(9.7f, 4f, 7.6f, 5f, 6.1f, 6.6f)
        lineTo(4f, 9f)
        moveTo(4f, 4.5f)
        lineTo(4f, 9f)
        lineTo(8.5f, 9f)
        moveTo(4f, 13f)
        curveTo(4.5f, 16.9f, 7.8f, 20f, 11.9f, 20f)
        curveTo(14.3f, 20f, 16.4f, 19f, 17.9f, 17.4f)
        lineTo(20f, 15f)
        moveTo(20f, 19.5f)
        lineTo(20f, 15f)
        lineTo(15.5f, 15f)
    }

    val Users: ImageVector = outlinedIcon("Users") {
        moveTo(16f, 20f)
        curveTo(16f, 17.8f, 14.2f, 16f, 12f, 16f)
        curveTo(9.8f, 16f, 8f, 17.8f, 8f, 20f)
        moveTo(12f, 13f)
        curveTo(10.3f, 13f, 9f, 11.7f, 9f, 10f)
        curveTo(9f, 8.3f, 10.3f, 7f, 12f, 7f)
        curveTo(13.7f, 7f, 15f, 8.3f, 15f, 10f)
        curveTo(15f, 11.7f, 13.7f, 13f, 12f, 13f)
        moveTo(19f, 18f)
        curveTo(18.8f, 16.6f, 17.9f, 15.5f, 16.7f, 14.9f)
        moveTo(16.2f, 6.3f)
        curveTo(17.6f, 6.8f, 18.4f, 8.3f, 18f, 9.8f)
        moveTo(5f, 18f)
        curveTo(5.2f, 16.6f, 6.1f, 15.5f, 7.3f, 14.9f)
        moveTo(7.8f, 6.3f)
        curveTo(6.4f, 6.8f, 5.6f, 8.3f, 6f, 9.8f)
    }

    val Bag: ImageVector = outlinedIcon("Bag") {
        moveTo(7f, 8f)
        lineTo(7f, 20f)
        lineTo(17f, 20f)
        lineTo(17f, 8f)
        close()
        moveTo(9f, 8f)
        curveTo(9f, 5.8f, 10.3f, 4f, 12f, 4f)
        curveTo(13.7f, 4f, 15f, 5.8f, 15f, 8f)
        moveTo(9.5f, 12f)
        lineTo(14.5f, 12f)
    }

    val Discount: ImageVector = outlinedIcon("Discount") {
        moveTo(20f, 10.5f)
        lineTo(13.5f, 4f)
        lineTo(6f, 4f)
        curveTo(4.9f, 4f, 4f, 4.9f, 4f, 6f)
        lineTo(4f, 13.5f)
        lineTo(10.5f, 20f)
        curveTo(11.3f, 20.8f, 12.6f, 20.8f, 13.4f, 20f)
        lineTo(20f, 13.4f)
        curveTo(20.8f, 12.6f, 20.8f, 11.3f, 20f, 10.5f)
        close()
        moveTo(8.5f, 8.5f)
        lineTo(8.51f, 8.5f)
        moveTo(9f, 15f)
        lineTo(15f, 9f)
        moveTo(14.8f, 15.2f)
        lineTo(14.81f, 15.2f)
    }

    val CheckCircle: ImageVector = outlinedIcon("CheckCircle") {
        moveTo(20f, 12f)
        curveTo(20f, 16.4f, 16.4f, 20f, 12f, 20f)
        curveTo(7.6f, 20f, 4f, 16.4f, 4f, 12f)
        curveTo(4f, 7.6f, 7.6f, 4f, 12f, 4f)
        curveTo(16.4f, 4f, 20f, 7.6f, 20f, 12f)
        moveTo(8.8f, 12.2f)
        lineTo(11f, 14.4f)
        lineTo(15.4f, 9.8f)
    }

    val Plus: ImageVector = outlinedIcon("Plus") {
        moveTo(12f, 5f)
        lineTo(12f, 19f)
        moveTo(5f, 12f)
        lineTo(19f, 12f)
    }

    val Minus: ImageVector = outlinedIcon("Minus") {
        moveTo(5f, 12f)
        lineTo(19f, 12f)
    }

    val Close: ImageVector = outlinedIcon("Close") {
        moveTo(6f, 6f)
        lineTo(18f, 18f)
        moveTo(18f, 6f)
        lineTo(6f, 18f)
    }

    val Search: ImageVector = outlinedIcon("Search") {
        moveTo(10.5f, 18f)
        curveTo(6.4f, 18f, 3f, 14.6f, 3f, 10.5f)
        curveTo(3f, 6.4f, 6.4f, 3f, 10.5f, 3f)
        curveTo(14.6f, 3f, 18f, 6.4f, 18f, 10.5f)
        curveTo(18f, 14.6f, 14.6f, 18f, 10.5f, 18f)
        close()
        moveTo(16f, 16f)
        lineTo(21f, 21f)
    }
}

private inline fun outlinedIcon(
    name: String,
    crossinline block: androidx.compose.ui.graphics.vector.PathBuilder.() -> Unit,
): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(
            fill = null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.8f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        ) {
            block()
        }
    }.build()
