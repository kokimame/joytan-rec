package com.joytan.rec.data

/**
 * 現在の状態
 */
enum class QRecStatus {
    /**
     * During initialization
     */
    INIT,
    /**
     * Initialization completed (1st time)
     */
    READY_FIRST,
    /**
     * Initialization completed (2nd time)
     */
    READY,
    /**
     * Recording stopped
     */
    STOP,
    /**
     * Delete while recording
     */
    DELETE_RECORDING,

    /**
     * Delete while playing
     */
    DELETE_PLAYING,
    /**
     * Starting to record
     */
    STARTING_RECORD,
    /**
     * While recording
     */
    RECORDING,
    /**
     * Stopping to record
     */
    STOPPING_RECORD,
    /**
     * While playing
     */
    PLAYING,

    /**
     * While playing is stopped
     */
    STOPPING_PLAYING,

    /**
     * While sharing to the server
     */
    SHARE
}
