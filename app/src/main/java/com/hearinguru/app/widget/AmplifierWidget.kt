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
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.hearinguru.app.service.AudioService
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch


class AmplifierWidget : GlanceAppWidget() {


   override suspend fun provideGlance(context: Context, id: GlanceId) {
      val prefs        = context.getSharedPreferences(AudioService.PREFS_NAME, Context.MODE_PRIVATE)
      val isOn         = prefs.getBoolean(AudioService.PREF_AMP_ON, false)
      val toggleIntent = if (isOn) {
         AudioService.stopAmpIntent(context)
      } else {
         AudioService.startAmpIntent(context, 0.6f)
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
               Text(
                  text  = "Sound Amplifier",
                  style = TextStyle(
                     color      = GlanceTheme.colors.onBackground,
                     fontWeight = FontWeight.Medium
                  )
               )

               Spacer(modifier = GlanceModifier.height(4))

               Text(
                  text  = if (isOn) "🎙 Listening..." else "🔇 Off",
                  style = TextStyle(
                     color      = GlanceTheme.colors.onBackground,
                     fontWeight = FontWeight.Bold
                  )
               )

               Spacer(modifier = GlanceModifier.height(8))

               Text(
                  text     = if (isOn) "Turn Off" else "Turn On",
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


class AmplifierWidgetReceiver : GlanceAppWidgetReceiver() {

   override val glanceAppWidget: GlanceAppWidget = AmplifierWidget()


   override fun onReceive(context: Context, intent: Intent) {
      super.onReceive(context, intent)
      if (intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE) {
         val pendingResult = goAsync()
         MainScope().launch {
            AmplifierWidget().updateAll(context)
            pendingResult.finish()
         }
      }
   }
}
