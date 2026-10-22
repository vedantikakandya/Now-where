package com.example.now_where.ui.visualization.route

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.now_where.domain.DayState
import com.example.now_where.domain.Event
import com.example.now_where.domain.TimeNotation
import com.example.now_where.ui.theme.NowWhereColors
import java.time.LocalDate
import java.time.LocalTime

// The preview must have a different name from the composable it calls.
// Before, both were called RoutePreview, so the preview was calling itself.
@Preview(widthDp = 390, heightDp = 800, showBackground = true)
@Composable
private fun RouteVisualizationPreview() {
    val today = LocalDate.now()
    val day = DayState(
        now = today.atTime(LocalTime.of(16, 42)),
        events = listOf(
            Event(1, "College", today.atTime(9, 0), today.atTime(13, 0)),
            Event(2, "Lunch", today.atTime(13, 0), today.atTime(14, 0)),
            Event(3, "Gym", today.atTime(17, 0), today.atTime(18, 30)),
            Event(4, "Study", today.atTime(19, 0), today.atTime(20, 30)),
            Event(5, "Dinner", today.atTime(21, 0), today.atTime(22, 0)),
        ),
        notion = TimeNotation.H24
    )
    Box(Modifier.fillMaxSize().background(NowWhereColors.Background)) {
        RouteVisualization(day = day, modifier = Modifier.fillMaxSize())
    }
}