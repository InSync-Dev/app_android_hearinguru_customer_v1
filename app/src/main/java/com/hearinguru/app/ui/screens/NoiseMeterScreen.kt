package com.hearinguru.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*

@Composable
fun NoiseMeterScreen() {
    val context = LocalContext.current
    var isRecording by remember { mutableStateOf(false) }
    var dbLevel by remember { mutableStateOf(42) }
    var hasPermission by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) }
    val scope = rememberCoroutineScope()
    var job by remember { mutableStateOf<Job?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasPermission = granted
        if (granted) isRecording = true
    }

    fun startMeasuring() {
        if (!hasPermission) { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO); return }
        isRecording = true
        job = scope.launch(Dispatchers.IO) {
            val sampleRate = 44100
            val bufSize = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
            val recorder = AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufSize * 4)
            recorder.startRecording()
            val buf = ShortArray(bufSize)
            while (isActive) {
                val read = recorder.read(buf, 0, buf.size)
                if (read > 0) {
                    val rms = buf.take(read).map { it.toLong() * it }.average().let { kotlin.math.sqrt(it) }
                    val db = (20 * kotlin.math.log10(rms.coerceAtLeast(1.0) / 32768.0) + 90).toInt().coerceIn(0, 120)
                    withContext(Dispatchers.Main) { dbLevel = db }
                }
                delay(200)
            }
            recorder.stop(); recorder.release()
        }
    }

    DisposableEffect(Unit) { onDispose { job?.cancel() } }

    val noiseCategory = when {
        dbLevel <= 70 -> NoiseLevel.SAFE
        dbLevel <= 85 -> NoiseLevel.CAUTION
        else -> NoiseLevel.DANGER
    }

    val numColor by animateColorAsState(
        targetValue = when (noiseCategory) {
            NoiseLevel.SAFE -> Color(0xFF80DEEA)
            NoiseLevel.CAUTION -> Color(0xFFFFE082)
            NoiseLevel.DANGER -> Color(0xFFEF9A9A)
        }, animationSpec = tween(500), label = "nc"
    )

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        // Display
        Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.inverseSurface, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(32.dp, 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("SOUND LEVEL RIGHT NOW", fontSize = 12.sp, color = Color.White.copy(alpha = 0.4f), letterSpacing = 1.sp)
                Spacer(Modifier.height(8.dp))
                Text("$dbLevel", fontSize = 86.sp, fontWeight = FontWeight.Bold, color = numColor, letterSpacing = (-3).sp)
                Text("DECIBELS (dB)", fontSize = 14.sp, color = Color.White.copy(alpha = 0.4f), letterSpacing = 1.sp)
                Spacer(Modifier.height(16.dp))
                Surface(shape = RoundedCornerShape(50), color = numColor.copy(alpha = 0.12f)) {
                    Row(Modifier.padding(horizontal = 18.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(10.dp).clip(RoundedCornerShape(50)).background(numColor))
                        Text(
                            when (noiseCategory) {
                                NoiseLevel.SAFE -> "Safe for your ears"
                                NoiseLevel.CAUTION -> "Be careful — extended exposure is risky"
                                NoiseLevel.DANGER -> "Dangerous — protect your ears now"
                            },
                            color = numColor, fontWeight = FontWeight.Bold, fontSize = 15.sp
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Text("WHAT DIFFERENT LEVELS MEAN", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 0.8.sp)
        Spacer(Modifier.height(8.dp))

        val refs = listOf(
            Triple(Color(0xFF80DEEA), "0 – 70 dB", "Safe · Normal talking, library"),
            Triple(Color(0xFFFFE082), "71 – 85 dB", "Be careful · Traffic, busy road"),
            Triple(Color(0xFFFFAB91), "86 – 100 dB", "Harmful · Power tools, drilling"),
            Triple(Color(0xFFEF9A9A), "100+ dB", "Dangerous · Can damage hearing"),
        )
        refs.forEachIndexed { i, (color, range, desc) ->
            Row(Modifier.padding(vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(14.dp).clip(RoundedCornerShape(50)).background(color))
                Text(range, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.width(90.dp))
                Text(desc, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (i < refs.size - 1) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }

        Spacer(Modifier.height(20.dp))
        Button(
            onClick = { if (isRecording) { job?.cancel(); isRecording = false } else startMeasuring() },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(50)
        ) {
            Icon(if (isRecording) Icons.Default.Stop else Icons.Default.Mic, null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(if (isRecording) "Stop measuring" else "Measure noise around me", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
        Spacer(Modifier.height(24.dp))
    }
}

enum class NoiseLevel { SAFE, CAUTION, DANGER }
