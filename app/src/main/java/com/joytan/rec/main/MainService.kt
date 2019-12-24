package com.joytan.rec.main

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.idling.CountingIdlingResource
import com.joytan.rec.R
import com.joytan.rec.analytics.AnalyticsHandler
import com.joytan.rec.data.QRecStatus
import com.joytan.rec.data.QRecStorage
import com.joytan.rec.main.handler.*
import com.joytan.rec.setting.SettingFragment
import com.joytan.util.BWU

/**
 * メインサービス。
 * 画面回転や画面の大きさが変わっても録音、再生を継続できるようにサービス化している。
 */
class MainService : Service() {

    private val handler = Handler()

    private val endTask = Runnable {
        BWU.log("MainService#endTask")
        stopSelf()
    }

    private val binder = MainServiceBinder()

    /**
     * Sound effect handler
     */
    private val seh = SoundEffectHandler()

    /**
     * Visual volume handle
     */
    private val vvh = VisualVolumeHandler()

    /**
     * 保存担当
     */
    private val storage = QRecStorage()

    /**
     * Handle recording
     */
    private val record = RecordHandler(storage)

    /**
     * Handle Play
     */
    private val play = PlayHandler(storage)

    /*
     * Handle Share feature
     */
    private val share = ShareHandler(storage)

    /**
     * Handle analytics
     */
    private val ah = AnalyticsHandler()

    /**
     * Espressoの同期待ちを登録する
     */
    private val registry = IdlingRegistry.getInstance()

    /**
     * Espressoの同期待ちカウンター
     */
    private val cir = CountingIdlingResource("MainService")



    /**
     * 録音可能時間
     */
    private val timeLimitTask = Runnable {
        if (record.isIncludeSound) {
            status = QRecStatus.STOPPING_RECORD
        } else {
            seh.delete()
            status = QRecStatus.DELETE_RECORDING
        }
        cb?.onShowWarningMessage(R.string.warning_time_limit)
        update()
    }

    /**
     * 遅延タスク
     */
    private var delayTask: Runnable = Runnable { }

    /**
     * 現在の状態
     */
    private var status = QRecStatus.INIT

    private lateinit var audioPath :String


    /**
     * コールバック
     */
    private var cb: MainServiceCallback? = null

    inner class MainServiceBinder : Binder() {
        val service: MainService
            get() = this@MainService
    }

    override fun onBind(intent: Intent): IBinder? {
        return binder
    }


    override fun onCreate() {
        super.onCreate()
        BWU.log("MainService#onCreate")
        //終了タスクを予約
        handler.postDelayed(endTask, END_TIME.toLong())
        //設定を反映
        onSettingUpdated()
        //視覚的ボリューム
        vvh.onCreate(this, object : VisualVolumeHandler.Callback {
            override fun getRecordVisualVolume(): Float {
                return record.visualVolume
            }

            override fun getPlayVisualVolume(): Float {
                return play.visualVolume
            }

            override fun onUpdateVolume(volume: Float) {
                cb?.onUpdateVolume(volume)
            }
        })
        record.onResume()
        play.onResume()
        share.onResume()
        vvh.onResume()
        //分析
        ah.onCreate(this)
        //Espressoの同期待ち登録
        registry.register(cir)
        //初回更新
        update()
    }

    /**
     * コールバックを設定する
     *
     * @param cb
     */
    fun setCallback(cb: MainServiceCallback?) {
        handler.removeCallbacks(endTask)
        if (cb == null) {
            handler.postDelayed(endTask, END_TIME.toLong())
        } else {
            cb.onUpdateStatus(false, status)
            cb.onUpdateVolume(0f)
        }
        this.cb = cb
    }


    override fun onDestroy() {
        super.onDestroy()
        BWU.log("MainService#onDestroy")
        handler.removeCallbacks(timeLimitTask)
        record.onPause()
        play.onPause()
        status = QRecStatus.READY_FIRST
        vvh.onPause()
        seh.onDestroy()
        handler.removeCallbacks(delayTask)
        //Espressoの同期待ち解除
        registry.unregister(cir)
    }

    /**
     * On Back button pressed
     *
     * @return
     */
    fun onBackPressed(): Boolean {
        if (status == QRecStatus.RECORDING || status == QRecStatus.STOPPING_RECORD) {
            seh.delete()
            status = QRecStatus.DELETE_RECORDING
            update()
            return true
        } else if (status == QRecStatus.STOP ||
                status == QRecStatus.PLAYING ||
                status == QRecStatus.STOPPING_PLAYING) {
            seh.delete()
            status = QRecStatus.DELETE_PLAYING
            update()
            return true
        } else {
            return false
        }
    }

    /*
     * On Setting updated
     */
    fun onSettingUpdated() {
        seh.isEnabled = SettingFragment.isSoundEffect(this)
    }

    /**
     * When Record is requested
     */
    fun onRecord() {
        if (status == QRecStatus.READY_FIRST || status == QRecStatus.READY ||
                status == QRecStatus.STOP || status == QRecStatus.PLAYING) {
            status = QRecStatus.STARTING_RECORD
            update()
        }
    }

    /**
     * When Play is requested
     */
    fun onPlay() {
        if (status == QRecStatus.RECORDING) {
            if (record.isIncludeSound) {
                status = QRecStatus.STOPPING_RECORD
            } else {
                seh.delete()
                status = QRecStatus.DELETE_RECORDING
                cb?.onShowWarningMessage(R.string.warning_no_sound)
            }
            update()
        }
    }

    /**
     * When Replay is requested
     */
    fun onReplay() {
        if (status == QRecStatus.PLAYING || status == QRecStatus.STOP) {
            status = QRecStatus.PLAYING
            update()
        }
    }

    /**
     * When Share is requested
     */
    fun onShare() {
        val maybeNullPath = cb?.onGetAudioPath()
        if(maybeNullPath == null) {
            return
        } else {
            audioPath = maybeNullPath
        }
        status = QRecStatus.SHARE
        cb?.onShowProgress("Sending...")
        update()
    }

    /**
     * When Delete is requested
     */
    fun onDelete() {
        if (status == QRecStatus.RECORDING) {
            seh.delete()
            status = QRecStatus.DELETE_RECORDING
            update()
        } else if (status == QRecStatus.PLAYING || status == QRecStatus.STOP) {
            seh.delete()
            status = QRecStatus.DELETE_PLAYING
            update()
        }
    }

    fun onLeft() {
        onScriptNavigation("left")
    }

    fun onRight() {
        onScriptNavigation("right")
    }

    fun onGrid() {
        cb?.onShowGrid()
    }

    fun onSetting() {
        cb?.onCallSetting()
    }

    /*
     * To navigate script content (left or right)
     */
    fun onScriptNavigation(direction: String) {
        if (status == QRecStatus.RECORDING) {
            seh.delete()
            status = QRecStatus.DELETE_RECORDING
            update()
        } else if (status == QRecStatus.PLAYING || status == QRecStatus.STOP) {
            seh.delete()
            status = QRecStatus.DELETE_PLAYING
            update()
        }
        cb?.onScriptNavigation(direction)
    }

    /**
     * Update according with status
     */
    private fun update() {
        if (status == QRecStatus.INIT) {
            // Load sound effects
            seh.onCreate(this) {
                // Finish initialization
                status = QRecStatus.READY_FIRST
                update()
            }
        } else if (status == QRecStatus.DELETE_RECORDING || status == QRecStatus.DELETE_PLAYING) {
            // Stop audio playing
            play.stop()
            record.stop()
            // Remove a task with time limit
            handler.removeCallbacks(timeLimitTask)
            // Analytics
            ah.sendAction("delete", (storage.length / 44100).toLong())
            //削除→録音可能
            this.delayTask = Runnable {
                status = QRecStatus.READY
                update()
            }
            handler.postDelayed(delayTask, 200)
        } else if (status == QRecStatus.READY_FIRST || status == QRecStatus.READY) {
            play.stop()
            record.stop()
        } else if (status == QRecStatus.STOP) {
            //視覚的ボリューム
            vvh.stop()
        } else if (status == QRecStatus.STARTING_RECORD) {
            cir.increment()
            //音を鳴らす
//            seh.start()
            //再生終了
            play.stop()
            //視覚的ボリュームをリセット
            vvh.reset()
            //効果音が鳴り終わったら録音開始
            delayTask = Runnable {
                status = QRecStatus.RECORDING
                update()
                cir.decrement()
            }
            handler.postDelayed(delayTask, 500)
            //イベントを送る
            ah.sendAction("record")
        } else if (status == QRecStatus.RECORDING) {
            //録音開始
            record.start()
            //タイムリミットタスク予約
            handler.postDelayed(timeLimitTask, TIME_LIMIT.toLong())
            //視覚的ボリューム
            vvh.record()
        } else if (status == QRecStatus.STOPPING_RECORD) {
            //録音終了
            record.stop()
            //タイムリミットタスク解除
            handler.removeCallbacks(timeLimitTask)
            //終了SE
            seh.play()
            //視覚的ボリュームをリセット
            vvh.reset()
            //効果音が鳴り終わったら再生開始
            delayTask = Runnable {
                status = QRecStatus.PLAYING
                update()
            }
            handler.postDelayed(delayTask, 550)
        } else if (status == QRecStatus.PLAYING) {
            //最後まで再生したイベント設定
            play.play {
                status = QRecStatus.STOPPING_PLAYING
                update()
            }
            //視覚的ボリュームをリセット
            vvh.reset()
            //視覚的ボリューム
            vvh.play()
            //Analytics
            ah.sendAction("play", (storage.length / 44100).toLong())
        } else if (status == QRecStatus.SHARE) {
            share.share (fileName = audioPath) {}
            status = QRecStatus.DELETE_PLAYING
            update()
            ah.sendAction("share")
        } else if (status == QRecStatus.STOPPING_PLAYING) {
            vvh.reset()
            play.stop()
            status = QRecStatus.STOP
            update()
        }
        //状態更新をActivityに通知
        cb?.onUpdateStatus(true, status)
    }

    companion object {

        /**
         * サービス生存時間は接続Activity消失から3秒後
         */
        private val END_TIME = 2000

        /**
         * 録音時間制限は 10sec
         */
        private val TIME_LIMIT = 10 * 1000
    }
}
