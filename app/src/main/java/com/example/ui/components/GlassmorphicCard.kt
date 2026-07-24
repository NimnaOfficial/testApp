package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberCardBg

@Composable
fun GlassmorphicCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = CyberCardBg.copy(alpha = 0.85f),
        border = BorderStroke(1.dp, CyberBorder),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Box(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}
