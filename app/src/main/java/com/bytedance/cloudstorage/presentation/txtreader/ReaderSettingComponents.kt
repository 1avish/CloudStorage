package com.bytedance.cloudstorage.presentation.txtreader

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.FormatLineSpacing
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bytedance.cloudstorage.utils.w
import com.bytedance.cloudstorage.utils.ws

// ── 阅读器设置面板：底部 Tab 栏 + 可展开的设置内容 ──

@Composable
internal fun ReaderSettingsOverlay(
    activePanel: ReaderSettingPanel?,
    onActivePanelChange: (ReaderSettingPanel) -> Unit,
    brightness: Float,
    onBrightnessChange: (Float) -> Unit,
    useSystemBrightness: Boolean,
    onUseSystemBrightnessChange: (Boolean) -> Unit,
    backgroundIndex: Int,
    onBackgroundIndexChange: (Int) -> Unit,
    fontSizeIndex: Int,
    onDecreaseFontSize: () -> Unit,
    onIncreaseFontSize: () -> Unit,
    lineSpacingIndex: Int,
    onLineSpacingIndexChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        activePanel
            ?.takeIf { it == ReaderSettingPanel.Brightness || it == ReaderSettingPanel.Background }
            ?.let { panel ->
            ReaderSettingPanelContent(
                panel = panel,
                brightness = brightness,
                onBrightnessChange = onBrightnessChange,
                useSystemBrightness = useSystemBrightness,
                onUseSystemBrightnessChange = onUseSystemBrightnessChange,
                backgroundIndex = backgroundIndex,
                onBackgroundIndexChange = onBackgroundIndexChange,
                fontSizeIndex = fontSizeIndex,
                onDecreaseFontSize = onDecreaseFontSize,
                onIncreaseFontSize = onIncreaseFontSize,
                lineSpacingIndex = lineSpacingIndex,
                onLineSpacingIndexChange = onLineSpacingIndexChange
            )
        }
        ReaderSettingsBottomBar(
            activePanel = activePanel,
            onActivePanelChange = onActivePanelChange
        )
    }
}

// 根据当前选中的 Tab 页，渲染对应的设置内容行
@Composable
private fun ReaderSettingPanelContent(
    panel: ReaderSettingPanel,
    brightness: Float,
    onBrightnessChange: (Float) -> Unit,
    useSystemBrightness: Boolean,
    onUseSystemBrightnessChange: (Boolean) -> Unit,
    backgroundIndex: Int,
    onBackgroundIndexChange: (Int) -> Unit,
    fontSizeIndex: Int,
    onDecreaseFontSize: () -> Unit,
    onIncreaseFontSize: () -> Unit,
    lineSpacingIndex: Int,
    onLineSpacingIndexChange: (Int) -> Unit,
) {
    when (panel) {
        ReaderSettingPanel.Brightness -> BrightnessSettingRow(
            brightness = brightness,
            onBrightnessChange = onBrightnessChange,
            useSystemBrightness = useSystemBrightness,
            onUseSystemBrightnessChange = onUseSystemBrightnessChange
        )
        ReaderSettingPanel.Background -> BackgroundSettingRow(
            selectedIndex = backgroundIndex,
            onSelected = onBackgroundIndexChange
        )
        ReaderSettingPanel.Font -> FontSettingRow(
            fontSizeIndex = fontSizeIndex,
            onDecrease = onDecreaseFontSize,
            onIncrease = onIncreaseFontSize
        )
        ReaderSettingPanel.LineSpacing -> LineSpacingSettingRow(
            selectedIndex = lineSpacingIndex,
            onSelected = onLineSpacingIndexChange
        )
    }
}

// 亮度面板：[小太阳] ──[Slider]── [大太阳]  [○ 跟随系统]
@Composable
private fun BrightnessSettingRow(
    brightness: Float,
    onBrightnessChange: (Float) -> Unit,
    useSystemBrightness: Boolean,
    onUseSystemBrightnessChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(READER_SETTING_ROW_HEIGHT.w.dp)
            .background(ReaderPanelSurface)
            .padding(horizontal = 18.w.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Brightness6,
            contentDescription = null,
            tint = ReaderSkyBlue,
            modifier = Modifier.size(22.w.dp)
        )
        Slider(
            value = brightness,
            onValueChange = onBrightnessChange,
            valueRange = 0.05f..1f,
            colors = SliderDefaults.colors(
                thumbColor = ReaderSkyBlue,
                activeTrackColor = ReaderSkyBlue,
                inactiveTrackColor = ReaderSkyBlue.copy(alpha = 0.18f)
            ),
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 10.w.dp)
        )
        Icon(
            imageVector = Icons.Default.Brightness6,
            contentDescription = null,
            tint = ReaderSkyBlue,
            modifier = Modifier.size(28.w.dp)
        )
        Spacer(modifier = Modifier.width(8.w.dp))
        Row(
            modifier = Modifier.clickable { onUseSystemBrightnessChange(!useSystemBrightness) },
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = useSystemBrightness,
                onClick = { onUseSystemBrightnessChange(!useSystemBrightness) },
                colors = RadioButtonDefaults.colors(
                    selectedColor = ReaderSkyBlue,
                    unselectedColor = ReaderSkyBlue
                )
            )
            Text(
                text = "系统亮度",
                fontSize = 15.ws.sp,
                color = ReaderSkyBlue,
                maxLines = 1
            )
        }
    }
}

// 背景色面板：5 个圆形色块，选中项显示蓝色描边
@Composable
private fun BackgroundSettingRow(
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(READER_SETTING_ROW_HEIGHT.w.dp)
            .background(ReaderPanelSurface)
            .padding(horizontal = 18.w.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ReaderBackgroundOptions.forEachIndexed { index, color ->
                val selected = selectedIndex == index
                Box(
                    modifier = Modifier
                        .size(34.w.dp)
                        .clip(CircleShape)
                        .background(color)
                        .border(
                            width = if (selected) 2.w.dp else 1.w.dp,
                            color = if (selected) ReaderBlue else ReaderDivider,
                            shape = CircleShape
                        )
                        .clickable { onSelected(index) }
                )
            }
        }
    }
}

// 字号面板：[A-]  3/6  [A+]，数字表示当前档位（1~6）
@Composable
private fun FontSettingRow(
    fontSizeIndex: Int,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(READER_SETTING_ROW_HEIGHT.w.dp)
            .background(ReaderPanelSurface)
            .padding(horizontal = 36.w.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ReaderPillButton(
            text = "A-",
            enabled = fontSizeIndex > 0,
            onClick = onDecrease,
            modifier = Modifier.width(88.w.dp)
        )
        Text(
            text = "${fontSizeIndex + 1}",
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 10.w.dp),
            fontSize = 18.ws.sp,
            fontWeight = FontWeight.Medium,
            color = ReaderText,
            maxLines = 1,
            textAlign = TextAlign.Center
        )
        ReaderPillButton(
            text = "A+",
            enabled = fontSizeIndex < ReaderFontSizes.lastIndex,
            onClick = onIncrease,
            modifier = Modifier.width(88.w.dp)
        )
    }
}

// 行间距面板：胶囊 segmented control，4 档可选
@Composable
private fun LineSpacingSettingRow(
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(READER_SETTING_ROW_HEIGHT.w.dp)
            .background(ReaderPanelSurface)
            .padding(horizontal = 18.w.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .height(40.w.dp)
                .clip(RoundedCornerShape(22.w.dp))
                .background(ReaderControlBg)
                .padding(3.w.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ReaderLineSpacingLabels.forEachIndexed { index, label ->
                val selected = selectedIndex == index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clip(RoundedCornerShape(20.w.dp))
                        .background(if (selected) Color.White else Color.Transparent)
                        .border(
                            width = if (selected) 1.w.dp else 0.w.dp,
                            color = if (selected) ReaderDivider else Color.Transparent,
                            shape = RoundedCornerShape(20.w.dp)
                        )
                        .clickable { onSelected(index) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontSize = 15.ws.sp,
                        fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                        color = if (selected) ReaderText else ReaderSubText,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

// 胶囊按钮（A-/A+），到达边界时半透明 + 禁用点击
@Composable
private fun ReaderPillButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(40.w.dp)
            .clip(RoundedCornerShape(22.w.dp))
            .background(ReaderControlBg)
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 17.ws.sp,
            fontWeight = FontWeight.Medium,
            color = if (enabled) ReaderSubText else ReaderSubText.copy(alpha = 0.4f),
            maxLines = 1
        )
    }
}

// 底部 Tab 栏：固定在页面最下方，支持导航栏安全区
@Composable
private fun ReaderSettingsBottomBar(
    activePanel: ReaderSettingPanel?,
    onActivePanelChange: (ReaderSettingPanel) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ReaderPanelSurface)
            .border(width = 1.w.dp, color = ReaderDivider)
            .navigationBarsPadding()
            .padding(horizontal = 14.w.dp, vertical = 4.w.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ReaderBottomBarItem(
                label = "亮度",
                icon = Icons.Default.Brightness6,
                selected = activePanel == ReaderSettingPanel.Brightness,
                accentColor = ReaderSkyBlue,
                onClick = { onActivePanelChange(ReaderSettingPanel.Brightness) },
                modifier = Modifier.weight(1f)
            )
            ReaderBottomBarItem(
                label = "背景",
                icon = Icons.Default.Palette,
                selected = activePanel == ReaderSettingPanel.Background,
                accentColor = ReaderBlue,
                onClick = { onActivePanelChange(ReaderSettingPanel.Background) },
                modifier = Modifier.weight(1f)
            )
            if (false) {
            ReaderBottomBarItem(
                label = "字体",
                icon = Icons.Default.FormatSize,
                selected = activePanel == ReaderSettingPanel.Font,
                accentColor = ReaderBlue,
                onClick = { onActivePanelChange(ReaderSettingPanel.Font) },
                modifier = Modifier.weight(1f)
            )
            ReaderBottomBarItem(
                label = "行间距",
                icon = Icons.Default.FormatLineSpacing,
                selected = activePanel == ReaderSettingPanel.LineSpacing,
                accentColor = ReaderBlue,
                onClick = { onActivePanelChange(ReaderSettingPanel.LineSpacing) },
                modifier = Modifier.weight(1f)
            )
            }
        }
    }
}

// 单个 Tab 项：图标 + 标签，selectedColor 由调用方指定（亮度用 SkyBlue，其他用 Blue）
@Composable
private fun ReaderBottomBarItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .height(READER_BOTTOM_BAR_ITEM_HEIGHT.w.dp)
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) accentColor else ReaderText,
            modifier = Modifier.size(22.w.dp)
        )
        Text(
            text = label,
            fontSize = 12.ws.sp,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
            color = if (selected) accentColor else ReaderText,
            maxLines = 1
        )
    }
}
