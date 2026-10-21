package com.hearinguru.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import com.hearinguru.app.service.AudioService


@Composable
fun SoundAmplifierScreen() {
   val context = LocalContext.current

   // Mirror service state so UI reflects widget toggles too
   val isOn    by AudioService.ampOn.collectAsState()
   val dbLevel by AudioService.ampDbLevel.collectAsState()

   var gain by remember { mutableFloatStateOf(0.6f) }
   var bass by remember { mutableFloatStateOf(0.3f) }
   var selectedMode by remember { mutableStateOf("Normal") }

   var hasPermission by remember {
      mutableStateOf(
         ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            == PackageManager.PERMISSION_GRANTED
      )
   }

   val permissionLauncher = rememberLauncherForActivityResult(
      ActivityResultContracts.RequestPermission()
   ) { granted ->
      hasPermission = granted
      if (granted) {
         val startIntent = AudioService.startAmpIntent(context, gain)
         ContextCompat.startForegroundService(context, startIntent)
      }
   }


   fun toggle() {
      if (isOn) {
         val stopIntent = AudioService.stopAmpIntent(context)
         context.startService(stopIntent)
      } else {
         if (!hasPermission) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
         }
         val startIntent = AudioService.startAmpIntent(context, gain)
         ContextCompat.startForegroundService(context, startIntent)
      }
   }


   Column(
      modifier = Modifier
         .fillMaxSize()
         .verticalScroll(rememberScrollState())
         .padding(16.dp),
      horizontalAlignment = Alignment.CenterHorizontally
   ) {
      // Display panel
      Surface(
         shape    = RoundedCornerShape(28.dp),
         color    = MaterialTheme.colorScheme.inverseSurface,
         modifier = Modifier.fillMaxWidth()
      ) {
         Column(
            Modifier.padding(28.dp, 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
         ) {
            Text(
               "INPUT LEVEL",
               fontSize     = 12.sp,
               color        = Color.White.copy(alpha = 0.4f),
               letterSpacing = 1.sp
            )
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.Bottom) {
               Text(
                  "$dbLevel",
                  fontSize     = 64.sp,
                  fontWeight   = FontWeight.Bold,
                  color        = MaterialTheme.colorScheme.inversePrimary,
                  letterSpacing = (-2).sp
               )
               Text(
                  " dB",
                  fontSize = 20.sp,
                  color    = Color.White.copy(alpha = 0.45f),
                  modifier = Modifier.padding(bottom = 10.dp)
               )
            }
            // Level bar visualiser
            Row(
               Modifier.height(44.dp),
               horizontalArrangement = Arrangement.spacedBy(4.dp),
               verticalAlignment     = Alignment.CenterVertically
            ) {
               repeat(12) { barIndex ->
                  val isActive = isOn && (dbLevel / 10) > barIndex
                  val barHeight = if (isActive) (12 + (barIndex % 4) * 8).dp else 4.dp
                  val barColor  = if (isActive) MaterialTheme.colorScheme.inversePrimary
                                  else Color.White.copy(alpha = 0.15f)
                  Box(
                     Modifier
                        .width(4.dp)
                        .height(barHeight)
                        .clip(RoundedCornerShape(2.dp))
                        .background(barColor)
                  )
               }
            }
         }
      }

      Spacer(Modifier.height(24.dp))

      // Big toggle button
      FilledIconButton(
         onClick  = ::toggle,
         modifier = Modifier.size(96.dp),
         shape    = RoundedCornerShape(28.dp),
         colors   = IconButtonDefaults.filledIconButtonColors(
            containerColor = if (isOn) MaterialTheme.colorScheme.primaryContainer
                             else MaterialTheme.colorScheme.surfaceContainerHigh
         )
      ) {
         Icon(
            Icons.Default.Mic,
            null,
            modifier = Modifier.size(36.dp),
            tint     = if (isOn) MaterialTheme.colorScheme.primary
                       else MaterialTheme.colorScheme.onSurfaceVariant
         )
      }
      Spacer(Modifier.height(8.dp))
      Text(
         if (isOn) "Listening..." else "Tap to start",
         fontWeight = FontWeight.Bold,
         color      = if (isOn) MaterialTheme.colorScheme.primary
                      else MaterialTheme.colorScheme.onSurfaceVariant
      )

      Spacer(Modifier.height(24.dp))

      // Gain control
      ControlCard(label = "How loud to make it") {
         SliderRow(
            icon          = Icons.Default.VolumeDown,
            value         = gain,
            onValueChange = { gain = it },
            display       = "${(gain * 100).toInt()}%"
         )
      }
      Spacer(Modifier.height(10.dp))
      ControlCard(label = "Bass (low sounds)") {
         SliderRow(
            icon          = Icons.Default.Equalizer,
            value         = bass,
            onValueChange = { bass = it },
            display       = "${(bass * 100).toInt()}%"
         )
      }
      Spacer(Modifier.height(10.dp))
      ControlCard(label = "Best used for") {
         val modes = listOf("Normal", "Conversation", "Watching TV", "Outdoors")
         Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            modes.chunked(2).forEach { modeChunk ->
               Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                  modeChunk.forEach { mode ->
                     FilterChip(
                        selected = selectedMode == mode,
                        onClick  = { selectedMode = mode },
                        label    = { Text(mode, fontWeight = FontWeight.SemiBold) },
                        modifier = Modifier.fillMaxWidth()
                     )
                  }
               }
            }
         }
      }
      Spacer(Modifier.height(80.dp))
   }
}


@Composable
fun ControlCard(label: String, content: @Composable ColumnScope.() -> Unit) {
   Surface(
      shape    = RoundedCornerShape(16.dp),
      color    = MaterialTheme.colorScheme.surfaceContainerLow,
      modifier = Modifier.fillMaxWidth(),
      border   = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
   ) {
      Column(Modifier.padding(18.dp)) {
         Text(
            label.uppercase(),
            style    = MaterialTheme.typography.labelSmall,
            color    = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight    = FontWeight.Bold,
            letterSpacing = 0.8.sp
         )
         Spacer(Modifier.height(12.dp))
         content()
      }
   }
}


@Composable
fun SliderRow(
   icon: androidx.compose.ui.graphics.vector.ImageVector,
   value: Float,
   onValueChange: (Float) -> Unit,
   display: String
) {
   Row(
      verticalAlignment     = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(14.dp)
   ) {
      Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
      Slider(
         value         = value,
         onValueChange = onValueChange,
         modifier      = Modifier.weight(1f),
         colors        = SliderDefaults.colors(
            thumbColor       = MaterialTheme.colorScheme.primary,
            activeTrackColor = MaterialTheme.colorScheme.primary
         )
      )
      Text(
         display,
         style      = MaterialTheme.typography.bodyMedium,
         color      = MaterialTheme.colorScheme.primary,
         fontWeight = FontWeight.Bold,
         modifier   = Modifier.width(44.dp)
      )
   }
}
