package com.example.now_where.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val NowWhereColorScheme= darkColorScheme(
    primary=NowWhereColors.Orange,
    background = NowWhereColors.Background,
    surface = NowWhereColors.Surface,
    onBackground = NowWhereColors.Ink,
    onSurface = NowWhereColors.Ink,
)

@Composable
fun NowwhereTheme(

    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = NowWhereColorScheme,
        typography = Typography,
        content = content
    )
}

