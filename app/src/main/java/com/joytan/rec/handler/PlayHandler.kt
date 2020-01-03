package com.joytan.rec.handler

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.*
import android.os.Process

import java.util.Arrays

import com.joytan.rec.data.QRecStorage
import com.joytan.rec.filter.FadeOut
import com.joytan.rec.filter.FirstCut
import com.joytan.rec.filter.PacketConverter
import com.joytan.rec.filter.PlayVisualVolumeProcessor

/**
 * 音声再生担当
 */
class PlayHandler(private val storage: QRecStorage) {
    private var track: AudioTrack? = null

    private var thread: Thread? = null

    private var playing = false

    private val vvp = PlayVisualVolumeProcessor()

    private val handler = Handler()

    private var fo: FadeOut? = null

    private val fc = FirstCut(RecordHandler.FC)

    /**
     * パケットの変換担当
     */
    private val converter = PacketConverter()


    /**
     * 再生パケットインデックス
     */
    private var index: Int = 0

    val visualVolume: Float
        @Synchronized get() = vvp.getVolume()

    fun onResume() {}

    fun onPause() {
        stop()
    }

    /**
     * 再生する
     */
    fun play(onEndListener: () -> Unit) {
        stop()
        if (storage.length == 0) {
            //長さ0の時はすぐに終わる
            onEndListener()
            return
        }
        track = AudioTrack(AudioManager.STREAM_MUSIC,
                RecordHandler.SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT,
                storage.packetSize * 2, AudioTrack.MODE_STREAM)
        val ltrack = track
        playing = true

        //最初のパケットは効果音が入っていることがあるので捨てる
        // index = 1
        index = 0

        thread = Thread(Runnable {
            //これがないと音が途切れる
            Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)

            fo = FadeOut(storage.length, RecordHandler.SAMPLE_RATE * 3 / 10)
            fc.reset()
            if (ltrack != null) {
                /*
                TODO: ERROR
                E/AndroidRuntime: FATAL EXCEPTION: Thread-52
                Process: jp.bellware.echo, PID: 16106
                java.lang.IllegalStateException: play() called on uninitialized AudioTrack.
                    at android.media.AudioTrack.play(AudioTrack.java:1755)
                    at jp.bellware.echo.main.PlayHandler$play$1.run(PlayHandler.kt:80)
                    at java.lang.Thread.run(Thread.java:776)
                 */
                try {
                    ltrack.play()

                    try {
                        Thread.sleep((storage.packetSize * 1000 / 44100).toLong())
                    } catch (e: InterruptedException) {
                    }

                    while (true) {
                        val packet = pullPacket(onEndListener)
                        if (packet != null) {
                            filter(packet)
                            val sd = converter.convert(packet)

                            val result = ltrack.write(sd, 0, sd.size)

                            if (result < 0)
                                break
                        } else {
                            break
                        }
                    }
                } catch (e: IllegalStateException) {
                }
            }

        })
        thread?.start()
    }

    private fun pullPacket(onEndListener: () -> Unit): FloatArray? {
        if (playing) {
            val packet = storage[index]
            ++index
            if (packet == null) {
                //終端
                if (onEndListener != null) {
                    handler.post { onEndListener() }
                }
                return null
            } else {
                return Arrays.copyOf(packet, packet.size)
            }
        } else
            return null
    }


    /**
     * フィルターをかける
     *
     * @param packet
     */
    @Synchronized
    private fun filter(packet: FloatArray) {
        for (i in packet.indices) {
            //ボリューム調整
            var s = packet[i]
            s /= storage.gain
            //フェードアウト
            val lfo = fo
            if(lfo != null)
                s = lfo.filter(s)
            //視覚的ボリューム
            vvp.add(fc.filter(s))
            //置き換え
            packet[i] = s
        }

    }

    /**
     * 再生を終了する
     */
    fun stop() {
        val lthread = thread
        val ltrack = track
        if (lthread != null && ltrack != null) {
            playing = false
            try {
                lthread.join()
            } catch (e: InterruptedException) {
            }

            ltrack.release()
            track = null
        }
    }


}
