package com.amora.companion.feature.pcspeaker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import java.net.DatagramPacket
import java.net.DatagramSocket
import kotlin.concurrent.thread

private const val TAG = "PcSpeakerService"

/** UDP port the phone listens on for audio data from the PC */
const val PC_SPEAKER_PORT = 5150

/** UDP port for pairing handshake (PC sends HELLO, phone replies ACK) */
const val PC_PAIR_PORT = 5149

/** Handshake message prefixes */
private const val HELLO_PREFIX = "AMORA_HELLO:"
private const val ACK_PREFIX   = "AMORA_ACK:"

/** PCM format matching the Python sender: 48 kHz, stereo, 16-bit */
private const val SAMPLE_RATE = 48_000
private const val CHANNELS    = AudioFormat.CHANNEL_OUT_STEREO
private const val ENCODING    = AudioFormat.ENCODING_PCM_16BIT

/** Maximum UDP receive packet size (8 KB buffer handles variable frame sizes cleanly) */
private const val MAX_PACKET_SIZE = 8192

class PcSpeakerService : android.app.Service() {

    private var audioSocket: DatagramSocket? = null
    private var pairSocket:  DatagramSocket? = null
    private var audioTrack:  AudioTrack?     = null
    private var running      = false
    private var pairListening = false

    companion object {
        const val ACTION_START      = "com.amora.pc_speaker.START"
        const val ACTION_STOP       = "com.amora.pc_speaker.STOP"
        const val ACTION_START_PAIR = "com.amora.pc_speaker.START_PAIR"

        var isRunning: Boolean = false
            private set

        /** Pair code shown on-screen; PC must send this to connect */
        var currentPairCode: String = generatePairCode()
            private set

        fun regeneratePairCode() {
            currentPairCode = generatePairCode()
        }

        private fun generatePairCode(): String =
            (1000..9999).random().toString()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopAll()
                return START_NOT_STICKY
            }
            ACTION_START_PAIR -> {
                if (!pairListening) {
                    startForegroundNotification(pairing = true)
                    startPairListener()
                }
            }
            ACTION_START -> {
                if (!running) {
                    startForegroundNotification(pairing = false)
                    startAudioStreaming()
                }
            }
        }
        return START_STICKY
    }

    private fun startForegroundNotification(pairing: Boolean) {
        val channelId = "amora_pc_speaker_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(
                channelId, "PC Speaker", NotificationManager.IMPORTANCE_LOW
            )
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(ch)
        }
        val (title, text) = if (pairing)
            "🔊 PC Speaker — Waiting to Pair" to "Pair code: $currentPairCode  |  Port $PC_PAIR_PORT"
        else
            "🔊 PC Speaker Active" to "Receiving audio on port $PC_SPEAKER_PORT"

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
        startForeground(3001, notification)
    }

    private fun startPairListener() {
        pairListening = true
        thread(name = "pc-speaker-pair", isDaemon = true) {
            try {
                pairSocket = DatagramSocket(PC_PAIR_PORT)
                pairSocket!!.soTimeout = 0
                val buffer = ByteArray(64)
                val packet = DatagramPacket(buffer, buffer.size)

                Log.i(TAG, "Pair listener ready on UDP :$PC_PAIR_PORT code=$currentPairCode")

                while (pairListening) {
                    try {
                        pairSocket!!.receive(packet)
                        val msg = String(packet.data, 0, packet.length).trim()

                        if (msg.startsWith(HELLO_PREFIX)) {
                            val receivedCode = msg.removePrefix(HELLO_PREFIX).trim()
                            if (receivedCode == currentPairCode) {
                                val ack = "$ACK_PREFIX$currentPairCode".toByteArray()
                                val ackPacket = DatagramPacket(
                                    ack, ack.size,
                                    packet.address, packet.port
                                )
                                pairSocket!!.send(ackPacket)

                                pairListening = false
                                pairSocket?.close()

                                startForegroundNotification(pairing = false)
                                startAudioStreaming()
                            } else {
                                val reject = "AMORA_REJECT:wrong_code".toByteArray()
                                pairSocket!!.send(
                                    DatagramPacket(reject, reject.size, packet.address, packet.port)
                                )
                            }
                        }
                    } catch (e: Exception) {
                        if (pairListening) Log.w(TAG, "Pair socket error: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Pair listener failed to bind port $PC_PAIR_PORT", e)
            }
        }
    }

    private fun startAudioStreaming() {
        if (running) return
        running  = true
        isRunning = true

        // A larger buffer with default (not low-latency) performance mode — the previous
        // near-minimum low-latency buffer had almost no tolerance for Wi-Fi/UDP jitter,
        // so any packet arriving even a few ms late underran AudioTrack and produced the
        // "cut and play, cut and play" stutter. Trading ~150-200ms of extra latency for
        // a jitter buffer fixes that — imperceptible for music/video played through a
        // phone speaker in the same room.
        val hwMinBufSize = AudioTrack.getMinBufferSize(SAMPLE_RATE, CHANNELS, ENCODING)
        val bufSize = maxOf(hwMinBufSize * 4, 32_768)

        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(CHANNELS)
                    .setEncoding(ENCODING)
                    .build()
            )
            .setBufferSizeInBytes(bufSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
            .also { it.play() }

        // Jitter buffer: the UDP receiver thread only enqueues packets; a separate
        // player thread drains the queue into AudioTrack at a steady pace. This decouples
        // network arrival timing (bursty/jittery over Wi-Fi) from playback timing.
        val jitterQueue = java.util.concurrent.LinkedBlockingQueue<ByteArray>()
        // ~150ms of priming before playback starts, so small early jitter doesn't underrun.
        val primeTargetPackets = 15
        val primedFlag = java.util.concurrent.atomic.AtomicBoolean(false)
        // Cap so a sustained network slowdown doesn't grow latency forever.
        val maxQueuedPackets = 60

        thread(name = "pc-speaker-player", isDaemon = true) {
            try {
                while (running || jitterQueue.isNotEmpty()) {
                    if (!primedFlag.get()) {
                        if (jitterQueue.size < primeTargetPackets && running) {
                            Thread.sleep(5)
                            continue
                        }
                        primedFlag.set(true)
                    }
                    val chunk = jitterQueue.poll(200, java.util.concurrent.TimeUnit.MILLISECONDS)
                    if (chunk != null) {
                        audioTrack?.write(chunk, 0, chunk.size)
                    } else if (running) {
                        // Queue ran dry — re-prime instead of trickling tiny writes that would glitch anyway.
                        primedFlag.set(false)
                    }
                }
            } catch (e: Exception) {
                if (running) Log.w(TAG, "Player thread error: ${e.message}")
            }
        }

        thread(name = "pc-speaker-udp", isDaemon = true) {
            try {
                audioSocket = DatagramSocket(PC_SPEAKER_PORT)
                // Short timeout so the receive loop can check `running` regularly,
                // but a timeout by itself no longer tears down the whole session —
                // the PC script often pauses (e.g. video paused) without disconnecting.
                audioSocket!!.soTimeout = 3500
                // Low latency socket buffer (8 KB = ~40 ms buffer, prevents kernel queue buildup)
                audioSocket!!.receiveBufferSize = 8192

                val buffer = ByteArray(MAX_PACKET_SIZE)
                val packet = DatagramPacket(buffer, buffer.size)

                var consecutiveTimeouts = 0
                // Only treat this as a real disconnect after ~2 minutes of total silence.
                val maxConsecutiveTimeouts = 34

                Log.i(TAG, "🔊 Audio streaming started on UDP :$PC_SPEAKER_PORT (Jitter-Buffered Mode)")
                while (running) {
                    try {
                        audioSocket!!.receive(packet)
                        consecutiveTimeouts = 0
                        if (packet.length > 0) {
                            // Copy — the shared `buffer` gets overwritten by the next receive().
                            val copy = packet.data.copyOfRange(0, packet.length)
                            jitterQueue.offer(copy)
                            while (jitterQueue.size > maxQueuedPackets) {
                                jitterQueue.poll() // drop oldest if the network can't keep up
                            }
                        }
                    } catch (e: java.net.SocketTimeoutException) {
                        consecutiveTimeouts++
                        if (consecutiveTimeouts >= maxConsecutiveTimeouts) {
                            Log.i(TAG, "PC stream idle for ~2 min — auto-resetting PC Speaker service")
                            stopAll()
                            break
                        }
                        // else: just a lull in audio (e.g. video paused) — keep waiting.
                    } catch (e: Exception) {
                        if (running) Log.w(TAG, "Packet receive error: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Audio socket error on port $PC_SPEAKER_PORT", e)
            } finally {
                audioSocket?.close()
            }
        }
    }

    private fun stopAll() {
        pairListening = false
        running       = false
        isRunning     = false

        try { pairSocket?.close()  } catch (_: Exception) {}
        try { audioSocket?.close() } catch (_: Exception) {}
        try {
            audioTrack?.stop()
            audioTrack?.flush()
            audioTrack?.release()
            audioTrack = null
        } catch (_: Exception) {}

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        Log.i(TAG, "PC Speaker stopped")
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAll()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
