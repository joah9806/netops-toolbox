package com.qoder.netops.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class ToolTab(val label: String, val glyph: ToolGlyph) {
    PING("Ping", ToolGlyph.PING),
    PORTS("端口", ToolGlyph.PORT),
    LAN("局域网", ToolGlyph.LAN)
}

@Composable
fun NetOpsApp() {
    var selectedIndex by rememberSaveable { mutableIntStateOf(0) }
    val tabs = ToolTab.entries
    val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { AppTopBar() },
        bottomBar = {
            if (!imeVisible) {
                BottomToolBar(
                    selected = tabs[selectedIndex],
                    onSelect = { selectedIndex = it.ordinal }
                )
            }
        }
    ) { innerPadding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
        ) {
            Crossfade(targetState = tabs[selectedIndex], label = "toolTab") { tab ->
                when (tab) {
                    ToolTab.PING -> PingScreen()
                    ToolTab.PORTS -> PortScreen()
                    ToolTab.LAN -> LanScreen()
                }
            }
        }
    }
}

@Composable
private fun BottomToolBar(selected: ToolTab, onSelect: (ToolTab) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Surface(color = scheme.surface) {
        Column {
            HorizontalDivider(color = scheme.outlineVariant)
            Row(
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                ToolTab.entries.forEach { tab ->
                    val active = tab == selected
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .testTag("tab_${tab.name.lowercase()}")
                            .clickable { onSelect(tab) }
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        ToolGlyphIcon(
                            glyph = tab.glyph,
                            color = if (active) scheme.onSurface else scheme.onSurfaceVariant,
                            modifier = Modifier.size(21.dp)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = tab.label,
                            fontSize = 10.sp,
                            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (active) scheme.onSurface else scheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(3.dp))
                        Box(
                            Modifier
                                .size(width = if (active) 14.dp else 0.dp, height = 2.dp)
                                .clip(RoundedCornerShape(50))
                                .background(if (active) scheme.onSurface else scheme.surface)
                        )
                    }
                }
            }
        }
    }
}