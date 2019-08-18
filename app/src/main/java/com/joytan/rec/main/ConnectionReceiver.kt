package com.joytan.rec.main

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import androidx.appcompat.app.AlertDialog
import com.joytan.rec.R

val ACTION = "android.net.conn.CONNECTIVITY_CHANGE"
/*
    here we handle connection lost event
    show a popup over the screen to notify the user
 */
class ConnectionReceiver : BroadcastReceiver() {
    private var alertDialog: AlertDialog? = null

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "android.net.conn.CONNECTIVITY_CHANGE") {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

            val activeNetwork = cm.activeNetworkInfo
            val isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting
            if (isConnected) {
                try {
                    if (alertDialog != null && alertDialog!!.isShowing())
                        alertDialog!!.dismiss()
                } catch (e: Exception) {
                    e.printStackTrace()
                }

            } else {
                if (alertDialog == null || !alertDialog!!.isShowing()) {
                    val builder = AlertDialog.Builder(context, R.style.AlertDialog)
                    builder.setTitle("No internet connection")
                    builder.setMessage("Internet connection is required to send/receive data through the Internet." +
                            "Offline feature is currently under development.")
                    builder.setCancelable(false)
                    alertDialog = builder.create()
                    alertDialog!!.show()
                }
            }
        }
    }

    companion object {
        private val TAG = "ConnectionReceiver"
    }
}