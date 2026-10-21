package com.hearinguru.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
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


data class SoundOption(val name: String, val description: String, val emoji: String, val subtitle: String)


@Composable
fun TinnitusScreen() {
   val context = LocalContext.current

   // Mirror service state so UI stays in sync even if service was started from widget
   val isPlaying   by AudioService.tinnitusPlaying.collectAsState()
   val selectedSound by AudioService.tinnitusSound.collectAsState()

   var volume by remember { mutableFloatStateOf(0.65f) }
   var selectedTimer by remember { mutableStateOf("30 min") }


   val sounds = listOf(
      SoundOption("White Noise", "Gentle broad sound · Relieves ringing",  "🌫️", "Good for tinnitus"),
      SoundOption("Pink Noise",  "Warm natural sound · Relaxing",          "🌸", "Warm, natural feel"),
      SoundOption("Ocean Waves", "Rhythm of the sea · Sleep aid",          "🌊", "Calming, for sleep"),
      SoundOption("Rainfall",    "Steady rain sounds · Focus",              "🌧️", "Steady, peaceful"),
   )


   fun startOrRestart(soundIndex: Int, vol: Float) {
      val startIntent = AudioService.startTinnitusIntent(context, soundIndex, vol)
      ContextCompat.startForegroundService(context, startIntent)
   }

   fun stop() {
      val stopIntent = AudioService.stopTinnitusIntent(context)
      context.startService(stopIntent)
   }

   fun togglePlayback() {
      if (isPlaying) stop() else startOrRestart(selectedSound, volume)
   }

   fun pickSound(index: Int) {
      val stopFirst = AudioService.stopTinnitusIntent(context)
      context.startService(stopFirst)
      val startWithNew = AudioService.startTinnitusIntent(context, index, volume)
      ContextCompat.startForegroundService(context, startWithNew)
   }


   // Restart when volume changes while already playing
   LaunchedEffect(volume) {
      if (isPlaying) startOrRestart(selectedSound, volume)
   }


   Column(
      modifier = Modifier
         .fillMaxSize()
         .verticalScroll(rememberScrollState())
         .padding(16.dp)
   ) {
      // Player card
      Surface(
         shape = RoundedCornerShape(28.dp),
         color = MaterialTheme.colorScheme.inverseSurface,
         modifier = Modifier.fillMaxWidth()
      ) {
         Column(
            Modifier.padding(24.dp, 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
         ) {
            Text(
               "NOW PLAYING",
               fontSize = 11.sp,
               color = Color.White.copy(alpha = 0.4f),
               letterSpacing = 1.sp
            )
            Spacer(Modifier.height(8.dp))
            Text(
               sounds[selectedSound].name,
               fontSize = 24.sp,
               fontWeight = FontWeight.Bold,
               color = Color.White
            )
            Text(
               sounds[selectedSound].description,
               fontSize = 13.sp,
               color = Color.White.copy(alpha = 0.5f)
            )
            Spacer(Modifier.height(20.dp))

            // Animated wave bars — animate when playing
            Row(
               Modifier.height(48.dp),
               horizontalArrangement = Arrangement.spacedBy(3.dp),
               verticalAlignment = Alignment.CenterVertically
            ) {
               listOf(10, 22, 38, 28, 14, 34, 38, 20, 10).forEachIndexed { barIndex, baseHeight ->
                  val playingTarget = if (isPlaying) (baseHeight * 0.15f) else baseHeight.toFloat()
                  val animatedHeight by rememberInfiniteTransition(label = "tw$barIndex").animateFloat(
                     initialValue = baseHeight.toFloat(),
                     targetValue  = playingTarget,
                     animationSpec = infiniteRepeatable(
                        tween(1400, delayMillis = barIndex * 120),
                        RepeatMode.Reverse
                     ),
                     label = "bar$barIndex"
                  )
                  Box(
                     Modifier
                        .width(4.dp)
                        .height(animatedHeight.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.inversePrimary)
                  )
               }
            }
            Spacer(Modifier.height(22.dp))

            // Transport controls
            Row(
               horizontalArrangement = Arrangement.spacedBy(28.dp),
               verticalAlignment = Alignment.CenterVertically
            ) {
               FilledTonalIconButton(
                  onClick = { pickSound((selectedSound - 1 + sounds.size) % sounds.size) },
                  modifier = Modifier.size(52.dp),
                  colors = IconButtonDefaults.filledTonalIconButtonColors(
                     containerColor = Color.White.copy(alpha = 0.1f),
                     contentColor   = Color.White
                  )
               ) { Icon(Icons.Default.SkipPrevious, null, modifier = Modifier.size(24.dp)) }

               FilledIconButton(
                  onClick = ::togglePlayback,
                  modifier = Modifier.size(68.dp),
                  shape = RoundedCornerShape(50),
                  colors = IconButtonDefaults.filledIconButtonColors(
                     containerColor = MaterialTheme.colorScheme.inversePrimary,
                     contentColor   = MaterialTheme.colorScheme.onPrimaryContainer
                  )
               ) {
                  Icon(
                     if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                     null,
                     modifier = Modifier.size(32.dp)
                  )
               }

               FilledTonalIconButton(
                  onClick = { pickSound((selectedSound + 1) % sounds.size) },
                  modifier = Modifier.size(52.dp),
                  colors = IconButtonDefaults.filledTonalIconButtonColors(
                     containerColor = Color.White.copy(alpha = 0.1f),
                     contentColor   = Color.White
                  )
               ) { Icon(Icons.Default.SkipNext, null, modifier = Modifier.size(24.dp)) }
            }
         }
      }

      Spacer(Modifier.height(12.dp))

      // Volume control
      ControlCard(label = "Volume") {
         SliderRow(
            icon          = Icons.Default.VolumeMute,
            value         = volume,
            onValueChange = { volume = it },
            display       = "${(volume * 100).toInt()}%"
         )
      }

      Spacer(Modifier.height(12.dp))
      Text(
         "CHOOSE A SOUND",
         style = MaterialTheme.typography.labelMedium,
         color = MaterialTheme.colorScheme.onSurfaceVariant,
         letterSpacing = 0.8.sp
      )
      Spacer(Modifier.height(8.dp))

      // Sound selection grid
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
         sounds.chunked(2).forEach { rowSounds ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
               rowSounds.forEach { sound ->
                  val soundIndex = sounds.indexOf(sound)
                  val isSelected = selectedSound == soundIndex

                  Surface(
                     modifier = Modifier
                        .weight(1f)
                        .clickable { pickSound(soundIndex) },
                     shape  = RoundedCornerShape(12.dp),
                     color  = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                              else MaterialTheme.colorScheme.surfaceContainerLow,
                     border = androidx.compose.foundation.BorderStroke(
                        1.5.dp,
                        if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant
                     )
                  ) {
                     Column(Modifier.padding(16.dp, 14.dp)) {
                        Text(sound.emoji, fontSize = 28.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(
                           sound.name,
                           fontWeight = FontWeight.Bold,
                           fontSize   = 15.sp,
                           color      = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                           sound.subtitle,
                           fontSize = 12.sp,
                           color    = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                     }
                  }
               }
            }
         }
      }

      Spacer(Modifier.height(16.dp))
      Text(
         "AUTO STOP AFTER",
         style = MaterialTheme.typography.labelMedium,
         color = MaterialTheme.colorScheme.onSurfaceVariant,
         letterSpacing = 0.8.sp
      )
      Spacer(Modifier.height(8.dp))

      Row(
         modifier              = Modifier.horizontalScroll(rememberScrollState()),
         horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
         listOf("15 min", "30 min", "1 hour", "2 hours", "Off").forEach { timerOption ->
            FilterChip(
               selected = selectedTimer == timerOption,
               onClick  = { selectedTimer = timerOption },
               label    = {
                  Text(timerOption, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
               }
            )
         }
      }
      Spacer(Modifier.height(24.dp))
   }
}
