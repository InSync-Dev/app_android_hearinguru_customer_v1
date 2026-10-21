package com.hearinguru.app.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartService
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.hearinguru.app.service.AudioService
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch


private val soundNames  = listOf("White Noise", "Pink Noise", "Ocean Waves", "Rainfall")
private val soundEmojis = listOf("🌫️", "🌸", "🌊", "🌧️")


class TinnitusWidget : GlanceAppWidget() {


   override suspend fun provideGlance(context: Context, id: GlanceId) {
      val prefs      = context.getSharedPreferences(AudioService.PREFS_NAME, Context.MODE_PRIVATE)
      val isPlaying  = prefs.getBoolean(AudioService.PREF_TINNITUS_ON, false)
      val soundIndex = prefs.getInt(AudioService.PREF_TINNITUS_SOUND, 0)
      val soundName  = soundNames.getOrElse(soundIndex)  { "White Noise" }
      val soundEmoji = soundEmojis.getOrElse(soundIndex) { "🌫️" }

      val toggleIntent: Intent = if (isPlaying) {
         AudioService.stopTinnitusIntent(context)
      } else {
         AudioService.startTinnitusIntent(context, soundIndex, 0.65f)
      }

      provideContent {
         GlanceTheme {
            Column(
               modifier = GlanceModifier
                  .fillMaxSize()
                  .background(GlanceTheme.colors.background)
                  .padding(horizontal = 16, vertical = 12),
               verticalAlignment = Alignment.CenterVertically
            ) {
               Row(
                  modifier          = GlanceModifier.fillMaxWidth(),
                  verticalAlignment = Alignment.CenterVertically
               ) {
                  Text(
                     text  = "Tinnitus Relief",
                     style = TextStyle(
                        color      = GlanceTheme.colors.onBackground,
                        fontWeight = FontWeight.Medium
                     )
                  )
               }

               Spacer(modifier = GlanceModifier.height(4))

               Text(
                  text  = if (isPlaying) "$soundEmoji $soundName" else "⏸ Stopped",
                  style = TextStyle(
                     color      = GlanceTheme.colors.onBackground,
                     fontWeight = FontWeight.Bold
                  )
               )

               Spacer(modifier = GlanceModifier.height(8))

               Text(
                  text     = if (isPlaying) "Stop" else "▶ Play",
                  modifier = GlanceModifier
                     .background(GlanceTheme.colors.primary)
                     .padding(horizontal = 12, vertical = 6)
                     .clickable(onClick = actionStartService(toggleIntent, isForegroundService = true)),
                  style = TextStyle(
                     color      = GlanceTheme.colors.onPrimary,
                     fontWeight = FontWeight.Medium
                  )
               )
            }
         }
      }
   }
}


class TinnitusWidgetReceiver : GlanceAppWidgetReceiver() {

   override val glanceAppWidget: GlanceAppWidget = TinnitusWidget()


   override fun onReceive(context: Context, intent: Intent) {
      super.onReceive(context, intent)
      if (intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE) {
         val pendingResult = goAsync()
         MainScope().launch {
            TinnitusWidget().updateAll(context)
            pendingResult.finish()
         }
      }
   }
}
