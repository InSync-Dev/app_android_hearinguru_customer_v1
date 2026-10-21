package com.hearinguru.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import android.content.pm.ServiceInfo
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.hearinguru.app.MainActivity
import com.hearinguru.app.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.sqrt
import kotlin.random.Random


class AudioService : Service() {


   companion object {

      const val ACTION_START_TINNITUS = "com.hearinguru.app.START_TINNITUS"
      const val ACTION_STOP_TINNITUS  = "com.hearinguru.app.STOP_TINNITUS"
      const val ACTION_START_AMP      = "com.hearinguru.app.START_AMP"
      const val ACTION_STOP_AMP       = "com.hearinguru.app.STOP_AMP"

      const val EXTRA_SOUND_INDEX = "sound_index"
      const val EXTRA_VOLUME      = "volume"
      const val EXTRA_GAIN        = "gain"

      private const val CHANNEL_ID = "hearinguru_audio"
      private const val NOTIF_ID   = 1001

      // Prefs keys — used by widgets to read persisted state
      const val PREFS_NAME           = "hearinguru_audio_state"
      const val PREF_TINNITUS_ON     = "tinnitus_on"
      const val PREF_TINNITUS_SOUND  = "tinnitus_sound"
      const val PREF_AMP_ON          = "amp_on"


      private val _tinnitusPlaying = MutableStateFlow(false)
      val tinnitusPlaying: StateFlow<Boolean> = _tinnitusPlaying

      private val _tinnitusSound = MutableStateFlow(0)
      val tinnitusSound: StateFlow<Int> = _tinnitusSound

      private val _ampOn = MutableStateFlow(false)
      val ampOn: StateFlow<Boolean> = _ampOn

      private val _ampDbLevel = MutableStateFlow(0)
      val ampDbLevel: StateFlow<Int> = _ampDbLevel


      fun startTinnitusIntent(context: Context, soundIndex: Int, volume: Float): Intent =
         Intent(context, AudioService::class.java).apply {
            action = ACTION_START_TINNITUS
            putExtra(EXTRA_SOUND_INDEX, soundIndex)
            putExtra(EXTRA_VOLUME, volume)
         }

      fun stopTinnitusIntent(context: Context): Intent =
         Intent(context, AudioService::class.java).apply { action = ACTION_STOP_TINNITUS }

      fun startAmpIntent(context: Context, gain: Float): Intent =
         Intent(context, AudioService::class.java).apply {
            action = ACTION_START_AMP
            putExtra(EXTRA_GAIN, gain)
         }

      fun stopAmpIntent(context: Context): Intent =
         Intent(context, AudioService::class.java).apply { action = ACTION_STOP_AMP }
   }


   private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
   private var tinnitusJob: Job? = null
   private var ampJob: Job? = null

   // MediaPlayer used when ocean waves file is selected (soundIndex == 2)
   private var oceanMediaPlayer: MediaPlayer? = null

   private lateinit var prefs: SharedPreferences


   override fun onBind(intent: Intent?): IBinder? = null


   override fun onCreate() {
      super.onCreate()
      prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      createNotificationChannel()
   }


   override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
      when (intent?.action) {
         ACTION_START_TINNITUS -> {
            val soundIndex = intent.getIntExtra(EXTRA_SOUND_INDEX, 0)
            val volume = intent.getFloatExtra(EXTRA_VOLUME, 0.65f)
            handleStartTinnitus(soundIndex, volume)
         }
         ACTION_STOP_TINNITUS -> handleStopTinnitus()
         ACTION_START_AMP -> {
            val gain = intent.getFloatExtra(EXTRA_GAIN, 0.6f)
            handleStartAmp(gain)
         }
         ACTION_STOP_AMP -> handleStopAmp()
      }
      return START_STICKY
   }


   private fun handleStartTinnitus(soundIndex: Int, volume: Float) {
      tinnitusJob?.cancel()

      // Release any previous ocean MediaPlayer before starting fresh
      releaseOceanPlayer()

      _tinnitusPlaying.value = true
      _tinnitusSound.value   = soundIndex
      persistState(tinnitusOn = true, tinnitusSound = soundIndex)
      pushForegroundNotification()

      // Ocean waves (index 2) — play the bundled file via MediaPlayer, looping
      val isOceanSound = soundIndex == 2
      if (isOceanSound) {
         val player = MediaPlayer.create(this, R.raw.ocean_waves)
         player.isLooping = true
         player.setVolume(volume, volume)
         player.start()
         oceanMediaPlayer = player
         return
      }

      // All other sounds — generate PCM samples in real time with AudioTrack
      tinnitusJob = serviceScope.launch {
         val sampleRate = 44100
         val bufSize    = sampleRate / 10

         val track = AudioTrack(
            AudioManager.STREAM_MUSIC, sampleRate,
            AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT,
            bufSize * 4, AudioTrack.MODE_STREAM
         )
         track.play()

         val generator = TinnitusGenerator(soundIndex, volume, sampleRate)
         val buf = ShortArray(bufSize * 2) // interleaved stereo frames
         while (isActive) {
            // Generate one buffer of the selected noise type, same sample in both ears
            for (frame in 0 until bufSize) {
               val sample = generator.next()
               buf[frame * 2]     = sample
               buf[frame * 2 + 1] = sample
            }
            track.write(buf, 0, buf.size)
         }

         track.stop()
         track.release()
      }
   }


   private fun releaseOceanPlayer() {
      oceanMediaPlayer?.stop()
      oceanMediaPlayer?.release()
      oceanMediaPlayer = null
   }


   private fun handleStopTinnitus() {
      tinnitusJob?.cancel()
      tinnitusJob = null
      releaseOceanPlayer()
      _tinnitusPlaying.value = false
      persistState(tinnitusOn = false)
      notifyWidgets()

      val isNothingRunning = !_ampOn.value
      if (isNothingRunning) stopSelf() else pushForegroundNotification()
   }


   private fun handleStartAmp(gain: Float) {
      ampJob?.cancel()
      _ampOn.value = true
      persistState(ampOn = true)
      pushForegroundNotification()

      ampJob = serviceScope.launch {
         val sampleRate = 44100
         val minBufSize = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
         )
         val bufSize = minBufSize * 4

         val recorder = AudioRecord(
            MediaRecorder.AudioSource.MIC, sampleRate,
            AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufSize
         )
         val player = AudioTrack(
            AudioManager.STREAM_MUSIC, sampleRate,
            AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT,
            bufSize, AudioTrack.MODE_STREAM
         )
         recorder.startRecording()
         player.play()

         val buf = ShortArray(bufSize / 2)
         while (isActive) {
            val readCount = recorder.read(buf, 0, buf.size)
            if (readCount > 0) {
               // Calculate RMS for dB meter display
               val sumOfSquares     = buf.take(readCount).sumOf { sample -> sample.toLong() * sample }
               val rmsValue         = sqrt(sumOfSquares.toDouble() / readCount).toInt()
               val dbValueForMeter  = (20 * log10(rmsValue.toDouble().coerceAtLeast(1.0) / 32768.0) + 90)
                  .toInt().coerceIn(0, 120)

               withContext(Dispatchers.Main) { _ampDbLevel.value = dbValueForMeter }

               // Apply gain amplification
               val amplificationFactor = gain * 3 + 1
               val amplified = ShortArray(readCount) { i ->
                  (buf[i] * amplificationFactor).toInt()
                     .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                     .toShort()
               }
               player.write(amplified, 0, readCount)
            }
         }

         recorder.stop(); recorder.release()
         player.stop(); player.release()
         withContext(Dispatchers.Main) { _ampDbLevel.value = 0 }
      }
   }


   private fun handleStopAmp() {
      ampJob?.cancel()
      ampJob       = null
      _ampOn.value = false
      persistState(ampOn = false)
      notifyWidgets()

      val isNothingRunning = !_tinnitusPlaying.value
      if (isNothingRunning) stopSelf() else pushForegroundNotification()
   }


   /** Stateful noise source — keeps filter state and time across buffers so sounds are continuous. */
   private class TinnitusGenerator(
      private val soundIndex: Int,
      private val volume: Float,
      private val sampleRate: Int
   ) {
      private var sampleCount = 0L

      // Pink noise filter state (Paul Kellet's economy filter)
      private var p0 = 0.0; private var p1 = 0.0; private var p2 = 0.0

      // Low-pass state for ocean / rain
      private var lowPass = 0.0
      private var dropLevel = 0.0

      private fun white() = Random.nextDouble(-1.0, 1.0)

      fun next(): Short {
         val value = when (soundIndex) {
            // White noise — equal energy across all frequencies
            0 -> white() * 0.5

            // Pink noise — energy falls with frequency for a warmer, softer feel
            1 -> {
               val w = white()
               p0 = 0.99765 * p0 + w * 0.0990460
               p1 = 0.96300 * p1 + w * 0.2965164
               p2 = 0.57000 * p2 + w * 1.0526913
               (p0 + p1 + p2 + w * 0.1848) * 0.2
            }

            // Ocean waves — low-passed noise swelling in and out every ~8 seconds
            2 -> {
               val timeSec = sampleCount.toDouble() / sampleRate
               val swell = 0.15 + 0.85 * (0.5 - 0.5 * cos(2 * PI * timeSec / 8.0))
               lowPass += 0.04 * (white() - lowPass)
               lowPass * swell * 4.0
            }

            // Rainfall — soft hiss plus random droplets
            else -> {
               lowPass += 0.35 * (white() - lowPass)
               if (Random.nextDouble() < 0.0008) dropLevel = Random.nextDouble(0.3, 0.8)
               dropLevel *= 0.995
               lowPass * 0.35 + white() * dropLevel * 0.4
            }
         }
         sampleCount++
         return (value * volume * Short.MAX_VALUE).toInt()
            .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
      }
   }


   override fun onDestroy() {
      serviceScope.cancel()
      releaseOceanPlayer()
      _tinnitusPlaying.value = false
      _ampOn.value           = false
      _ampDbLevel.value      = 0
      persistState(tinnitusOn = false, ampOn = false)
      super.onDestroy()
   }


   private fun createNotificationChannel() {
      val channel = NotificationChannel(
         CHANNEL_ID,
         "Audio Service",
         NotificationManager.IMPORTANCE_LOW
      ).apply {
         description = "Keeps tinnitus relief and sound amplifier running in background"
         setSound(null, null)
      }
      getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
   }


   private fun pushForegroundNotification() {
      val activeFeatures = buildList {
         if (_tinnitusPlaying.value) add("Tinnitus Relief")
         if (_ampOn.value)           add("Sound Amplifier")
      }.joinToString(" · ")

      val openAppIntent = Intent(this, MainActivity::class.java).apply {
         flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
      }
      val openAppPendingIntent = PendingIntent.getActivity(
         this, 0, openAppIntent,
         PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
      )

      val notification = NotificationCompat.Builder(this, CHANNEL_ID)
         .setSmallIcon(android.R.drawable.ic_lock_silent_mode_off)
         .setContentTitle("HearinGuru")
         .setContentText("$activeFeatures active")
         .setContentIntent(openAppPendingIntent)
         .setOngoing(true)
         .setSilent(true)
         .build()

      // Microphone type only while the amplifier runs — Android 14+ throws if mic permission isn't granted
      val foregroundServiceType =
         if (_ampOn.value) ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
         else ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK

      ServiceCompat.startForeground(this, NOTIF_ID, notification, foregroundServiceType)
   }


   private fun persistState(
      tinnitusOn: Boolean?    = null,
      tinnitusSound: Int?     = null,
      ampOn: Boolean?         = null
   ) {
      prefs.edit().apply {
         tinnitusOn?.let    { putBoolean(PREF_TINNITUS_ON,    it) }
         tinnitusSound?.let { putInt(PREF_TINNITUS_SOUND,     it) }
         ampOn?.let         { putBoolean(PREF_AMP_ON,         it) }
      }.apply()
   }


   private fun notifyWidgets() {
      // Widgets observe shared prefs on next update; request an update
      val intentForTinnitusWidgets = Intent(this, com.hearinguru.app.widget.TinnitusWidgetReceiver::class.java).apply {
         action = android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE
      }
      sendBroadcast(intentForTinnitusWidgets)

      val intentForAmpWidgets = Intent(this, com.hearinguru.app.widget.AmplifierWidgetReceiver::class.java).apply {
         action = android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE
      }
      sendBroadcast(intentForAmpWidgets)
   }
}
