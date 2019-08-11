package jp.bellware.echo.main

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
//import android.databinding.ObservableField
import android.os.IBinder
import android.view.GestureDetector
import android.view.View
import androidx.databinding.ObservableField
import jp.bellware.echo.R
import jp.bellware.util.BWU

/**
 * メイン画面のViewModel
 * @param context
 * @param listener リスナ
 */
class MainViewModel(private val context: Context, private val listener: Listener) {

    interface Listener {

        /**
         * 録音結果の削除が発生した時のイベント
         */
        fun onDeleteRecord()

        /**
         * 録音開始のイベント
         */
        fun onStartRecord()

        /**
         * 録音終了のイベント
         */
        fun onStopRecord()


        /**
         * 視覚的ボリュームを更新する
         */
        fun onUpdateVolume(volume: Float)

        /**
         * 警告メッセージを表示する
         */
        fun onShowWarningMessage(resId: Int)

        fun onShowProgress(message: String)

        fun onDismissProgress()

        fun onScriptNavigation(direction: String)

        fun getAudioOutputPath(): String
    }

    /**
     * ステータス表示
     */
    val statusFrameVisibility = ObservableField<Int>()

    /**
     * 録音ボタン表示
     */
    val recordVisibility = ObservableField<Int>()

    /**
     * 再生ボタン表示
     */
    val playVisibility = ObservableField<Int>()

    /**
     * 再再生ボタン表示
     */
    val replayVisibility = ObservableField<Int>()

    /**
     * 停止ボタン表示
     */
    val stopVisibility = ObservableField<Int>()

    /**
     * Display share button
     */
    val shareVisibility = ObservableField<Int>()

    /**
     * 削除ボタン表示
     */
    val deleteVisibility = ObservableField<Int>()

    /**
     * ステータス表示
     */
    val statusImageSrc = ObservableField<Int>(R.drawable.microphone_48dp)

    /**
     * メインサービス
     */
    private var service: MainService? = null

    private val mainCB = object : MainServiceCallback {
        override fun onUpdateStatus(animation: Boolean, status: QRecStatus) {
            BWU.log("MainActivity#onUpdateStatus $status")
            //ステータスビュー
            if (status == QRecStatus.INIT) {
                //初期化中
                //何も表示しない
                statusFrameVisibility.set(View.INVISIBLE)
                recordVisibility.set(View.INVISIBLE)
                playVisibility.set(View.INVISIBLE)
                replayVisibility.set(View.INVISIBLE)
                stopVisibility.set(View.INVISIBLE)
                shareVisibility.set(View.INVISIBLE)
                deleteVisibility.set(View.INVISIBLE)
            } else if (status == QRecStatus.DELETE_RECORDING || status == QRecStatus.DELETE_PLAYING) {
                //アニメーション開始
                if (animation) {
                    listener.onDeleteRecord()
                } else {
                    statusFrameVisibility.set(View.INVISIBLE)
                    recordVisibility.set(View.VISIBLE)
                    playVisibility.set(View.INVISIBLE)
                    replayVisibility.set(View.INVISIBLE)
                    stopVisibility.set(View.INVISIBLE)
                    shareVisibility.set(View.INVISIBLE)
                    deleteVisibility.set(View.INVISIBLE)
                }
            } else if (status == QRecStatus.READY_FIRST || status == QRecStatus.READY) {
                //録音ボタンにする
                statusFrameVisibility.set(View.INVISIBLE)
                recordVisibility.set(View.VISIBLE)
                playVisibility.set(View.INVISIBLE)
                replayVisibility.set(View.INVISIBLE)
                stopVisibility.set(View.INVISIBLE)
                shareVisibility.set(View.INVISIBLE)
                deleteVisibility.set(View.INVISIBLE)
            } else if (status == QRecStatus.STOP) {
                //停止状態
                statusFrameVisibility.set(View.VISIBLE)
                statusImageSrc.set(R.drawable.speaker_48dp)
                recordVisibility.set(View.VISIBLE)
                playVisibility.set(View.INVISIBLE)
                replayVisibility.set(View.VISIBLE)
                stopVisibility.set(View.VISIBLE)
                shareVisibility.set(View.VISIBLE)
                deleteVisibility.set(View.VISIBLE)
            } else if (status == QRecStatus.STARTING_RECORD) {
                //スピーカーアイコン表示
                statusImageSrc.set(R.drawable.microphone_48dp)
                //再生ボタンにする
                recordVisibility.set(View.INVISIBLE)
                playVisibility.set(View.VISIBLE)
                //サブコントロールは非表示
                replayVisibility.set(View.INVISIBLE)
                stopVisibility.set(View.INVISIBLE)
                shareVisibility.set(View.INVISIBLE)
                if (animation) {
                    listener.onStartRecord()
                } else {
                    statusFrameVisibility.set(View.VISIBLE)
                    deleteVisibility.set(View.VISIBLE)
                }

            } else if (status == QRecStatus.RECORDING) {
                statusFrameVisibility.set(View.VISIBLE)
                statusImageSrc.set(R.drawable.microphone_48dp)
                recordVisibility.set(View.INVISIBLE)
                playVisibility.set(View.VISIBLE)
                replayVisibility.set(View.INVISIBLE)
                stopVisibility.set(View.INVISIBLE)
                shareVisibility.set(View.INVISIBLE)
                deleteVisibility.set(View.VISIBLE)
            } else if (status == QRecStatus.STOPPING_RECORD) {
                //スピーカーを表示
                statusImageSrc.set(R.drawable.speaker_48dp)
                //録音ボタンにする
                recordVisibility.set(View.VISIBLE)
                playVisibility.set(View.INVISIBLE)
                if (animation) {
                    listener.onStopRecord()
                } else {
                    statusFrameVisibility.set(View.VISIBLE)
                    replayVisibility.set(View.VISIBLE)
                    stopVisibility.set(View.VISIBLE)
                    shareVisibility.set(View.VISIBLE)
                }
            } else if (status == QRecStatus.PLAYING || status == QRecStatus.SHARE) {
                //再生中
                statusFrameVisibility.set(View.VISIBLE)
                statusImageSrc.set(R.drawable.speaker_48dp)
                recordVisibility.set(View.VISIBLE)
                playVisibility.set(View.INVISIBLE)
                replayVisibility.set(View.VISIBLE)
                stopVisibility.set(View.VISIBLE)
                shareVisibility.set(View.VISIBLE)
                deleteVisibility.set(View.VISIBLE)
            }
        }

        override fun onUpdateVolume(volume: Float) {
            listener.onUpdateVolume(volume)
        }

        override fun onScriptNavigation(direction: String) {
            listener.onScriptNavigation(direction)
        }

        override fun onShowWarningMessage(resId: Int) {
            listener.onShowWarningMessage(resId)
        }

        override fun onShowProgress(message: String) {
            listener.onShowProgress(message)
        }

        override fun onDismissProgress() {
            listener.onDismissProgress()
        }

        override fun getAudioOutputPath(): String {
            return listener.getAudioOutputPath()
        }

    }


    private val conn = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
            service = (binder as MainService.MainServiceBinder).service
            service?.setCallback(mainCB)
            service?.onSettingUpdated()
        }

        override fun onServiceDisconnected(name: ComponentName) {

        }
    }

    /**
     * ActivityのonCreateから呼ばれる
     */
    fun onCreate() {

    }

    /**
     * ActivityのonStartから呼ばれる
     */
    fun onStart() {
        //録音サービスを開始
        val intent = Intent(context, MainService::class.java)
        context.startService(intent)
        context.bindService(intent, conn, 0)
    }

    /**
     * ActivityのonStopから呼ばれる
     */
    fun onStop() {
        service?.setCallback(null)
        service = null
        context.unbindService(conn)
    }

    /**
     * ActivityのonDestroyから呼ばれる
     */
    fun onDestroy() {
    }


    /**
     * On Back button pressed
     */
    fun onBackPressed(): Boolean {
        var result = service?.onBackPressed()
        return result == true
    }

    /**
     * On Setting button pressed
     */
    fun onSettingUpdate() {
        service?.onSettingUpdated()
    }

    /**
     * On Record button clicked
     */
    fun onRecordClicked(view : View) {
        service?.onRecord()
    }

    /**
     * On Play button clicked
     */
    fun onPlayClicked(view : View) {
        service?.onPlay()
    }

    /**
     * On Replay button clicked
     */
    fun onReplayClicked(view : View) {
        service?.onReplay()
    }


    /**
     * On Share button clicked
     */
    fun onShareClicked(view : View) {
        service?.onShare()
    }


    /**
     * On Delete button clicked
     */
    fun onDeleteClicked(view : View) {
        service?.onDelete()
    }

    /*
     * On Left button clicked
     */
    fun onLeftClicked() {
        service?.onLeft()
    }
    fun onRightClicked() {
        service?.onRight()
    }

    fun onLeftClicked(view : View) {
        service?.onLeft()
    }
    fun onRightClicked(view : View) {
        service?.onRight()
    }

}