package com.hearinguru.app.ui.screens

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.sin

// ─── Intro screen ────────────────────────────────────────────────────────────
@Composable
fun HearingTestScreen(onStartTest: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Hearing, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(36.dp))
        }
        Spacer(Modifier.height(18.dp))
        Text("Hearing Test", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "This test checks how well you can hear different sounds. It takes about 5 minutes and you can do it right here at home.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 24.sp
        )
        Spacer(Modifier.height(20.dp))

        NoticeBox(
            icon = Icons.Default.Headphones,
            text = "Please wear earphones or headphones before starting for the most accurate result.",
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
        )
        Spacer(Modifier.height(10.dp))
        NoticeBox(
            icon = Icons.Default.VolumeOff,
            text = "Find a quiet room before you start. Background noise affects your result.",
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer
        )

        Spacer(Modifier.height(20.dp))
        Text("HOW THE TEST WORKS", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 0.8.sp)
        Spacer(Modifier.height(12.dp))

        val steps = listOf(
            "We play a soft sound in one ear at a time",
            "Tap the big button when you hear the sound",
            "If you cannot hear it, tap \"I cannot hear this\"",
            "You will get your result with a simple explanation"
        )
        steps.forEachIndexed { i, text ->
            Row(
                Modifier.padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text("${i + 1}", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                }
                Text(text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 6.dp), lineHeight = 22.sp)
            }
            if (i < steps.size - 1) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }

        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onStartTest,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(50),
        ) {
            Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Begin test now", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun NoticeBox(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, containerColor: Color, contentColor: Color) {
    Surface(shape = RoundedCornerShape(12.dp), color = containerColor) {
        Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Icon(icon, null, tint = contentColor, modifier = Modifier.size(22.dp).padding(top = 1.dp))
            Text(text, style = MaterialTheme.typography.bodyMedium, color = contentColor, lineHeight = 20.sp)
        }
    }
}

// ─── Active test screen ───────────────────────────────────────────────────────
@Composable
fun TestActiveScreen(onResult: () -> Unit) {
    var step by remember { mutableIntStateOf(1) }
    val totalSteps = 8
    val frequencies = listOf(500, 1000, 2000, 4000, 500, 1000, 2000, 4000)
    val ears = listOf("Left ear", "Right ear", "Right ear", "Right ear", "Left ear", "Left ear", "Left ear", "Right ear")
    val scope = rememberCoroutineScope()
    val currentFreq = frequencies.getOrElse(step - 1) { 1000 }
    val currentEar = ears.getOrElse(step - 1) { "Right ear" }

    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    val waveAnim by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900)), label = "w"
    )

    // Play tone when step changes
    LaunchedEffect(step) {
        withContext(Dispatchers.IO) {
            playTone(currentFreq.toDouble(), 1500, if (currentEar.contains("Left")) 0 else 1)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Step $step of $totalSteps — $currentEar", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { step.toFloat() / totalSteps },
            modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(20.dp))

        Surface(
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.primary
        ) {
            Row(Modifier.padding(horizontal = 18.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Hearing, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(18.dp))
                Text("Testing your $currentEar", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
        Spacer(Modifier.height(20.dp))

        // Freq card
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("SOUND FREQUENCY", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f), letterSpacing = 0.8.sp)
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("$currentFreq", fontSize = 54.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, letterSpacing = (-1).sp)
                    Text(" Hz", fontSize = 20.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f), modifier = Modifier.padding(bottom = 8.dp))
                }
                // Animated wave bars
                Row(
                    Modifier.height(44.dp),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val barHeights = listOf(12, 26, 40, 32, 18, 36, 40, 24, 12)
                    barHeights.forEachIndexed { i, h ->
                        val animH by rememberInfiniteTransition(label = "b$i").animateFloat(
                            initialValue = h.toFloat(), targetValue = (h * 0.3f),
                            animationSpec = infiniteRepeatable(tween(900, delayMillis = i * 80), RepeatMode.Reverse), label = "bar$i"
                        )
                        Box(
                            Modifier
                                .width(5.dp)
                                .height(animH.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text("Sound is playing now...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
            }
        }

        Spacer(Modifier.height(24.dp))
        Text("Did you hear that sound?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text("Tap the button below as soon as you hear anything", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        Spacer(Modifier.height(28.dp))

        // Big hear button
        FilledIconButton(
            onClick = {
                scope.launch {
                    if (step < totalSteps) step++ else onResult()
                }
            },
            modifier = Modifier.size(110.dp),
            shape = RoundedCornerShape(50),
            colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Hearing, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(36.dp))
                Text("Yes, I\nheard it", color = MaterialTheme.colorScheme.onPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, lineHeight = 14.sp)
            }
        }
        Spacer(Modifier.height(20.dp))
        OutlinedButton(
            onClick = {
                scope.launch {
                    if (step < totalSteps) step++ else onResult()
                }
            },
            shape = RoundedCornerShape(50),
            modifier = Modifier.height(50.dp)
        ) {
            Text("I could not hear this sound", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        }
    }
}

private fun playTone(freqHz: Double, durationMs: Int, channel: Int) {
    val sampleRate = 44100
    val numSamples = (sampleRate * durationMs / 1000.0).toInt()
    val buffer = ShortArray(numSamples) { i ->
        (sin(2 * PI * freqHz * i / sampleRate) * Short.MAX_VALUE * 0.5).toInt().toShort()
    }
    val audioTrack = AudioTrack(
        AudioManager.STREAM_MUSIC, sampleRate,
        if (channel == 0) AudioFormat.CHANNEL_OUT_MONO else AudioFormat.CHANNEL_OUT_STEREO,
        AudioFormat.ENCODING_PCM_16BIT, buffer.size * 2, AudioTrack.MODE_STATIC
    )
    audioTrack.write(buffer, 0, buffer.size)
    audioTrack.play()
    Thread.sleep(durationMs.toLong())
    audioTrack.stop()
    audioTrack.release()
}

// ─── Result screen ────────────────────────────────────────────────────────────
@Composable
fun TestResultScreen(
    onBookConsult: () -> Unit,
    onBrowseHAs: () -> Unit,
    onRetake: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Result hero
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(28.dp, 22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🔔", fontSize = 52.sp)
                Spacer(Modifier.height(10.dp))
                Text("Mild Hearing Loss", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(6.dp))
                Text("Your hearing is slightly reduced. Very common after age 40 and very treatable.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer, textAlign = TextAlign.Center, lineHeight = 20.sp)
            }
        }

        Spacer(Modifier.height(12.dp))

        // Severity scale
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(18.dp)) {
                Text("WHERE YOU FALL ON THE SCALE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 0.8.sp)
                Spacer(Modifier.height(12.dp))
                Box(
                    Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(6.dp))
                        .background(Brush.horizontalGradient(listOf(Color(0xFF4CAF50), Color(0xFFFF9800), Color(0xFFF44336))))
                )
                Spacer(Modifier.height(6.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    listOf("Normal", "Mild", "Moderate", "Severe").forEach {
                        Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                    Row(Modifier.padding(12.dp, 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ArrowUpward, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Text("You are here — mild range (25–40 dB)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // What this means
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(18.dp)) {
                Text("What this means for you", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                listOf(
                    Pair("💬", "Hard to follow conversations in noisy places like restaurants or family gatherings"),
                    Pair("📺", "You may need to turn the TV louder than others in the room prefer"),
                    Pair("📱", "Phone calls may sometimes be difficult to hear clearly"),
                ).forEach { (emoji, text) ->
                    Row(Modifier.padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(emoji, fontSize = 20.sp)
                        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 20.sp)
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // CTA dark card
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.inverseSurface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(22.dp)) {
                Surface(shape = RoundedCornerShape(50), color = Color.White.copy(alpha = 0.12f)) {
                    Row(Modifier.padding(horizontal = 12.dp, vertical = 5.dp), horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Verified, null, tint = MaterialTheme.colorScheme.inversePrimary, modifier = Modifier.size(14.dp))
                        Text("Based on your result", color = MaterialTheme.colorScheme.inversePrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text("A hearing aid could help you a lot", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.inverseOnSurface)
                Spacer(Modifier.height(8.dp))
                Text("Our specialist will review your results and recommend the right hearing aid for your lifestyle — completely free.", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.72f), lineHeight = 20.sp)
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = onBookConsult,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.inversePrimary, contentColor = MaterialTheme.colorScheme.onPrimaryContainer)
                ) {
                    Icon(Icons.Default.SupportAgent, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Book free consultation", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onBrowseHAs,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.inverseOnSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                ) {
                    Icon(Icons.Default.HearingDisabled, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Browse hearing aids", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onRetake, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(50)) {
            Icon(Icons.Default.Refresh, null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Take the test again", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
        Spacer(Modifier.height(24.dp))
    }
}
