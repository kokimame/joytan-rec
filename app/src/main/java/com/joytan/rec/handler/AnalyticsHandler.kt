package com.joytan.rec.handler

import android.content.Context
import android.os.Bundle

import com.google.firebase.analytics.FirebaseAnalytics;

/**
 * Handling Firebase Analytics
 */
class AnalyticsHandler {

    /**
     * Firebase Analytics
     */
    private lateinit var fa : FirebaseAnalytics


    fun onCreate(context: Context) {
        fa = FirebaseAnalytics.getInstance(context)
    }

    fun onResume() {}

    /**
     * Send an event
     *
     * @param action
     */
    fun sendAction(action: String) {
        val bundle = Bundle()
        fa.logEvent(action, bundle)
    }

    /**
     * Send an event with a value
     *
     * @param action
     * @param value
     */
    fun sendAction(action: String, value: Long) {
        val bundle = Bundle();
        bundle.putLong(FirebaseAnalytics.Param.VALUE, value)
        fa.logEvent(action, bundle)
    }
}
