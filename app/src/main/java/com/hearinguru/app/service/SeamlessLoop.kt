package com.hearinguru.app.service

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import java.io.ByteArrayOutputStream
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin


/** A decoded sound ready for endless playback: interleaved 16-bit stereo PCM. */
class SeamlessLoop(val pcm: ShortArray, val sampleRate: Int) {
   val frameCount get() = pcm.size / 2
}


/**
 * Decodes a bundled audio file and turns it into a loop with no audible seam.
 *
 * Android's MediaPlayer.isLooping leaves a short gap/click every time a clip restarts,
 * which is very noticeable on 10–20 s ambience clips. Instead we decode once and blend the
 * clip's tail into its head (equal-power crossfade), so the end flows straight into the start.
 */
object SeamlessLoopLoader {

   private const val CROSSFADE_SECONDS = 1.5

   private val cache = mutableMapOf<Int, SeamlessLoop>()


   @Synchronized
   fun load(context: Context, rawResId: Int): SeamlessLoop =
      cache.getOrPut(rawResId) { buildLoop(decodeToStereo(context, rawResId)) }


   private fun buildLoop(decoded: SeamlessLoop): SeamlessLoop {
      val pcm = decoded.pcm
      val totalFrames = decoded.frameCount
      val fadeFrames = minOf((CROSSFADE_SECONDS * decoded.sampleRate).toInt(), totalFrames / 4)
      if (fadeFrames <= 0) return decoded

      // Loop body = everything except the tail; the tail is faded into the first frames instead
      val loopFrames = totalFrames - fadeFrames
      val loop = pcm.copyOf(loopFrames * 2)
      for (frame in 0 until fadeFrames) {
         val progress = frame.toDouble() / fadeFrames
         val fadeIn  = sin(progress * PI / 2)  // equal-power curves keep loudness steady
         val fadeOut = cos(progress * PI / 2)
         for (ch in 0..1) {
            val head = pcm[frame * 2 + ch]
            val tail = pcm[(loopFrames + frame) * 2 + ch]
            loop[frame * 2 + ch] = (head * fadeIn + tail * fadeOut).toInt()
               .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
         }
      }
      return SeamlessLoop(loop, decoded.sampleRate)
   }


   /** Decodes the whole file to interleaved stereo 16-bit PCM using the platform codecs. */
   private fun decodeToStereo(context: Context, rawResId: Int): SeamlessLoop {
      val extractor = MediaExtractor()
      context.resources.openRawResourceFd(rawResId).use { fd ->
         extractor.setDataSource(fd.fileDescriptor, fd.startOffset, fd.length)
      }

      val trackIndex = (0 until extractor.trackCount).first { index ->
         extractor.getTrackFormat(index).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
      }
      extractor.selectTrack(trackIndex)
      val inputFormat = extractor.getTrackFormat(trackIndex)

      val codec = MediaCodec.createDecoderByType(inputFormat.getString(MediaFormat.KEY_MIME)!!)
      codec.configure(inputFormat, null, null, 0)
      codec.start()

      var sampleRate   = inputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
      var channelCount = inputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
      val output = ByteArrayOutputStream()
      val info = MediaCodec.BufferInfo()
      var inputDone = false
      var outputDone = false

      try {
         while (!outputDone) {
            if (!inputDone) {
               val inIndex = codec.dequeueInputBuffer(10_000)
               if (inIndex >= 0) {
                  val size = extractor.readSampleData(codec.getInputBuffer(inIndex)!!, 0)
                  if (size < 0) {
                     codec.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                     inputDone = true
                  } else {
                     codec.queueInputBuffer(inIndex, 0, size, extractor.sampleTime, 0)
                     extractor.advance()
                  }
               }
            }

            val outIndex = codec.dequeueOutputBuffer(info, 10_000)
            when {
               outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                  sampleRate   = codec.outputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                  channelCount = codec.outputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
               }
               outIndex >= 0 -> {
                  val buffer = codec.getOutputBuffer(outIndex)!!
                  val chunk = ByteArray(info.size)
                  buffer.position(info.offset)
                  buffer.get(chunk)
                  output.write(chunk)
                  codec.releaseOutputBuffer(outIndex, false)
                  if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) outputDone = true
               }
            }
         }
      } finally {
         codec.stop()
         codec.release()
         extractor.release()
      }

      val bytes = output.toByteArray()
      val samples = ShortArray(bytes.size / 2)
      java.nio.ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(samples)

      // Normalise to stereo: duplicate mono, keep the first two channels of anything wider
      val frames = samples.size / channelCount
      val stereo = ShortArray(frames * 2)
      for (frame in 0 until frames) {
         val left = samples[frame * channelCount]
         stereo[frame * 2]     = left
         stereo[frame * 2 + 1] = if (channelCount > 1) samples[frame * channelCount + 1] else left
      }
      return SeamlessLoop(stereo, sampleRate)
   }
}
