package openpay.app.ui

import openpay.app.ui.theme.FaintInk
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import openpay.app.data.PayState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** The OpenPay mark: a ring with a gap and a dot in the gap. */
@Composable
fun MarkIcon(size: Dp, color: Color = MaterialTheme.colorScheme.primary) {
    Canvas(modifier = Modifier.size(size)) {
        val stroke = this.size.minDimension * 0.16f
        val inset = stroke
        val ring = Offset(this.size.width / 2f, this.size.height / 2f)
        drawArc(
            color = color,
            startAngle = -70f,
            sweepAngle = 320f,
            useCenter = false,
            topLeft = Offset(inset, inset),
            size = androidx.compose.ui.geometry.Size(
                this.size.width - inset * 2,
                this.size.height - inset * 2
            ),
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )
        val dotRadius = this.size.minDimension * 0.11f
        val angle = Math.toRadians(-45.0)
        drawCircle(
            color = color,
            radius = dotRadius,
            center = Offset(
                (ring.x + Math.cos(angle) * (this.size.minDimension / 2f - inset)).toFloat(),
                (ring.y + Math.sin(angle) * (this.size.minDimension / 2f - inset)).toFloat()
            )
        )
    }
}

@Composable
fun Eyebrow(text: String) {
    Text(
        text = text.uppercase(Locale.ROOT),
        style = MaterialTheme.typography.labelSmall,
        fontFamily = FontFamily.Monospace,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
fun StateChip(state: PayState) {
    val (bg, fg, label) = when (state) {
        PayState.PENDING -> Triple(Color(0xFFF6ECD9), Color(0xFFB45309), "pending")
        PayState.SUCCESS -> Triple(Color(0xFFE4F2E8), Color(0xFF0A5C34), "confirmed")
        PayState.FAILED -> Triple(Color(0xFFF3DEDC), Color(0xFF8C2B22), "failed")
    }
    Box(
        modifier = Modifier
            .background(bg, CircleShape)
            .size(width = 84.dp, height = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = fg,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp
        )
    }
}

private val dayFormat = SimpleDateFormat("d MMM yyyy", Locale.ENGLISH)
private val fullFormat = SimpleDateFormat("d MMM yyyy, h:mm a", Locale.ENGLISH)

fun dayLabel(epoch: Long): String = dayFormat.format(Date(epoch))

fun fullLabel(epoch: Long): String = fullFormat.format(Date(epoch))
