package jp.bellware.echo.main

/**
 * Callback from MainService
 */
interface MainServiceCallback {
    /**
     * Update animation
     * @param animation アニメーションフラグ。バックグランドからの復帰による状態再現の場合はfalseとなる。
     * @param status 状態
     */
    fun onUpdateStatus(animation: Boolean, status: QRecStatus)

    /**
     * Update visual volume
     * @param volume
     */
    fun onUpdateVolume(volume: Float)

    /**
     * Navigate content in main_script (left or right)
     * @param direction (left or right, otherwise raise exception)
     */
    fun onScriptNavigation(direction: String)

    fun onGetAudioPath(): String

    /**
     * Warning message
     * @param resId Message Id
     */
    fun onShowWarningMessage(resId: Int)

    fun onShowProgress(message: String)

    fun onDismissProgress()

    fun onShowGrid()

    fun onCallSetting()
}
