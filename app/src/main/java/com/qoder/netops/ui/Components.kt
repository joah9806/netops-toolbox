package com.qoder.netops.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qoder.netops.core.TerminalLine
import com.qoder.netops.core.TerminalLineKind
import com.qoder.netops.ui.theme.MonoFamily
import com.qoder.netops.ui.theme.TerminalTextStyle
import com.qoder.netops.ui.theme.netOpsPalette

@Composable
fun LogoMark(size: Dp = 38.dp) {
    val palette = netOpsPalette()
    val shape = RoundedCornerShape(size * 0.28f)
    Box(
        modifier = Modifier
            .size(size)
            .clip(shape)
            .background(palette.logoBackground),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = ">_",
            color = palette.logoForeground,
            fontFamily = MonoFamily,
            fontWeight = FontWeight.Bold,
            fontSize = (size.value * 0.38f).sp,
            letterSpacing = (-1).sp,
            maxLines = 1
        )
    }
}

@Composable
fun AppTopBar() {
    val palette = netOpsPalette()
    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LogoMark()
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("网维工具箱", style = MaterialTheme.typography.titleLarge)
                Text(
                    text = "NET · OPS · TOOLKIT",
                    fontFamily = MonoFamily,
                    fontSize = 8.5.sp,
                    letterSpacing = 1.6.sp,
                    color = palette.terminalDim
                )
            }
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Text(
                    text = "v1.0",
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                    fontFamily = MonoFamily,
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ScreenHeader(eyebrow: String, title: String, description: String) {
    Column {
        Text(
            text = eyebrow,
            fontFamily = MonoFamily,
            fontSize = 9.sp,
            letterSpacing = 2.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(9.dp))
        Text(title, style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(5.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun CardSurface(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(content = content)
    }
}

@Composable
fun MiniChip(
    label: String,
    selected: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(50)
    Surface(
        modifier = Modifier
            .clip(shape)
            .clickable(enabled = enabled, onClick = onClick),
        shape = shape,
        color = if (selected) scheme.primary else scheme.surface,
        contentColor = if (selected) scheme.onPrimary else scheme.onSurfaceVariant,
        border = if (selected) null else BorderStroke(1.dp, scheme.outlineVariant)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 7.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
        )
    }
}

@Composable
fun ChipsRow(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

@Composable
fun MonoField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Done,
    trailing: (@Composable () -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        singleLine = singleLine,
        label = {
            Text(label, style = MaterialTheme.typography.labelMedium)
        },
        placeholder = {
            Text(
                text = placeholder,
                fontFamily = MonoFamily,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.62f),
                maxLines = 1
            )
        },
        textStyle = MaterialTheme.typography.bodyLarge.copy(
            fontFamily = MonoFamily,
            fontSize = 14.sp
        ),
        shape = RoundedCornerShape(12.dp),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        trailingIcon = trailing,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.onBackground,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            disabledBorderColor = MaterialTheme.colorScheme.outlineVariant,
            errorBorderColor = MaterialTheme.colorScheme.error,
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
            disabledTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
            cursorColor = MaterialTheme.colorScheme.onSurface,
            focusedLabelColor = MaterialTheme.colorScheme.onSurface,
            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    )
}

@Composable
fun PrimaryActionButton(
    label: String,
    running: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp),
        shape = RoundedCornerShape(14.dp),
        border = if (running) BorderStroke(1.dp, scheme.outline) else null,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (running) scheme.surface else scheme.primary,
            contentColor = if (running) scheme.onSurface else scheme.onPrimary,
            disabledContainerColor = scheme.surfaceVariant,
            disabledContentColor = scheme.onSurfaceVariant
        )
    ) {
        if (running) {
            Box(
                Modifier
                    .size(9.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(scheme.error)
            )
        } else {
            PlayGlyph(color = scheme.onPrimary)
        }
        Spacer(Modifier.width(9.dp))
        Text(if (running) "停止" else label, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun PlayGlyph(color: Color, modifier: Modifier = Modifier.size(9.dp)) {
    Canvas(modifier) {
        val path = Path().apply {
            moveTo(0f, 0f)
            lineTo(size.width, size.height / 2f)
            lineTo(0f, size.height)
            close()
        }
        drawPath(path, color)
    }
}

@Composable
fun SquareActionButton(
    running: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier
            .size(52.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = when {
            running -> scheme.surface
            enabled -> scheme.primary
            else -> scheme.surfaceVariant
        },
        border = if (running) BorderStroke(1.dp, scheme.outline) else null
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (running) {
                Box(
                    Modifier
                        .size(9.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(scheme.error)
                )
            } else {
                PlayGlyph(
                    color = if (enabled) scheme.onPrimary else scheme.onSurfaceVariant,
                    modifier = Modifier.size(11.dp)
                )
            }
        }
    }
}

@Composable
fun StatusPill(text: String, color: Color, container: Color) {
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(container)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            fontFamily = MonoFamily,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
            color = color
        )
    }
}

@Composable
fun TinyBadge(text: String, color: Color, container: Color) {
    Box(
        Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(container)
            .padding(horizontal = 5.dp, vertical = 1.dp)
    ) {
        Text(text, fontSize = 8.sp, fontWeight = FontWeight.SemiBold, color = color)
    }
}

@Composable
fun TerminalPanel(
    lines: List<TerminalLine>,
    running: Boolean,
    emptyHint: String,
    modifier: Modifier = Modifier,
    onCopy: (() -> Unit)? = null
) {
    val scheme = MaterialTheme.colorScheme
    val palette = netOpsPalette()
    val shape = RoundedCornerShape(18.dp)
    Surface(
        modifier = modifier,
        shape = shape,
        color = scheme.surface,
        border = BorderStroke(1.dp, scheme.outlineVariant)
    ) {
        Column(Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(
                        Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(scheme.outline)
                    )
                    Box(
                        Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(scheme.outline)
                    )
                    Box(
                        Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(scheme.outline)
                    )
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "TERMINAL",
                    fontFamily = MonoFamily,
                    fontSize = 8.5.sp,
                    letterSpacing = 1.6.sp,
                    color = palette.terminalDim
                )
                Spacer(Modifier.weight(1f))
                if (running) {
                    RunningBadge()
                }
                if (onCopy != null && lines.isNotEmpty()) {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "复制",
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable(onClick = onCopy)
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                        fontFamily = MonoFamily,
                        fontSize = 9.sp,
                        color = scheme.onSurfaceVariant
                    )
                }
            }
            HorizontalDivider(color = scheme.outlineVariant)
            Box(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(bottomStart = 17.dp, bottomEnd = 17.dp))
                    .background(palette.terminalBackground)
            ) {
                val scroll = rememberScrollState()
                LaunchedEffect(lines.size, running) {
                    if (lines.isNotEmpty()) scroll.animateScrollTo(scroll.maxValue)
                }
                if (lines.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = ">_",
                            fontFamily = MonoFamily,
                            fontSize = 20.sp,
                            color = palette.terminalDim.copy(alpha = 0.7f)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = emptyHint,
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.terminalDim,
                            lineHeight = 17.sp
                        )
                    }
                } else {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(scroll)
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        lines.forEach { line -> TerminalLineRow(line) }
                    }
                }
            }
        }
    }
}

@Composable
private fun RunningBadge() {
    val palette = netOpsPalette()
    val transition = rememberInfiniteTransition(label = "running")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 780),
            repeatMode = RepeatMode.Reverse
        ),
        label = "runningAlpha"
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(palette.success.copy(alpha = alpha))
        )
        Spacer(Modifier.width(5.dp))
        Text(
            text = "RUNNING",
            fontFamily = MonoFamily,
            fontSize = 8.sp,
            letterSpacing = 1.2.sp,
            color = palette.success
        )
    }
}

@Composable
private fun TerminalLineRow(line: TerminalLine) {
    val scheme = MaterialTheme.colorScheme
    val palette = netOpsPalette()
    val color = when (line.kind) {
        TerminalLineKind.COMMAND -> scheme.onSurface
        TerminalLineKind.OUTPUT -> scheme.onSurface.copy(alpha = 0.82f)
        TerminalLineKind.SUCCESS -> palette.success
        TerminalLineKind.ERROR -> scheme.error
        TerminalLineKind.INFO -> palette.terminalDim
    }
    Text(
        text = line.text,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp),
        style = TerminalTextStyle,
        color = color,
        fontWeight = if (line.kind == TerminalLineKind.COMMAND) FontWeight.SemiBold else FontWeight.Normal
    )
}

@Composable
fun ThinProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onBackground
) {
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 260),
        label = "progress"
    )
    Box(
        modifier
            .fillMaxWidth()
            .height(4.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(animated)
                .clip(CircleShape)
                .background(color)
        )
    }
}

@Composable
fun StatCell(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Column(modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = value,
            fontFamily = MonoFamily,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = valueColor,
            maxLines = 1
        )
    }
}

@Composable
fun EmptyPanel(
    title: String,
    hint: String,
    modifier: Modifier = Modifier
) {
    val palette = netOpsPalette()
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = ">_",
                fontFamily = MonoFamily,
                fontSize = 18.sp,
                color = palette.terminalDim
            )
            Spacer(Modifier.height(8.dp))
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                text = hint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun SmallAction(label: String, onClick: () -> Unit, enabled: Boolean = true) {
    Text(
        text = label,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        fontFamily = MonoFamily,
        fontSize = 10.sp,
        color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.outline
    )
}

enum class ToolGlyph { PING, PORT, LAN }

@Composable
fun ToolGlyphIcon(
    glyph: ToolGlyph,
    color: Color,
    modifier: Modifier = Modifier.size(22.dp)
) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val strokeWidth = 1.7.dp.toPx()
        val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
        when (glyph) {
            ToolGlyph.PING -> {
                val path = Path().apply {
                    moveTo(w * 0.05f, h * 0.55f)
                    lineTo(w * 0.28f, h * 0.55f)
                    lineTo(w * 0.38f, h * 0.22f)
                    lineTo(w * 0.55f, h * 0.82f)
                    lineTo(w * 0.66f, h * 0.40f)
                    lineTo(w * 0.74f, h * 0.55f)
                    lineTo(w * 0.95f, h * 0.55f)
                }
                drawPath(path, color, style = stroke)
            }

            ToolGlyph.PORT -> {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(w * 0.18f, h * 0.36f),
                    size = Size(w * 0.64f, h * 0.44f),
                    cornerRadius = CornerRadius(w * 0.10f),
                    style = stroke
                )
                drawLine(
                    color = color,
                    start = Offset(w * 0.36f, h * 0.14f),
                    end = Offset(w * 0.36f, h * 0.36f),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = color,
                    start = Offset(w * 0.64f, h * 0.14f),
                    end = Offset(w * 0.64f, h * 0.36f),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = color,
                    start = Offset(w * 0.50f, h * 0.80f),
                    end = Offset(w * 0.50f, h * 0.95f),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
            }

            ToolGlyph.LAN -> {
                val topLeft = Offset(w * 0.24f, h * 0.26f)
                val topRight = Offset(w * 0.76f, h * 0.26f)
                val bottom = Offset(w * 0.50f, h * 0.76f)
                drawLine(color, topLeft, bottom, strokeWidth = strokeWidth, cap = StrokeCap.Round)
                drawLine(color, topRight, bottom, strokeWidth = strokeWidth, cap = StrokeCap.Round)
                listOf(topLeft, topRight, bottom).forEach { center ->
                    drawCircle(
                        color = color,
                        radius = w * 0.095f,
                        center = center,
                        style = Stroke(width = strokeWidth)
                    )
                }
            }
        }
    }
}