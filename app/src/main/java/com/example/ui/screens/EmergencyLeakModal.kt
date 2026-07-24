package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ErrorRed

@Composable
fun EmergencyLeakModal(
    isVisible: Boolean,
    pulses: Int,
    flowRateLpm: Float,
    isPending: Boolean,
    onClearLeakCommand: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(visible = isVisible, enter = fadeIn(), exit = fadeOut()) {
        val infiniteTransition = rememberInfiniteTransition(label = "leak_flash")
        val flashAlpha by infiniteTransition.animateFloat(
            initialValue = 0.7f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(500, easing = LinearEasing)),
            label = "flash"
        )

        Box(
            modifier = modifier.fillMaxSize().background(ErrorRed.copy(alpha = flashAlpha * 0.85f)).testTag("emergency_leak_overlay"),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(32.dp)
            ) {
                Icon(imageVector = Icons.Default.Warning, contentDescription = "Leak Warning", tint = Color.White, modifier = Modifier.size(80.dp))
                Spacer(Modifier.height(16.dp))
                Text(text = "LEAK DETECTED", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.White, letterSpacing = 2.sp)
                Spacer(Modifier.height(8.dp))
                Text(text = "EMERGENCY SHUTOFF ACTIVE", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color.White.copy(alpha = 0.9f), letterSpacing = 1.sp)
                Spacer(Modifier.height(24.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "$pulses", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text(text = "PULSES", fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "%.1f".format(flowRateLpm), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text(text = "L/MIN", fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
                    }
                }

                Spacer(Modifier.height(32.dp))

                Button(
                    onClick = onClearLeakCommand,
                    enabled = !isPending,
                    modifier = Modifier.fillMaxWidth(0.7f).height(56.dp).testTag("btn_clear_leak"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = ErrorRed)
                ) {
                    if (isPending) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 3.dp, color = ErrorRed)
                    } else {
                        Text(text = "RESET LEAK FLAG", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(Modifier.height(16.dp))
                Text(text = "Valve has been automatically shut off.\nClear the leak flag after inspection.", fontSize = 13.sp, color = Color.White.copy(alpha = 0.7f), textAlign = TextAlign.Center)
            }
        }
    }
}
